package com.ranaseek.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream

/** 背景图的读取与保存（app 私有目录，不上传、不外泄）。 */
object ImageStore {

    private const val DIR = "background"

    fun loadBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    }.getOrNull()

    fun saveBackgroundJpeg(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        // 清理旧图，避免堆文件
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "chat_bg.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file.absolutePath
    }

    fun loadBackground(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }

    /** 保存聊天图片：缩放到长边 1024 的 JPEG（控制 base64 体积）。 */
    fun saveChatImage(context: Context, uri: Uri): String? {
        val bitmap = loadBitmap(context, uri) ?: return null
        val scaled = scaleToMaxSide(bitmap, 1024)
        val dir = File(context.filesDir, "chat_images").apply { mkdirs() }
        val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
        return runCatching {
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        }.getOrNull()
    }

    fun loadChatImage(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }

    private fun scaleToMaxSide(src: Bitmap, maxSide: Int): Bitmap {
        val side = maxOf(src.width, src.height)
        if (side <= maxSide) return src
        val ratio = maxSide.toFloat() / side
        return Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    /** 亚克力磨砂：box blur（3 次迭代近似高斯），比缩放模糊自然。 */
    fun blurForAcrylic(source: Bitmap, blurPx: Int): Bitmap {
        if (blurPx <= 0) return source
        val radius = (blurPx / 2).coerceIn(1, 30)
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        repeat(3) {
            boxBlurH(pixels, tmp, w, h, radius)
            boxBlurV(tmp, pixels, w, h, radius)
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun boxBlurH(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val row = y * w
            var sa = 0
            var sr = 0
            var sg = 0
            var sb = 0
            for (i in -r..r) {
                val x = i.coerceIn(0, w - 1)
                val px = src[row + x]
                sa += (px shr 24) and 0xFF
                sr += (px shr 16) and 0xFF
                sg += (px shr 8) and 0xFF
                sb += px and 0xFF
            }
            for (x in 0 until w) {
                dst[row + x] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val addX = (x + r + 1).coerceAtMost(w - 1)
                val subX = (x - r).coerceAtLeast(0)
                val add = src[row + addX]
                val sub = src[row + subX]
                sa += ((add shr 24) and 0xFF) - ((sub shr 24) and 0xFF)
                sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                sb += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }

    private fun boxBlurV(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var sa = 0
            var sr = 0
            var sg = 0
            var sb = 0
            for (i in -r..r) {
                val y = i.coerceIn(0, h - 1)
                val px = src[y * w + x]
                sa += (px shr 24) and 0xFF
                sr += (px shr 16) and 0xFF
                sg += (px shr 8) and 0xFF
                sb += px and 0xFF
            }
            for (y in 0 until h) {
                dst[y * w + x] =
                    ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val addY = (y + r + 1).coerceAtMost(h - 1)
                val subY = (y - r).coerceAtLeast(0)
                val add = src[addY * w + x]
                val sub = src[subY * w + x]
                sa += ((add shr 24) and 0xFF) - ((sub shr 24) and 0xFF)
                sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                sb += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }
}
