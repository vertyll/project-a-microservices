package com.vertyll.projecta.mail.domain.model.entity

import com.vertyll.projecta.mail.domain.model.enums.SagaStepStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.Instant

/**
 * Represents a step in a saga (a distributed transaction across multiple services).
 */
@Entity
@Table(name = "saga_steps")
class SagaStep(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val sagaId: String,

    @Column(nullable = false)
    val stepName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: SagaStepStatus,

    @Lob
    @Column(columnDefinition = "TEXT")
    val payload: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = true)
    val completedAt: Instant? = null,

    @Column(nullable = true)
    val compensationStepId: Long? = null
) {
    // No-args constructor required by JPA
    constructor() : this(
        id = 0,
        sagaId = "",
        stepName = "",
        status = SagaStepStatus.STARTED,
        payload = null,
        createdAt = Instant.now(),
        completedAt = null,
        compensationStepId = null
    )
}
