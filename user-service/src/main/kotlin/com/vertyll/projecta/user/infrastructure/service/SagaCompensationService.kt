package com.vertyll.projecta.user.infrastructure.service

import com.vertyll.projecta.user.domain.model.entity.SagaStep
import com.vertyll.projecta.user.domain.model.entity.User
import com.vertyll.projecta.user.domain.model.enums.SagaCompensationActions
import com.vertyll.projecta.user.domain.model.enums.SagaStepNames
import com.vertyll.projecta.user.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.user.domain.repository.SagaStepRepository
import com.vertyll.projecta.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Service that handles compensation actions for the User Service
 */
@Service
class SagaCompensationService(
    private val userRepository: UserRepository,
    private val sagaStepRepository: SagaStepRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Listens for compensation events and processes them
     */
    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getSagaCompensationTopic()}"])
    @Transactional
    fun handleCompensationEvent(payload: String) {
        try {
            val event =
                objectMapper.readValue(
                    payload,
                    Map::class.java,
                )
            val sagaId = event["sagaId"] as String
            val actionStr = event["action"] as String

            logger.info("Processing compensation action: $actionStr for saga $sagaId")

            when (actionStr) {
                SagaCompensationActions.DELETE_USER_PROFILE.value -> {
                    val userId = (event["userId"] as Number).toLong()
                    deleteUserProfile(userId)
                }
                SagaCompensationActions.REVERT_USER_PROFILE_UPDATE.value -> {
                    val userId = (event["userId"] as Number).toLong()
                    val originalData = event["originalData"]
                    revertUserProfileUpdate(userId, originalData)
                }
                SagaCompensationActions.REVERT_USER_EMAIL_UPDATE.value -> {
                    val userId = (event["userId"] as Number).toLong()
                    val originalEmail = event["originalEmail"]?.toString()
                    revertUserEmailUpdate(userId, originalEmail)
                }
                SagaCompensationActions.RECREATE_USER_PROFILE.value -> {
                    val userId = (event["userId"] as Number).toLong()
                    val userData = event["userData"]
                    recreateUserProfile(userId, userData)
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
                    val compensationStep =
                        SagaStep(
                            sagaId = sagaId,
                            stepName = SagaStepNames.compensationNameFromString(step.stepName),
                            status = SagaStepStatus.COMPENSATED,
                            createdAt = Instant.now(),
                            completedAt = Instant.now(),
                            compensationStepId = step.id,
                        )
                    sagaStepRepository.save(compensationStep)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process compensation event: ${e.message}", e)
        }
    }

    /**
     * Delete a user profile as part of compensation
     */
    @Transactional
    fun deleteUserProfile(userId: Long) {
        try {
            userRepository.findById(userId).ifPresent { user ->
                logger.info("Compensating by deleting user profile with ID $userId")
                userRepository.delete(user)
            }
        } catch (e: Exception) {
            logger.error("Failed to delete user profile $userId as compensation: ${e.message}", e)
            throw e
        }
    }

    /**
     * Revert a user profile update as part of compensation
     */
    @Transactional
    fun revertUserProfileUpdate(
        userId: Long,
        originalData: Any?,
    ) {
        try {
            val user =
                userRepository.findById(userId).orElse(null) ?: run {
                    logger.warn("Cannot compensate - user $userId not found")
                    return
                }

            logger.info("Compensating by reverting profile update for user $userId")

            if (originalData == null) {
                logger.warn("No original data provided for compensation, cannot revert changes")
                return
            }

            // Convert originalData back to a User object and update fields
            try {
                val originalDataMap =
                    originalData as? Map<*, *> ?: run {
                        logger.error("Original data is not in the expected format")
                        return
                    }

                // Update only the fields that were in the original data
                originalDataMap["firstName"]?.let { user.firstName = it.toString() }
                originalDataMap["lastName"]?.let { user.lastName = it.toString() }
                originalDataMap["email"]?.let { user.setEmail(it.toString()) }
                originalDataMap["profilePicture"]?.let { user.profilePicture = it.toString() }

                userRepository.save(user)
                logger.info("Successfully reverted profile update for user $userId")
            } catch (e: Exception) {
                logger.error("Failed to parse and apply original data: ${e.message}", e)
                throw e
            }
        } catch (e: Exception) {
            logger.error("Failed to revert user profile update for $userId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Revert a user email update as part of compensation
     */
    @Transactional
    fun revertUserEmailUpdate(
        userId: Long,
        originalEmail: String?,
    ) {
        try {
            val user =
                userRepository.findById(userId).orElse(null) ?: run {
                    logger.warn("Cannot compensate - user $userId not found")
                    return
                }

            logger.info("Compensating by reverting email update for user $userId")

            if (originalEmail == null) {
                logger.warn("No original email provided for compensation, cannot revert changes")
                return
            }

            // Set the email back to the original value
            user.setEmail(originalEmail)
            userRepository.save(user)
            logger.info("Successfully reverted email update for user $userId")
        } catch (e: Exception) {
            logger.error("Failed to revert user email update for $userId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Recreate a user profile as part of compensation
     */
    @Transactional
    fun recreateUserProfile(
        userId: Long,
        userData: Any?,
    ) {
        try {
            // Check if user already exists (idempotency check)
            if (userRepository.existsById(userId)) {
                logger.info("User $userId already exists, skipping recreation")
                return
            }

            logger.info("Compensating by recreating user profile $userId")

            if (userData == null) {
                logger.warn("No user data provided for compensation, cannot recreate user")
                return
            }

            try {
                val userDataMap =
                    userData as? Map<*, *> ?: run {
                        logger.error("User data is not in the expected format")
                        return
                    }

                // Extract required fields
                val firstName =
                    userDataMap["firstName"]?.toString() ?: run {
                        logger.error("Missing firstName in user data")
                        return
                    }

                val lastName =
                    userDataMap["lastName"]?.toString() ?: run {
                        logger.error("Missing lastName in user data")
                        return
                    }

                val email =
                    userDataMap["email"]?.toString() ?: run {
                        logger.error("Missing email in user data")
                        return
                    }

                // Create new user with original data
                val user =
                    User(
                        id = userId,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        profilePicture = userDataMap["profilePicture"]?.toString(),
                        phoneNumber = userDataMap["phoneNumber"]?.toString(),
                        address = userDataMap["address"]?.toString(),
                    )

                userRepository.save(user)
                logger.info("Successfully recreated user $userId")
            } catch (e: Exception) {
                logger.error("Failed to parse and apply user data: ${e.message}", e)
                throw e
            }
        } catch (e: Exception) {
            logger.error("Failed to recreate user $userId: ${e.message}", e)
            throw e
        }
    }
}
