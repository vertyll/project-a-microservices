package com.vertyll.projecta.role.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.common.kafka.KafkaTopicNames
import com.vertyll.projecta.role.domain.model.Saga
import com.vertyll.projecta.role.domain.model.SagaCompensationActions
import com.vertyll.projecta.role.domain.model.SagaStatus
import com.vertyll.projecta.role.domain.model.SagaStep
import com.vertyll.projecta.role.domain.model.SagaStepNames
import com.vertyll.projecta.role.domain.model.SagaStepStatus
import com.vertyll.projecta.role.domain.model.SagaTypes
import com.vertyll.projecta.role.domain.repository.SagaRepository
import com.vertyll.projecta.role.domain.repository.SagaStepRepository
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
        SagaTypes.ROLE_CREATION.value to listOf(SagaStepNames.CREATE_ROLE.value),
        SagaTypes.ROLE_UPDATE.value to listOf(SagaStepNames.UPDATE_ROLE.value),
        SagaTypes.ROLE_ASSIGNMENT.value to listOf(SagaStepNames.ASSIGN_ROLE.value),
        SagaTypes.ROLE_REVOCATION.value to listOf(SagaStepNames.REVOKE_ROLE.value),
        SagaTypes.ROLE_DELETION.value to listOf(SagaStepNames.DELETE_ROLE.value)
    )

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
                    SagaStepNames.CREATE_ROLE.value -> compensateCreateRole(saga.id, step)
                    SagaStepNames.ASSIGN_ROLE.value -> compensateAssignRole(saga.id, step)
                    SagaStepNames.REVOKE_ROLE.value -> compensateRevokeRole(saga.id, step)
                    SagaStepNames.UPDATE_ROLE.value -> compensateUpdateRole(saga.id, step)
                    SagaStepNames.DELETE_ROLE.value -> compensateDeleteRole(saga.id, step)
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
     * Compensate for creating a role
     */
    private fun compensateCreateRole(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val roleId = (payload["roleId"] as Number).toLong()

            // Send compensation event to delete the role
            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload = mapOf(
                    "sagaId" to sagaId,
                    "stepId" to step.id,
                    "action" to SagaCompensationActions.DELETE_ROLE.value,
                    "roleId" to roleId
                ),
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for CreateRole: ${e.message}", e)
        }
    }

    /**
     * Compensate for assigning a role
     */
    private fun compensateAssignRole(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as Number).toLong()
            val roleId = (payload["roleId"] as Number).toLong()

            // Send compensation event to revoke the role
            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload = mapOf(
                    "sagaId" to sagaId,
                    "stepId" to step.id,
                    "action" to SagaCompensationActions.REVOKE_ROLE.value,
                    "userId" to userId,
                    "roleId" to roleId
                ),
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for AssignRole: ${e.message}", e)
        }
    }

    /**
     * Compensate for revoking a role
     */
    private fun compensateRevokeRole(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as Number).toLong()
            val roleId = (payload["roleId"] as Number).toLong()
            val roleName = payload["roleName"] as String

            // Send compensation event to assign the role back
            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload = mapOf(
                    "sagaId" to sagaId,
                    "stepId" to step.id,
                    "action" to SagaCompensationActions.ASSIGN_ROLE.value,
                    "userId" to userId,
                    "roleId" to roleId,
                    "roleName" to roleName
                ),
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for RevokeRole: ${e.message}", e)
        }
    }

    /**
     * Compensate for updating a role
     */
    private fun compensateUpdateRole(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val roleId = (payload["roleId"] as Number).toLong()
            val originalData = payload["originalData"] as? Map<*, *>

            if (originalData != null) {
                // Send compensation event to revert the role update
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.SAGA_COMPENSATION,
                    key = sagaId,
                    payload = mapOf(
                        "sagaId" to sagaId,
                        "stepId" to step.id,
                        "action" to SagaCompensationActions.REVERT_ROLE_UPDATE.value,
                        "roleId" to roleId,
                        "originalData" to originalData
                    ),
                    sagaId = sagaId
                )
            } else {
                logger.warn("No original data available for compensating role update: $roleId")
            }
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for UpdateRole: ${e.message}", e)
        }
    }

    /**
     * Compensate for deleting a role
     */
    private fun compensateDeleteRole(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val roleId = (payload["roleId"] as Number).toLong()
            val originalData = payload["originalData"] as? Map<*, *>

            if (originalData != null) {
                // Send compensation event to recreate the role
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.SAGA_COMPENSATION,
                    key = sagaId,
                    payload = mapOf(
                        "sagaId" to sagaId,
                        "stepId" to step.id,
                        "action" to SagaCompensationActions.RECREATE_ROLE.value,
                        "roleId" to roleId,
                        "originalData" to originalData
                    ),
                    sagaId = sagaId
                )
            } else {
                logger.warn("No original data available for compensating role deletion: $roleId")
            }
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for DeleteRole: ${e.message}", e)
        }
    }
}
