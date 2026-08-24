package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.dto.RoleResponse
import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.port.inbound.RoleUseCase
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.VersionGuard
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository

class RoleService(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val identityProvider: IdentityProviderPort,
) : RoleUseCase {
    override fun getRoleById(id: Long): RoleResponse {
        val role = roleRepository.findById(id) ?: throw ApiException(IamError.ROLE_NOT_FOUND)
        return mapToDto(role)
    }

    override fun getRoleByName(name: String): RoleResponse {
        val role = roleRepository.findByName(name) ?: throw ApiException(IamError.ROLE_NOT_FOUND)
        return mapToDto(role)
    }

    override fun getAllRoles(): List<RoleResponse> = roleRepository.findAll().map { mapToDto(it) }

    override fun getRolesForUser(userId: Long): List<RoleResponse> {
        val user = userRepository.findById(userId) ?: throw ApiException(IamError.USER_NOT_FOUND)
        return user.roles.map { mapToDto(it) }
    }

    override fun assignRoleToUser(
        userId: Long,
        roleName: String,
        version: Long?,
    ) {
        val user = userRepository.findById(userId) ?: throw ApiException(IamError.USER_NOT_FOUND)

        VersionGuard.requireMatch(user.version, version) {
            ApiException(IamError.VERSION_MISMATCH)
        }

        val role = roleRepository.findByName(roleName) ?: throw ApiException(IamError.ROLE_NOT_FOUND)
        val updated = user.withRole(role)
        userRepository.save(updated)

        updated.keycloakId?.let { identityProvider.assignRole(it.toString(), roleName) }
    }

    override fun removeRoleFromUser(
        userId: Long,
        roleName: String,
        version: Long?,
    ) {
        val user = userRepository.findById(userId) ?: throw ApiException(IamError.USER_NOT_FOUND)

        VersionGuard.requireMatch(user.version, version) {
            ApiException(IamError.VERSION_MISMATCH)
        }

        val role = roleRepository.findByName(roleName) ?: throw ApiException(IamError.ROLE_NOT_FOUND)
        val updated = user.withoutRole(role.id!!)
        userRepository.save(updated)

        updated.keycloakId?.let { identityProvider.removeRole(it.toString(), roleName) }
    }

    private fun mapToDto(role: Role): RoleResponse =
        RoleResponse(
            id = role.id!!,
            name = role.name,
            description = role.description,
            version = role.version,
        )
}
