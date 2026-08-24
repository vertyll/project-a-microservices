package com.vertyll.veds.file.application.port.outbound

import java.time.Instant

interface ObjectStoragePort {
    fun presignUpload(
        objectKey: String,
        contentType: String,
        maxSizeBytes: Long,
    ): PresignedUrl

    fun presignDownload(
        objectKey: String,
        originalName: String,
        contentType: String,
    ): PresignedUrl

    fun sizeOf(objectKey: String): Long?

    fun delete(objectKey: String)
}

data class PresignedUrl(
    val url: String,
    val expiresAt: Instant,
)
