package com.vertyll.veds.iam.domain.model

import java.time.Instant

data class VerificationToken(
    val id: Long? = null,
    val token: String,
    val username: String,
    val expiryDate: Instant,
    val used: Boolean = false,
    val tokenType: String,
    val additionalData: String? = null,
    val sagaId: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    fun markUsed(): VerificationToken =
        copy(
            used = true,
            updatedAt = Instant.now(),
        )
}
