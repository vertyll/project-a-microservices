package com.vertyll.projecta.user.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Represents the state of a saga (a distributed transaction across multiple services).
 */
@Entity
@Table(name = "user_saga")
class Saga(
    @Id
    val id: String, // Using a UUID string as ID

    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: SagaStatus,

    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(nullable = true)
    var lastError: String? = null,

    @Column(nullable = false)
    val startedAt: Instant,

    @Column(nullable = true)
    var completedAt: Instant? = null,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    // No-args constructor required by JPA
    constructor() : this(
        id = "",
        type = "",
        status = SagaStatus.STARTED,
        payload = "",
        lastError = null,
        startedAt = Instant.now(),
        completedAt = null
    )
}
