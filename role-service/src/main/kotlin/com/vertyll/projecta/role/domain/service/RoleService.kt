package com.vertyll.projecta.role.domain.service

import com.vertyll.projecta.common.exception.ApiException
import com.vertyll.projecta.role.domain.dto.RoleCreateDto
import com.vertyll.projecta.role.domain.dto.RoleResponseDto
import com.vertyll.projecta.role.domain.dto.RoleUpdateDto
import com.vertyll.projecta.role.domain.model.Role
import com.vertyll.projecta.role.domain.model.UserRole
import com.vertyll.projecta.role.domain.repository.RoleRepository
import com.vertyll.projecta.role.domain.repository.UserRoleRepository
import com.vertyll.projecta.role.infrastructure.kafka.RoleEventProducer
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(
    private val roleRepository: RoleRepository,
    private val userRoleRepository: UserRoleRepository,
    private val roleEventProducer: RoleEventProducer
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Initialize default roles on startup
     */
    @PostConstruct
    fun initializeDefaultRoles() {
        logger.info("Initializing default roles")
        try {
            // Create default USER role if it doesn't exist
            if (!roleRepository.existsByName("USER")) {
                val userRole = Role.create(
                    name = "USER",
                    description = "Default role for all users"
                )
                roleRepository.save(userRole)
                logger.info("Created default USER role")
            }

            // Create ADMIN role if it doesn't exist
            if (!roleRepository.existsByName("ADMIN")) {
                val adminRole = Role.create(
                    name = "ADMIN",
                    description = "Admin role with all privileges"
                )
                roleRepository.save(adminRole)
                logger.info("Created default ADMIN role")
            }
        } catch (e: Exception) {
            logger.error("Error initializing default roles: ${e.message}", e)
        }
    }

    @Transactional
    fun createRole(dto: RoleCreateDto): RoleResponseDto {
        if (roleRepository.existsByName(dto.name)) {
            throw ApiException("Role with name ${dto.name} already exists", HttpStatus.BAD_REQUEST)
        }

        val role = Role.create(
            name = dto.name,
            description = dto.description
        )

        val savedRole = roleRepository.save(role)

        try {
            roleEventProducer.sendRoleCreatedEvent(savedRole)
        } catch (e: Exception) {
            logger.error("Failed to send role created event: ${e.message}", e)
        }

        return mapToDto(savedRole)
    }

    @Transactional
    fun updateRole(id: Long, dto: RoleUpdateDto): RoleResponseDto {
        val role = roleRepository.findById(id)
            .orElseThrow { ApiException("Role not found", HttpStatus.NOT_FOUND) }

        if (dto.name != role.name && roleRepository.existsByName(dto.name)) {
            throw ApiException("Role with name ${dto.name} already exists", HttpStatus.BAD_REQUEST)
        }

        val updatedRole = Role(
            id = role.id,
            name = dto.name,
            description = dto.description
        )

        val savedRole = roleRepository.save(updatedRole)

        try {
            roleEventProducer.sendRoleUpdatedEvent(savedRole)
        } catch (e: Exception) {
            logger.error("Failed to send role updated event: ${e.message}", e)
        }

        return mapToDto(savedRole)
    }

    @Transactional(readOnly = true)
    fun getRoleById(id: Long): RoleResponseDto {
        val role = roleRepository.findById(id)
            .orElseThrow { ApiException("Role not found", HttpStatus.NOT_FOUND) }
        return mapToDto(role)
    }

    @Transactional(readOnly = true)
    fun getRoleByName(name: String): RoleResponseDto {
        val role = roleRepository.findByName(name)
            .orElseThrow { ApiException("Role not found", HttpStatus.NOT_FOUND) }
        return mapToDto(role)
    }

    @Transactional(readOnly = true)
    fun getAllRoles(): List<RoleResponseDto> {
        return roleRepository.findAll().map { mapToDto(it) }
    }

    /**
     * Assigns a role to a user
     * @param userId The ID of the user
     * @param roleName The name of the role to assign
     * @return The created user-role mapping
     */
    @Transactional
    fun assignRoleToUser(userId: Long, roleName: String): UserRole {
        logger.info("Assigning role $roleName to user $userId")

        // Check if role exists
        val role = roleRepository.findByName(roleName)
            .orElseThrow {
                ApiException("Role $roleName not found", HttpStatus.NOT_FOUND)
            }

        // Check if user already has this role
        if (userRoleRepository.existsByUserIdAndRoleId(userId, role.id!!)) {
            logger.info("User $userId already has role $roleName")
            return userRoleRepository.findByUserIdAndRoleId(userId, role.id)
                .orElseThrow { ApiException("User-role mapping not found", HttpStatus.INTERNAL_SERVER_ERROR) }
        }

        // Create and save the user-role mapping
        val userRole = UserRole(
            userId = userId,
            roleId = role.id
        )

        val savedUserRole = userRoleRepository.save(userRole)

        // Send role assigned event
        try {
            roleEventProducer.sendRoleAssignedEvent(savedUserRole, role.name)
        } catch (e: Exception) {
            logger.error("Failed to send role assigned event: ${e.message}", e)
            // Continue even if event fails to send
        }

        logger.info("Successfully assigned role $roleName to user $userId")
        return savedUserRole
    }

    /**
     * Removes a role from a user
     * @param userId The ID of the user
     * @param roleName The name of the role to remove
     */
    @Transactional
    fun removeRoleFromUser(userId: Long, roleName: String) {
        logger.info("Removing role $roleName from user $userId")

        // Check if role exists
        val role = roleRepository.findByName(roleName)
            .orElseThrow {
                ApiException("Role $roleName not found", HttpStatus.NOT_FOUND)
            }

        // Check if user has this role
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, role.id!!)) {
            logger.info("User $userId doesn't have role $roleName")
            return
        }

        // Delete the user-role mapping
        userRoleRepository.deleteByUserIdAndRoleId(userId, role.id)

        // Send role revoked event
        try {
            roleEventProducer.sendRoleRevokedEvent(userId, role.id, role.name)
        } catch (e: Exception) {
            logger.error("Failed to send role revoked event: ${e.message}", e)
            // Continue even if event fails to send
        }

        logger.info("Successfully removed role $roleName from user $userId")
    }

    /**
     * Gets all roles for a user
     * @param userId The ID of the user
     * @return A list of role DTOs
     */
    @Transactional(readOnly = true)
    fun getRolesForUser(userId: Long): List<RoleResponseDto> {
        val userRoles = userRoleRepository.findByUserId(userId)
        val roleIds = userRoles.map { it.roleId }

        if (roleIds.isEmpty()) {
            return emptyList()
        }

        return roleRepository.findAllById(roleIds).map { mapToDto(it) }
    }

    /**
     * Gets all users for a role
     * @param roleId The ID of the role
     * @return A list of user IDs
     */
    @Transactional(readOnly = true)
    fun getUsersForRole(roleId: Long): List<Long> {
        val userRoles = userRoleRepository.findByRoleId(roleId)
        return userRoles.map { it.userId }
    }

    private fun mapToDto(role: Role): RoleResponseDto {
        return RoleResponseDto(
            id = role.id!!,
            name = role.name,
            description = role.description
        )
    }
}
