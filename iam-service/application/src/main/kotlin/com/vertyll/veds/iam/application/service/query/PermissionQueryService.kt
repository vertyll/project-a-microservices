package com.vertyll.veds.iam.application.service.query

import com.vertyll.veds.iam.application.dto.PermissionResponse
import com.vertyll.veds.iam.application.port.inbound.query.PermissionQueryUseCase
import com.vertyll.veds.iam.domain.repository.PermissionRepository
import com.vertyll.veds.iam.domain.repository.RoleRepository

class PermissionQueryService(
    private val permissionRepository: PermissionRepository,
    private val roleRepository: RoleRepository,
) : PermissionQueryUseCase {
    override fun getAllPermissions(): List<PermissionResponse> {
        val roles = roleRepository.findAll()

        return permissionRepository
            .findAll()
            .sortedBy { it.name }
            .map { permission ->
                PermissionResponse(
                    id = permission.id ?: error("a stored permission has no id"),
                    name = permission.name,
                    description = permission.description,
                    grantedByRoles =
                        roles
                            .filter { role -> role.permissions.any { it.id == permission.id } }
                            .map { it.name }
                            .sorted(),
                )
            }
    }
}
