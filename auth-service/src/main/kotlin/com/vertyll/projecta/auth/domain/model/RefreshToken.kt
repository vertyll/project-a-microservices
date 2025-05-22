package com.vertyll.projecta.auth.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "refresh_token")
class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 1024)
    var token: String,

    @Column(nullable = false)
    var username: String,

    @Column(nullable = false)
    var expiryDate: Instant,

    @Column(nullable = false)
    var revoked: Boolean = false,

    @Column(nullable = true)
    var deviceInfo: String? = null,

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
                expiryDate = Instant.now(),
                revoked = false,
                deviceInfo = null
            )

    val isRevoked: Boolean
        get() = revoked
}
