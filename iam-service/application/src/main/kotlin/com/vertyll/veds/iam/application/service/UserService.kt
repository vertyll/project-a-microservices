package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.command.UpdateProfileCommand
import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.port.inbound.UserUseCase
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.model.VersionGuard
import com.vertyll.veds.iam.domain.repository.UserRepository

class UserService(
    private val userRepository: UserRepository,
) : UserUseCase {
    private companion object {
    }

    override fun getAllUsers(pageRequest: PageRequest): PageResult<UserResponse> = userRepository.findAll(pageRequest).map(::mapToDto)

    override fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id) ?: throw ApiException(IamError.USER_NOT_FOUND)
        return mapToDto(user)
    }

    override fun getUserByEmail(email: String): UserResponse {
        val user = userRepository.findByEmail(email) ?: throw ApiException(IamError.USER_NOT_FOUND)
        return mapToDto(user)
    }

    override fun updateProfile(
        id: Long,
        request: UpdateProfileCommand,
        version: Long?,
    ): UserResponse {
        val user = userRepository.findById(id) ?: throw ApiException(IamError.USER_NOT_FOUND)

        VersionGuard.requireMatch(user.version, version) {
            ApiException(IamError.USER_VERSION_MISMATCH)
        }

        val updated =
            user.withProfile(
                firstName = request.firstName,
                lastName = request.lastName,
                profilePicture = request.profilePicture,
                phoneNumber = request.phoneNumber,
                address = request.address,
            )
        return mapToDto(userRepository.save(updated))
    }

    private fun mapToDto(user: User): UserResponse =
        UserResponse(
            id = user.id!!,
            keycloakId = user.keycloakId?.toString(),
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            roles = user.roles.map { it.name }.toSet(),
            permissions = user.permissions.map { it.name }.toSet(),
            profilePicture = user.profilePicture,
            phoneNumber = user.phoneNumber,
            address = user.address,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString(),
            version = user.version,
        )
}
