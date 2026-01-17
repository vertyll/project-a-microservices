package com.vertyll.projecta.auth.domain.model.entity

import com.vertyll.projecta.sharedinfrastructure.role.RoleType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "auth_user_role",
    uniqueConstraints = [UniqueConstraint(columnNames = ["auth_user_id", "role_id"])],
)
class AuthUserRole(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "auth_user_id", nullable = false)
    val authUserId: Long,
    @Column(name = "role_id", nullable = false)
    val roleId: Long,
    @Column(name = "role_name", nullable = false, length = 50)
    val roleName: RoleType,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    // No-args constructor for JPA
    constructor() : this(
        id = null,
        authUserId = 0,
        roleId = 0,
        roleName = RoleType.USER,
    )
}
