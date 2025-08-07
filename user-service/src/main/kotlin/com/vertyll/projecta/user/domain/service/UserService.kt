package com.vertyll.projecta.user.domain.service

import com.vertyll.projecta.common.event.user.UserRegisteredEvent
import com.vertyll.projecta.common.role.RoleType
import com.vertyll.projecta.user.domain.dto.EmailUpdateDto
import com.vertyll.projecta.user.domain.dto.UserCreateDto
import com.vertyll.projecta.user.domain.dto.UserResponseDto
import com.vertyll.projecta.user.domain.model.entity.User
import com.vertyll.projecta.user.domain.repository.UserRepository
import com.vertyll.projecta.user.infrastructure.exception.ApiException
import com.vertyll.projecta.user.infrastructure.kafka.UserEventProducer
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userEventProducer: UserEventProducer
) {

    @Transactional
    fun createUser(dto: UserCreateDto): UserResponseDto {
        if (userRepository.existsByEmail(dto.email)) {
            throw ApiException(
                message = "Email already exists",
                status = HttpStatus.BAD_REQUEST
            )
        }

        val user = User.create(
            firstName = dto.firstName,
            lastName = dto.lastName,
            email = dto.email,
            roles = dto.roles.ifEmpty { setOf(RoleType.USER.value) },
            profilePicture = dto.profilePicture,
            phoneNumber = dto.phoneNumber,
            address = dto.address
        )

        val savedUser = userRepository.save(user)

        // Publish user created event
        userEventProducer.send(
            UserRegisteredEvent(
                userId = savedUser.id!!,
                email = savedUser.getEmail(),
                firstName = savedUser.firstName,
                lastName = savedUser.lastName,
                roles = savedUser.getCachedRoles()
            )
        )

        return mapToDto(savedUser)
    }

    @Transactional(readOnly = true)
    fun getUserById(id: Long): UserResponseDto {
        val user = userRepository.findById(id)
            .orElseThrow {
                ApiException(
                    message = "User not found",
                    status = HttpStatus.NOT_FOUND
                )
            }
        return mapToDto(user)
    }

    @Transactional(readOnly = true)
    fun getUserByEmail(email: String): UserResponseDto {
        val user = userRepository.findByEmail(email)
            .orElseThrow {
                ApiException(
                    message = "User not found",
                    status = HttpStatus.NOT_FOUND
                )
            }
        return mapToDto(user)
    }

    @Transactional
    fun updateEmail(request: EmailUpdateDto): UserResponseDto {
        if (userRepository.existsByEmail(request.newEmail)) {
            throw ApiException(
                message = "Email already exists",
                status = HttpStatus.BAD_REQUEST
            )
        }

        val user = userRepository.findByEmail(request.currentEmail)
            .orElseThrow {
                ApiException(
                    message = "User not found",
                    status = HttpStatus.NOT_FOUND
                )
            }

        user.setEmail(request.newEmail)
        val savedUser = userRepository.save(user)
        return mapToDto(savedUser)
    }

    private fun mapToDto(user: User): UserResponseDto {
        return UserResponseDto(
            id = user.id!!,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.getEmail(),
            roles = user.getCachedRoles(),
            profilePicture = user.profilePicture,
            phoneNumber = user.phoneNumber,
            address = user.address,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString()
        )
    }
}
