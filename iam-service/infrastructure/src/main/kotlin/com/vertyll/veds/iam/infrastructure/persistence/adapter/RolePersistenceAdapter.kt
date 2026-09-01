package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.PermissionJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.RoleJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.PermissionJpaRepository
import com.vertyll.veds.iam.infrastructure.persistence.repository.RoleJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class RolePersistenceAdapter(
    private val repository: RoleJpaRepository,
    private val permissionRepository: PermissionJpaRepository,
) : RoleRepository {
    override fun save(role: Role): Role {
        val managedPermissions =
            role.permissions
                .map { permission ->
                    val id =
                        permission.id
                            ?: error("cannot grant an unsaved permission '${permission.name}' to a role")
                    permissionRepository
                        .findById(id)
                        .orElseThrow { IllegalStateException("permission $id no longer exists") }
                }.toMutableSet()
        return repository.save(role.toJpaEntity(managedPermissions)).toDomain()
    }

    override fun findById(id: Long): Role? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByName(name: String): Role? = repository.findByName(name).orElse(null)?.toDomain()

    override fun existsByName(name: String): Boolean = repository.existsByName(name)

    override fun findAll(): List<Role> = repository.findAll().map { it.toDomain() }
}

private fun Role.toJpaEntity(managedPermissions: MutableSet<PermissionJpaEntity>) =
    RoleJpaEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        permissions = managedPermissions,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun RoleJpaEntity.toDomain() =
    Role(
        id = this.id,
        name = this.name,
        description = this.description,
        permissions = this.permissions.map { it.toDomain() }.toSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
