package com.vertyll.projecta.role.infrastructure.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.role.domain.model.entity.Role
import com.vertyll.projecta.role.domain.model.entity.SagaStep
import com.vertyll.projecta.role.domain.model.entity.UserRole
import com.vertyll.projecta.role.domain.model.enums.SagaCompensationActions
import com.vertyll.projecta.role.domain.model.enums.SagaStepNames
import com.vertyll.projecta.role.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.role.domain.repository.RoleRepository
import com.vertyll.projecta.role.domain.repository.SagaStepRepository
import com.vertyll.projecta.role.domain.repository.UserRoleRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service that handles compensation actions for the Role Service
 */
@Service
class SagaCompensationService(
    private val roleRepository: RoleRepository,
    private val userRoleRepository: UserRoleRepository,
    private val sagaStepRepository: SagaStepRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Listens for compensation events and processes them
     */
    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getSagaCompensationTopic()}"])
    @Transactional
    fun handleCompensationEvent(payload: String) {
        try {
            val event = objectMapper.readValue(
                payload,
                Map::class.java
            )
            val sagaId = event["sagaId"] as String
            val actionStr = event["action"] as String

            logger.info("Processing compensation action: $actionStr for saga $sagaId")

            when (actionStr) {
                SagaCompensationActions.DELETE_ROLE.value -> {
                    val roleId = (event["roleId"] as Number).toLong()
                    deleteRole(roleId)
                }
                SagaCompensationActions.REVOKE_ROLE.value -> {
                    val userId = (event["userId"] as Number).toLong()
                    val roleId = (event["roleId"] as Number).toLong()
                    revokeRole(userId, roleId)
                }
                SagaCompensationActions.ASSIGN_ROLE.value -> {
                    val userId = (event["userId"] as Number).toLong()
                    val roleId = (event["roleId"] as Number).toLong()
                    val roleName = event["roleName"] as String
                    assignRole(userId, roleId, roleName)
                }
                SagaCompensationActions.REVERT_ROLE_UPDATE.value -> {
                    val roleId = (event["roleId"] as Number).toLong()
                    val originalData = event["originalData"]
                    revertRoleUpdate(roleId, originalData)
                }
                SagaCompensationActions.RECREATE_ROLE.value -> {
                    val roleId = (event["roleId"] as Number).toLong()
                    val originalData = event["originalData"]
                    recreateRole(roleId, originalData)
                }
                else -> {
                    logger.warn("Unknown compensation action: $actionStr")
                }
            }

            // If there's a stepId, record that compensation was completed
            val stepId = event["stepId"] as? Number
            if (stepId != null) {
                val step = sagaStepRepository.findById(stepId.toLong()).orElse(null)
                if (step != null) {
                    val compensationStep = SagaStep(
                        sagaId = sagaId,
                        stepName = SagaStepNames.compensationNameFromString(step.stepName),
                        status = SagaStepStatus.COMPENSATED,
                        createdAt = Instant.now(),
                        completedAt = Instant.now(),
                        compensationStepId = step.id
                    )
                    sagaStepRepository.save(compensationStep)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process compensation event: ${e.message}", e)
        }
    }

    /**
     * Delete a role as part of compensation
     */
    @Transactional
    fun deleteRole(roleId: Long) {
        try {
            roleRepository.findById(roleId).ifPresent { role ->
                logger.info("Compensating by deleting role with ID $roleId")
                roleRepository.delete(role)
            }
        } catch (e: Exception) {
            logger.error("Failed to delete role $roleId as compensation: ${e.message}", e)
            throw e
        }
    }

    /**
     * Revoke a role from a user as part of compensation
     */
    @Transactional
    fun revokeRole(userId: Long, roleId: Long) {
        try {
            if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
                logger.info("Compensating by revoking role $roleId from user $userId")
                userRoleRepository.deleteByUserIdAndRoleId(userId, roleId)
            }
        } catch (e: Exception) {
            logger.error("Failed to revoke role $roleId from user $userId as compensation: ${e.message}", e)
            throw e
        }
    }

    /**
     * Assign a role to a user as part of compensation
     */
    @Transactional
    fun assignRole(userId: Long, roleId: Long, roleName: String) {
        try {
            if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
                logger.info("Compensating by assigning role $roleName ($roleId) to user $userId")

                val role = roleRepository.findById(roleId).orElse(null)
                if (role != null) {
                    val userRole = UserRole(
                        userId = userId,
                        roleId = roleId
                    )
                    userRoleRepository.save(userRole)
                } else {
                    logger.warn("Cannot compensate - role $roleId not found")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to assign role $roleName to user $userId as compensation: ${e.message}", e)
            throw e
        }
    }

    /**
     * Revert a role update as part of compensation
     */
    @Transactional
    fun revertRoleUpdate(roleId: Long, originalData: Any?) {
        try {
            val roleOpt = roleRepository.findById(roleId)
            if (!roleOpt.isPresent) {
                logger.warn("Cannot compensate - role $roleId not found")
                return
            }

            val role = roleOpt.get()
            logger.info("Compensating by reverting update for role $roleId")

            if (originalData == null) {
                logger.warn("No original data provided for compensation, cannot revert changes")
                return
            }

            // Convert originalData back to a Role object and update fields
            try {
                val originalDataMap = originalData as? Map<*, *> ?: run {
                    logger.error("Original data is not in the expected format")
                    return
                }

                // Since Role properties are val, we need to create a new Role object
                val updatedRole = Role(
                    id = role.id,
                    name = originalDataMap["name"]?.toString() ?: role.name,
                    description = originalDataMap["description"]?.toString() ?: role.description,
                    createdAt = role.createdAt,
                    updatedAt = Instant.now()
                )

                roleRepository.save(updatedRole)
                logger.info("Successfully reverted update for role $roleId")
            } catch (e: Exception) {
                logger.error("Failed to parse and apply original data: ${e.message}", e)
                throw e
            }
        } catch (e: Exception) {
            logger.error("Failed to revert role update for $roleId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Recreate a deleted role as part of compensation
     */
    @Transactional
    fun recreateRole(roleId: Long, originalData: Any?) {
        try {
            // Check if role already exists (idempotency check)
            if (roleRepository.existsById(roleId)) {
                logger.info("Role $roleId already exists, skipping recreation")
                return
            }

            logger.info("Compensating by recreating role $roleId")

            if (originalData == null) {
                logger.warn("No original data provided for compensation, cannot recreate role")
                return
            }

            try {
                val originalDataMap = originalData as? Map<*, *> ?: run {
                    logger.error("Original data is not in the expected format")
                    return
                }

                // Create new role with original ID and values
                val name = originalDataMap["name"]?.toString() ?: run {
                    logger.error("Missing name in original data")
                    return
                }

                val description = originalDataMap["description"]?.toString() ?: ""

                val role = Role(
                    id = roleId,
                    name = name,
                    description = description
                )

                roleRepository.save(role)
                logger.info("Successfully recreated role $roleId")
            } catch (e: Exception) {
                logger.error("Failed to parse and apply original data: ${e.message}", e)
                throw e
            }
        } catch (e: Exception) {
            logger.error("Failed to recreate role $roleId: ${e.message}", e)
            throw e
        }
    }
}
