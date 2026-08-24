package com.vertyll.veds.notification.infrastructure.persistence.entity

import com.vertyll.veds.notification.domain.model.NotificationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "notification")
internal class NotificationJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: String,
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: NotificationStatus,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
