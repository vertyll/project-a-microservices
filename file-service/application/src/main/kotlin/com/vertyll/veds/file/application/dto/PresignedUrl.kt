package com.vertyll.veds.file.application.dto

import java.time.Instant

/**
 * A time-limited URL the browser uses to talk to the object store directly.
 *
 * Returned by `ObjectStoragePort`. It lives with the other DTOs rather than in the port package:
 * a port package holds contracts, and this is the value one of them carries.
 */
data class PresignedUrl(
    val url: String,
    val expiresAt: Instant,
)
