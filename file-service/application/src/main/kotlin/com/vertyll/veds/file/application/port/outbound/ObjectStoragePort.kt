package com.vertyll.veds.file.application.port.outbound

import com.vertyll.veds.file.application.dto.PresignedUrl

interface ObjectStoragePort {
    /**
     * A URL the client may `PUT` the object to.
     *
     * It carries no size limit: a presigned `PUT` is signed over the key and the content
     * type, and the store accepts whatever arrives. The scope's maximum is checked twice
     * instead — against the size the client declares before the URL is issued, and against
     * the size the store reports at confirmation, where an object that grew past the limit
     * is deleted rather than recorded.
     */
    fun presignUpload(
        objectKey: String,
        contentType: String,
    ): PresignedUrl

    fun presignDownload(
        objectKey: String,
        originalName: String,
        contentType: String,
    ): PresignedUrl

    fun sizeOf(objectKey: String): Long?

    fun delete(objectKey: String)
}
