package com.vertyll.projecta.user.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.common.kafka.KafkaTopicNames
import com.vertyll.projecta.user.domain.model.entity.Saga
import com.vertyll.projecta.user.domain.model.entity.SagaStep
import com.vertyll.projecta.user.domain.model.enums.SagaCompensationActions
import com.vertyll.projecta.user.domain.model.enums.SagaStatus
import com.vertyll.projecta.user.domain.model.enums.SagaStepNames
import com.vertyll.projecta.user.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.user.domain.model.enums.SagaTypes
import com.vertyll.projecta.user.domain.repository.SagaRepository
import com.vertyll.projecta.user.domain.repository.SagaStepRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Manages the state of sagas and coordinates compensating transactions.
 */
@Service
class SagaManager(
    private val sagaRepository: SagaRepository,
    private val sagaStepRepository: SagaStepRepository,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Define the expected steps for each saga type
    private val sagaStepDefinitions = mapOf(
        SagaTypes.USER_REGISTRATION.value to listOf(
            SagaStepNames.CREATE_USER_PROFILE.value
        ),
        SagaTypes.USER_UPDATE.value to listOf(
            SagaStepNames.UPDATE_USER_PROFILE.value
        ),
        SagaTypes.USER_DELETION.value to listOf(
            SagaStepNames.DELETE_USER_PROFILE.value
        ),
        SagaTypes.USER_PASSWORD_CHANGE.value to listOf(
            SagaStepNames.UPDATE_USER_CREDENTIALS.value
        ),
        SagaTypes.USER_EMAIL_CHANGE.value to listOf(
            SagaStepNames.UPDATE_USER_EMAIL.value
        )
    )

    /**
     * Starts a new saga
     * @param sagaType The type of saga to start
     * @param payload Additional data related to the saga
     * @return The created saga instance
     */
    @Transactional
    fun startSaga(sagaType: SagaTypes, payload: Any): Saga {
        val payloadJson = payload as? String ?: objectMapper.writeValueAsString(payload)

        val saga = Saga(
            id = UUID.randomUUID().toString(),
            type = sagaType.value,
            status = SagaStatus.STARTED,
            payload = payloadJson,
            startedAt = Instant.now()
        )

        return sagaRepository.save(saga)
    }

    /**
     * Records a step in a saga
     * @param sagaId The ID of the saga
     * @param stepName The name of the step
     * @param status The status of the step
     * @param payload Additional data related to the step
     * @return The created saga step
     */
    @Transactional
    fun recordSagaStep(sagaId: String, stepName: SagaStepNames, status: SagaStepStatus, payload: Any? = null): SagaStep {
        val payloadJson = payload?.let {
            it as? String ?: objectMapper.writeValueAsString(it)
        }

        val saga = sagaRepository.findById(sagaId).orElseThrow {
            IllegalArgumentException("Saga with ID $sagaId not found")
        }

        val step = SagaStep(
            sagaId = sagaId,
            stepName = stepName.value,
            status = status,
            payload = payloadJson,
            createdAt = Instant.now()
        )

        val savedStep = sagaStepRepository.save(step)

        // Update saga status based on step status
        if (status == SagaStepStatus.FAILED) {
            saga.status = SagaStatus.COMPENSATING
            saga.lastError = "Step $stepName failed"
            sagaRepository.save(saga)

            // Trigger compensation
            triggerCompensation(saga)
        } else if (status == SagaStepStatus.COMPLETED) {
            // Check if this is the last step in the saga based on the saga type
            saga.updatedAt = Instant.now()

            // If all expected steps are completed, mark the saga as completed
            if (areAllStepsCompleted(saga)) {
                saga.status = SagaStatus.COMPLETED
                saga.completedAt = Instant.now()
                logger.info("All steps completed for saga ${saga.id}, marking as COMPLETED")
            }

            sagaRepository.save(saga)
        }

        return savedStep
    }

    /**
     * Checks if all expected steps for a saga have been completed
     */
    private fun areAllStepsCompleted(saga: Saga): Boolean {
        // Get the expected steps for this saga type
        val expectedSteps = sagaStepDefinitions[saga.type] ?: return false

        // Get all completed steps for this saga
        val completedSteps = sagaStepRepository.findBySagaIdAndStatus(
            saga.id,
            SagaStepStatus.COMPLETED
        )

        // Get the step names of completed steps
        val completedStepNames = completedSteps.map { it.stepName }

        // Check if all expected steps are in the completed steps
        return expectedSteps.all { expectedStep ->
            completedStepNames.contains(expectedStep)
        }
    }

    /**
     * Triggers compensation for a failed saga
     * @param saga The saga to compensate
     */
    private fun triggerCompensation(saga: Saga) {
        // Get all completed steps for this saga in reverse order
        val completedSteps = sagaStepRepository.findBySagaIdAndStatus(
            saga.id,
            SagaStepStatus.COMPLETED
        ).sortedByDescending { it.createdAt }

        logger.info("Triggering compensation for saga ${saga.id} with ${completedSteps.size} steps to compensate")

        // For each completed step, create a compensation event
        completedSteps.forEach { step ->
            try {
                // Create a compensation event based on the step name
                when (step.stepName) {
                    SagaStepNames.CREATE_USER_PROFILE.value -> compensateCreateUserProfile(saga.id, step)
                    SagaStepNames.UPDATE_USER_PROFILE.value -> compensateUpdateUserProfile(saga.id, step)
                    SagaStepNames.UPDATE_USER_CREDENTIALS.value -> compensateUpdateUserCredentials(saga.id, step)
                    SagaStepNames.UPDATE_USER_EMAIL.value -> compensateUpdateUserEmail(saga.id, step)
                    SagaStepNames.DELETE_USER_PROFILE.value -> compensateDeleteUserProfile(saga.id, step)
                    else -> logger.warn("No compensation defined for step ${step.stepName}")
                }

                // Record compensation step
                sagaStepRepository.save(
                    SagaStep(
                        sagaId = saga.id,
                        stepName = SagaStepNames.compensationNameFromString(step.stepName),
                        status = SagaStepStatus.STARTED,
                        createdAt = Instant.now()
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to create compensation event for step ${step.stepName}: ${e.message}", e)
            }
        }

        // Update saga status
        saga.status = SagaStatus.COMPENSATING
        sagaRepository.save(saga)
    }

    /**
     * Compensate for creating a user profile
     */
    private fun compensateCreateUserProfile(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as Number).toLong()

            // Send compensation event to delete the user profile
            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload = mapOf(
                    "sagaId" to sagaId,
                    "stepId" to step.id,
                    "action" to SagaCompensationActions.DELETE_USER_PROFILE.value,
                    "userId" to userId
                ),
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for CreateUserProfile: ${e.message}", e)
        }
    }

    /**
     * Compensate for updating a user profile
     */
    private fun compensateUpdateUserProfile(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as Number).toLong()
            val originalData = payload["originalData"]

            // Send compensation event to revert the user profile update
            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload = mapOf(
                    "sagaId" to sagaId,
                    "stepId" to step.id,
                    "action" to SagaCompensationActions.REVERT_USER_PROFILE_UPDATE.value,
                    "userId" to userId,
                    "originalData" to originalData
                ),
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for UpdateUserProfile: ${e.message}", e)
        }
    }

    /**
     * Compensate for updating user credentials
     */
    private fun compensateUpdateUserCredentials(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as Number).toLong()
            val originalData = payload["originalData"]

            // Send compensation event to revert the credentials update
            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload = mapOf(
                    "sagaId" to sagaId,
                    "stepId" to step.id,
                    "action" to SagaCompensationActions.REVERT_USER_CREDENTIALS_UPDATE.value,
                    "userId" to userId,
                    "originalData" to originalData
                ),
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for UpdateUserCredentials: ${e.message}", e)
        }
    }

    /**
     * Compensate for updating user email
     */
    private fun compensateUpdateUserEmail(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as Number).toLong()
            val originalEmail = payload["originalEmail"]?.toString()

            if (originalEmail != null) {
                // Send compensation event to revert the email update
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.SAGA_COMPENSATION,
                    key = sagaId,
                    payload = mapOf(
                        "sagaId" to sagaId,
                        "stepId" to step.id,
                        "action" to SagaCompensationActions.REVERT_USER_EMAIL_UPDATE.value,
                        "userId" to userId,
                        "originalEmail" to originalEmail
                    ),
                    sagaId = sagaId
                )
            } else {
                logger.warn("No original email available for compensating email update for user $userId")
            }
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for UpdateUserEmail: ${e.message}", e)
        }
    }

    /**
     * Compensate for deleting a user profile (recreate it)
     */
    private fun compensateDeleteUserProfile(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as Number).toLong()
            val userData = payload["userData"]

            if (userData != null) {
                // Send compensation event to recreate the user profile
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.SAGA_COMPENSATION,
                    key = sagaId,
                    payload = mapOf(
                        "sagaId" to sagaId,
                        "stepId" to step.id,
                        "action" to SagaCompensationActions.RECREATE_USER_PROFILE.value,
                        "userId" to userId,
                        "userData" to userData
                    ),
                    sagaId = sagaId
                )
            } else {
                logger.warn("No user data available for compensating user deletion: $userId")
            }
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for DeleteUserProfile: ${e.message}", e)
        }
    }
}
