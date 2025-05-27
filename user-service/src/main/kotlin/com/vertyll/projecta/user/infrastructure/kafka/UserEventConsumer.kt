package com.vertyll.projecta.user.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.event.EventSource
import com.vertyll.projecta.common.event.user.UserProfileUpdatedEvent
import com.vertyll.projecta.common.event.user.UserRegisteredEvent
import com.vertyll.projecta.common.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.common.kafka.KafkaTopicNames
import com.vertyll.projecta.common.kafka.KafkaTopicsConfig
import com.vertyll.projecta.user.domain.dto.EmailUpdateDto
import com.vertyll.projecta.user.domain.dto.UserCreateDto
import com.vertyll.projecta.user.domain.model.enums.SagaStepNames
import com.vertyll.projecta.user.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.user.domain.model.enums.SagaTypes
import com.vertyll.projecta.user.domain.repository.UserRepository
import com.vertyll.projecta.user.domain.service.SagaManager
import com.vertyll.projecta.user.domain.service.UserService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserEventConsumer(
    private val objectMapper: ObjectMapper,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val sagaManager: SagaManager,
    @Suppress("unused") private val kafkaTopicsConfig: KafkaTopicsConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getUserRegisteredTopic()}"])
    @Transactional
    fun consume(record: ConsumerRecord<String, String>) {
        try {
            logger.info("Received user registration event with key: ${record.key()}")

            // Deserialize the message payload
            val event = objectMapper.readValue(record.value(), UserRegisteredEvent::class.java)
            logger.info("Deserialized event for user: ${event.email}")

            // Skip events that we sent ourselves to avoid circular processing
            if (event.eventSource == EventSource.USER_SERVICE.value) {
                logger.debug("Ignoring event from User Service - skip circular processing")
                return
            }

            // Get or create saga
            val sagaId = event.sagaId ?: run {
                logger.warn("No saga ID provided in event, creating a new saga")
                sagaManager.startSaga(
                    SagaTypes.USER_REGISTRATION,
                    mapOf("event" to event)
                ).id
            }

            // Start a saga step
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.CREATE_USER_PROFILE,
                status = SagaStepStatus.STARTED
            )

            // Check if user already exists (idempotency check)
            val existingUser = userRepository.findByEmail(event.email).orElse(null)
            if (existingUser != null) {
                logger.info("User ${event.email} already exists with ID ${existingUser.id}, skipping creation")

                // Record step as completed
                sagaManager.recordSagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.CREATE_USER_PROFILE,
                    status = SagaStepStatus.COMPLETED,
                    payload = mapOf("userId" to existingUser.id)
                )

                return
            }

            try {
                // Create user
                handleUserRegisteredEvent(event, sagaId)
            } catch (e: Exception) {
                // Record step as failed
                sagaManager.recordSagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.CREATE_USER_PROFILE,
                    status = SagaStepStatus.FAILED,
                    payload = mapOf("error" to e.message)
                )

                // Send compensation event to notify auth service
                sendCompensationEvent(event, sagaId, e.message ?: "User creation failed")

                // Re-throw to rollback transaction
                throw e
            }
        } catch (e: Exception) {
            logger.error("Error processing user registration event: ${e.message}", e)
        }
    }

    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getUserUpdatedTopic()}"])
    @Transactional
    fun consumeProfileUpdates(record: ConsumerRecord<String, String>) {
        try {
            logger.info("Received user profile update event with key: ${record.key()}")

            // Deserialize the message payload
            val event = objectMapper.readValue(record.value(), UserProfileUpdatedEvent::class.java)
            logger.info("Deserialized profile update event for user: ${event.email}, type: ${event.updateType}")

            // Get or create saga
            val sagaId = event.sagaId ?: sagaManager.startSaga(
                SagaTypes.USER_PROFILE_UPDATE,
                mapOf("event" to event)
            ).id

            // Start a saga step
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.UPDATE_USER_PROFILE,
                status = SagaStepStatus.STARTED
            )

            try {
                // Process the update
                handleUserProfileUpdatedEvent(event)

                // Record step as completed
                sagaManager.recordSagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.UPDATE_USER_PROFILE,
                    status = SagaStepStatus.COMPLETED,
                    payload = mapOf(
                        "email" to event.email,
                        "updateType" to event.updateType
                    )
                )
            } catch (e: Exception) {
                // Record step as failed
                sagaManager.recordSagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.UPDATE_USER_PROFILE,
                    status = SagaStepStatus.FAILED,
                    payload = mapOf("error" to e.message)
                )

                // Re-throw to rollback transaction
                throw e
            }
        } catch (e: Exception) {
            logger.error("Error processing user profile update event: ${e.message}", e)
        }
    }

    @KafkaListener(topics = ["user-deletion"])
    @Transactional
    fun handleDeletion(record: ConsumerRecord<String, String>) {
        try {
            logger.info("Received user deletion event with key: ${record.key()}")

            val payload = objectMapper.readValue(record.value(), Map::class.java)
            val userId = (payload["userId"] as Number).toLong()
            val sagaId = payload["sagaId"] as String

            logger.info("Processing deletion for user ID $userId as part of saga $sagaId")

            // Start a saga step
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.DELETE_USER_PROFILE,
                status = SagaStepStatus.STARTED
            )

            try {
                // Delete the user
                userRepository.findById(userId).ifPresent { user ->
                    userRepository.delete(user)
                    logger.info("Deleted user $userId")
                }

                // Record step as completed
                sagaManager.recordSagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.DELETE_USER_PROFILE,
                    status = SagaStepStatus.COMPLETED,
                    payload = mapOf("userId" to userId)
                )
            } catch (e: Exception) {
                // Record step as failed
                sagaManager.recordSagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.DELETE_USER_PROFILE,
                    status = SagaStepStatus.FAILED,
                    payload = mapOf("error" to e.message)
                )

                // Re-throw to rollback transaction
                throw e
            }
        } catch (e: Exception) {
            logger.error("Error processing user deletion event: ${e.message}", e)
        }
    }

    private fun handleUserRegisteredEvent(event: UserRegisteredEvent, sagaId: String) {
        logger.info("Processing user registration for ${event.email}")

        // Convert the event to a UserCreateDto and create the user profile
        val userCreateDto = UserCreateDto(
            firstName = event.firstName,
            lastName = event.lastName,
            email = event.email,
            roles = event.roles
        )

        try {
            val createdUser = userService.createUser(userCreateDto)
            logger.info("Successfully created user profile ${createdUser.id} for ${event.email}")

            // Record step as completed
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.CREATE_USER_PROFILE,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf(
                    "userId" to createdUser.id,
                    "email" to createdUser.email
                )
            )

            // Send event to notify successful user creation
            val creationConfirmedEvent = mapOf(
                "userId" to createdUser.id,
                "email" to createdUser.email,
                "status" to "CREATED",
                "sagaId" to sagaId
            )

            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.USER_CREATION_CONFIRMED,
                key = createdUser.id.toString(),
                payload = creationConfirmedEvent,
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create user profile for ${event.email}", e)
            // This will be handled by the outer try-catch, which will record the failure and send a compensation event
            throw e
        }
    }

    private fun handleUserProfileUpdatedEvent(event: UserProfileUpdatedEvent) {
        logger.info("Processing user profile update for ${event.email}, type: ${event.updateType}")

        try {
            when (event.updateType) {
                UserProfileUpdatedEvent.UpdateType.EMAIL -> {
                    // Event should contain the new email in updatedFields
                    val newEmail = event.updatedFields["email"] ?: run {
                        logger.error("Missing email in update fields for email change event")
                        return
                    }

                    userService.updateEmail(
                        EmailUpdateDto(
                            currentEmail = event.email,
                            newEmail = newEmail
                        )
                    )
                    logger.info("Successfully updated email from ${event.email} to $newEmail")
                }

                UserProfileUpdatedEvent.UpdateType.PASSWORD -> {
                    // Password changes are handled by Auth Service, no action needed here
                    logger.info("Received password change notification for user ${event.email}")
                }

                UserProfileUpdatedEvent.UpdateType.PROFILE -> {
                    // For any other profile fields that need to be updated
                    logger.info(
                        "Received profile update notification for user ${event.email} with fields: ${event.updatedFields.keys}"
                    )
                    // Additional processing for specific fields could be added here
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process profile update for ${event.email}: ${e.message}", e)
            throw e
        }
    }

    private fun sendCompensationEvent(event: UserRegisteredEvent, sagaId: String, errorMessage: String) {
        val compensationEvent = mapOf(
            "userId" to event.userId,
            "email" to event.email,
            "sagaId" to sagaId,
            "errorMessage" to errorMessage,
            "action" to "COMPENSATE_USER_CREATION"
        )

        kafkaOutboxProcessor.saveOutboxMessage(
            topic = KafkaTopicNames.SAGA_COMPENSATION,
            key = sagaId,
            payload = compensationEvent,
            sagaId = sagaId
        )

        logger.info("Sent compensation event for user ${event.email} in saga $sagaId")
    }
}
