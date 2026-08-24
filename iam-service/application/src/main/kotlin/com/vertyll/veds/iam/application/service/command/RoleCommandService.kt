package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.port.inbound.command.RoleCommandUseCase
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.VersionGuard
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository

class RoleCommandService(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val identityProvider: IdentityProviderPort,
) : RoleCommandUseCase {
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
}
