package com.ranaseek.app.data.repo

/** 已上传到 DeepSeek Files API 的文件。 */
data class UploadedFile(
    val id: String,
    val name: String,
    val purpose: String,
    val sizeBytes: Long = 0
)
