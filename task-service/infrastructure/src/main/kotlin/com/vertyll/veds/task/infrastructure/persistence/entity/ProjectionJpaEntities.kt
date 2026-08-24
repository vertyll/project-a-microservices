package com.vertyll.veds.task.infrastructure.persistence.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import java.io.Serializable
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
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(
    name = "project_category_ref",
    indexes = [Index(name = "idx_category_ref_project", columnList = "project_id")],
)
internal class ProjectCategoryRefJpaEntity(
    @Id
    @Column(name = "category_id", nullable = false, updatable = false)
    var categoryId: UUID,
    @Column(name = "project_id", nullable = false)
    var projectId: UUID,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_category_ref_name", joinColumns = [JoinColumn(name = "category_id")])
    @MapKeyColumn(name = "language", length = 8)
    @Column(name = "name", nullable = false)
    var names: MutableMap<String, String> = mutableMapOf(),
    @Column(name = "color", nullable = false, length = 32)
    var color: String,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(
    name = "project_status_ref",
    indexes = [Index(name = "idx_status_ref_project", columnList = "project_id")],
)
internal class ProjectStatusRefJpaEntity(
    @Id
    @Column(name = "status_id", nullable = false, updatable = false)
    var statusId: UUID,
    @Column(name = "project_id", nullable = false)
    var projectId: UUID,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_status_ref_name", joinColumns = [JoinColumn(name = "status_id")])
    @MapKeyColumn(name = "language", length = 8)
    @Column(name = "name", nullable = false)
    var names: MutableMap<String, String> = mutableMapOf(),
    @Column(name = "color", nullable = false, length = 32)
    var color: String,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

internal data class ProjectMembershipRefId(
    var projectId: UUID? = null,
    var userId: UUID? = null,
) : Serializable

@Entity
@Table(
    name = "project_membership_ref",
    indexes = [Index(name = "idx_membership_ref_user", columnList = "user_id")],
)
@IdClass(ProjectMembershipRefId::class)
internal class ProjectMembershipRefJpaEntity(
    @Id
    @Column(name = "project_id", nullable = false, updatable = false)
    var projectId: UUID,
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,
    @Column(name = "role_code", nullable = false, length = 32)
    var roleCode: String,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "user_ref")
internal class UserRefJpaEntity(
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,
    @Column(name = "email", nullable = false)
    var email: String,
    @Column(name = "first_name")
    var firstName: String? = null,
    @Column(name = "last_name")
    var lastName: String? = null,
    @Column(name = "avatar_file_id")
    var avatarFileId: UUID? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
