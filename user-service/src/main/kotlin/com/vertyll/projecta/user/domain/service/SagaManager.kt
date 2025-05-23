package com.vertyll.projecta.user.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.common.kafka.KafkaTopicNames
import com.vertyll.projecta.user.domain.model.Saga
import com.vertyll.projecta.user.domain.model.SagaStatus
import com.vertyll.projecta.user.domain.model.SagaStep
import com.vertyll.projecta.user.domain.model.SagaStepStatus
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

    /**
     * Starts a new saga
     * @param sagaType The type of saga to start
     * @param payload Additional data related to the saga
     * @return The created saga instance
     */
    @Transactional
    fun startSaga(sagaType: String, payload: Any): Saga {
        val payloadJson = payload as? String ?: objectMapper.writeValueAsString(payload)

        val saga = Saga(
            id = UUID.randomUUID().toString(),
            type = sagaType,
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
    fun recordSagaStep(sagaId: String, stepName: String, status: SagaStepStatus, payload: Any? = null): SagaStep {
        val payloadJson = payload?.let {
            it as? String ?: objectMapper.writeValueAsString(it)
        }

        val saga = sagaRepository.findById(sagaId).orElseThrow {
            IllegalArgumentException("Saga with ID $sagaId not found")
        }

        val step = SagaStep(
            sagaId = sagaId,
            stepName = stepName,
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
            // Check if this is the last step
            // This would require knowing the total steps in the saga
            // For now, we'll just update the saga status
            saga.updatedAt = Instant.now()
            sagaRepository.save(saga)
        }

        return savedStep
    }

    /**
     * Marks a saga as completed
     * @param sagaId The ID of the saga to complete
     * @return The updated saga
     */
    @Transactional
    fun completeSaga(sagaId: String): Saga {
        val saga = sagaRepository.findById(sagaId).orElseThrow {
            IllegalArgumentException("Saga with ID $sagaId not found")
        }

        saga.status = SagaStatus.COMPLETED
        saga.completedAt = Instant.now()

        return sagaRepository.save(saga)
    }

    /**
     * Marks a saga as failed and initiates compensation
     * @param sagaId The ID of the saga that failed
     * @param error The error that caused the failure
     * @return The updated saga
     */
    @Transactional
    fun failSaga(sagaId: String, error: String): Saga {
        val saga = sagaRepository.findById(sagaId).orElseThrow {
            IllegalArgumentException("Saga with ID $sagaId not found")
        }

        saga.status = SagaStatus.FAILED
        saga.lastError = error
        saga.updatedAt = Instant.now()

        val savedSaga = sagaRepository.save(saga)

        // Trigger compensation
        triggerCompensation(savedSaga)

        return savedSaga
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
                    "CreateUserProfile" -> compensateCreateUserProfile(saga.id, step)
                    "UpdateUserProfile" -> compensateUpdateUserProfile(saga.id, step)
                    else -> logger.warn("No compensation defined for step ${step.stepName}")
                }

                // Record compensation step
                sagaStepRepository.save(
                    SagaStep(
                        sagaId = saga.id,
                        stepName = "Compensate${step.stepName}",
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
                    "action" to "DELETE_USER_PROFILE",
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
                    "action" to "REVERT_USER_PROFILE_UPDATE",
                    "userId" to userId,
                    "originalData" to originalData
                ),
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for UpdateUserProfile: ${e.message}", e)
        }
    }
}
