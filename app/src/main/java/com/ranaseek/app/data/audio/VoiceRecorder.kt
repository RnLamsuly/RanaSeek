package com.ranaseek.app.data.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream

/**
 * 录音：AudioRecord 采集 16kHz / 16bit / 单声道 PCM，停止时封装成 WAV。
 * DeepSeek ASR 要求 WAV(16bit) 或 Ogg Opus，单文件 ≤8MiB、≤120 秒。
 */
class VoiceRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private var pcmFile: File? = null
    @Volatile
    private var recording = false

    private val sampleRate = 16000

    fun start(): Boolean {
        if (recording) return false
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return false
        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 4
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return false
            }
            val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.pcm")
            pcmFile = file
            audioRecord = record
            recording = true
            record.startRecording()
            thread = Thread {
                try {
                    FileOutputStream(file).use { out ->
                        val buffer = ByteArray(minBuf)
                        while (recording) {
                            val n = record.read(buffer, 0, buffer.size)
                            if (n > 0) out.write(buffer, 0, n)
                        }
                    }
                } catch (_: Exception) {
                }
            }.also { it.start() }
            true
        } catch (e: Exception) {
            recording = false
            false
        }
    }

    /** 停止录音并返回 WAV 文件；失败返回 null。 */
    fun stop(): File? {
        if (!recording) return null
        recording = false
        runCatching { audioRecord?.stop() }
        runCatching { thread?.join(1000) }
        runCatching { audioRecord?.release() }
        audioRecord = null
        thread = null
        val pcm = pcmFile ?: return null
        pcmFile = null
        return try {
            val wav = File(context.cacheDir, "rec_${System.currentTimeMillis()}.wav")
            pcmToWav(pcm, wav)
            pcm.delete()
            wav
        } catch (e: Exception) {
            null
        }
    }

    fun cancel() {
        recording = false
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        thread = null
        pcmFile?.delete()
        pcmFile = null
    }

    private fun pcmToWav(pcm: File, wav: File) {
        val data = pcm.readBytes()
        val totalLen = 36 + data.size
        val header = ByteArray(44)
        fun putInt(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = (value shr 8 and 0xFF).toByte()
            header[offset + 2] = (value shr 16 and 0xFF).toByte()
            header[offset + 3] = (value shr 24 and 0xFF).toByte()
        }

        fun putShort(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = (value shr 8 and 0xFF).toByte()
        }

        "RIFF".toByteArray().copyInto(header, 0)
        putInt(4, totalLen)
        "WAVE".toByteArray().copyInto(header, 8)
        "fmt ".toByteArray().copyInto(header, 12)
        putInt(16, 16)                 // fmt chunk size
        putShort(20, 1)                // PCM
        putShort(22, 1)                // mono
        putInt(24, sampleRate)
        putInt(28, sampleRate * 2)     // byte rate
        putShort(32, 2)                // block align
        putShort(34, 16)               // bits
        "data".toByteArray().copyInto(header, 36)
        putInt(40, data.size)

        FileOutputStream(wav).use { out ->
            out.write(header)
            out.write(data)
        }
    }
}
