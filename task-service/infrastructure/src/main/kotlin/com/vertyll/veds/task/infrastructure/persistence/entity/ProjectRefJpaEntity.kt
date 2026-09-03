package com.vertyll.veds.task.infrastructure.persistence.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "project_ref")
internal class ProjectRefJpaEntity(
    @Id
    @Column(name = "project_id", nullable = false, updatable = false)
    var projectId: UUID,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "hidden_work_log_enabled", nullable = false)
    var hiddenWorkLogEnabled: Boolean = false,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_ref_hidden_work_log_role", joinColumns = [JoinColumn(name = "project_id")])
    @Column(name = "role_code", nullable = false)
    var hiddenWorkLogRoles: MutableSet<String> = mutableSetOf(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
