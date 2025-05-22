package com.vertyll.projecta.role.domain.model

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
    name = "user_role",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "role_id"])]
)
class UserRole(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "role_id", nullable = false)
    val roleId: Long,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    // No-args constructor for JPA
    constructor() : this(
        id = null,
        userId = 0,
        roleId = 0
    )
}
