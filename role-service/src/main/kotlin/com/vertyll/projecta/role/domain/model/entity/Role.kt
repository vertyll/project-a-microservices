package com.vertyll.projecta.role.domain.model.entity

import com.vertyll.projecta.sharedinfrastructure.role.RoleType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "role")
class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    val name: RoleType,
    @Column(nullable = true)
    val description: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    // No-args constructor required by JPA
    constructor() : this(
        id = null,
        name = RoleType.USER,
        description = null,
    )

    companion object {
        fun create(
            name: RoleType,
            description: String? = null,
        ): Role =
            Role(
                name = name,
                description = description,
            )
    }
}
