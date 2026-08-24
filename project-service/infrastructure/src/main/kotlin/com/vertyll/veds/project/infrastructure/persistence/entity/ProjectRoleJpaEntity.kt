package com.vertyll.veds.project.infrastructure.persistence.entity

import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "project_role")
internal class ProjectRoleJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true, length = 32)
    var code: ProjectRoleCode,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "project_role_permission",
        joinColumns = [JoinColumn(name = "project_role_id")],
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 64)
    var permissions: MutableSet<ProjectPermission> = mutableSetOf(),
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "project_role_translation",
        joinColumns = [JoinColumn(name = "project_role_id")],
    )
    var translations: MutableSet<TranslationEmbeddable> = mutableSetOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
