package com.vertyll.projecta.auth.domain.model

import jakarta.persistence.*
import java.time.Instant

/**
 * Entity representing the relationship between an AuthUser and a role from the Role Service.
 * This is used to track which roles a user has, without duplicating the role definitions.
 */
@Entity
@Table(name = "auth_user_role",
       uniqueConstraints = [UniqueConstraint(columnNames = ["auth_user_id", "role_id"])])
class AuthUserRole(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "auth_user_id", nullable = false)
    val authUserId: Long,
    
    @Column(name = "role_id", nullable = false)
    val roleId: Long,
    
    @Column(name = "role_name", nullable = false, length = 50)
    val roleName: String,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    // No-args constructor for JPA
    protected constructor() : this(
        id = null,
        authUserId = 0,
        roleId = 0,
        roleName = ""
    )
}
