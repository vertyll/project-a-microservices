package com.vertyll.projecta.auth.domain.model.entity

import com.vertyll.projecta.common.auth.TokenTypes
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(name = "verification_token")
class VerificationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 1024)
    var token: String,

    @Column(nullable = false)
    var username: String,

    @Column(nullable = false)
    var expiryDate: LocalDateTime,

    @Column(nullable = false)
    var used: Boolean = false,

    @Column(nullable = false)
    var tokenType: String,

    @Column(nullable = true)
    var additionalData: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    // No-arg constructor for JPA
    constructor() :
        this(
            id = null,
            token = "",
            username = "",
            expiryDate = LocalDateTime.now(),
            used = false,
            tokenType = "",
            additionalData = null
        )

    /**
     * Check if the token is of a specific type.
     */
    fun isTokenType(type: TokenTypes): Boolean {
        return this.tokenType == type.value
    }
}
