package com.vertyll.veds.iam.domain.model

import java.time.Instant

data class Role(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val permissions: Set<Permission> = emptySet(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    fun grants(permissionName: String): Boolean = permissions.any { it.name == permissionName }

    fun withPermission(permission: Permission): Role {
        if (permission.id == null || permissions.any { it.id == permission.id }) return this
        return copy(permissions = permissions + permission, updatedAt = Instant.now())
    }

    fun withoutPermission(permissionId: Long): Role {
        if (permissions.none { it.id == permissionId }) return this
        return copy(permissions = permissions.filterNot { it.id == permissionId }.toSet(), updatedAt = Instant.now())
    }

    companion object {
        fun create(
            name: String,
            description: String? = null,
            permissions: Set<Permission> = emptySet(),
        ): Role =
            Role(
                name = name,
                description = description,
                permissions = permissions,
            )
    }
}
