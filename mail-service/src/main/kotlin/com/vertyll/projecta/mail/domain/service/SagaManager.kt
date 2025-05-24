package com.vertyll.projecta.mail.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.common.kafka.KafkaTopicNames
import com.vertyll.projecta.mail.domain.model.Saga
import com.vertyll.projecta.mail.domain.model.SagaCompensationActions
import com.vertyll.projecta.mail.domain.model.SagaStatus
import com.vertyll.projecta.mail.domain.model.SagaStep
import com.vertyll.projecta.mail.domain.model.SagaStepNames
import com.vertyll.projecta.mail.domain.model.SagaStepStatus
import com.vertyll.projecta.mail.domain.model.SagaTypes
import com.vertyll.projecta.mail.domain.repository.SagaRepository
import com.vertyll.projecta.mail.domain.repository.SagaStepRepository
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
        SagaTypes.EMAIL_SENDING.value to listOf(
            SagaStepNames.PROCESS_TEMPLATE.value,
            SagaStepNames.SEND_EMAIL.value
        ),
        SagaTypes.EMAIL_BATCH_PROCESSING.value to listOf(
            SagaStepNames.PROCESS_TEMPLATE.value,
            SagaStepNames.SEND_EMAIL.value,
            SagaStepNames.RECORD_EMAIL_LOG.value
        ),
        SagaTypes.TEMPLATE_MANAGEMENT.value to listOf(
            SagaStepNames.TEMPLATE_UPDATE.value
        )
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
        } else if (status == SagaStepStatus.COMPLETED || status == SagaStepStatus.PARTIALLY_COMPLETED) {
            // Check if this is the last step in the saga based on the saga type
            saga.updatedAt = Instant.now()

            // If all expected steps are completed, mark the saga as completed
            if (status != SagaStepStatus.PARTIALLY_COMPLETED && areAllStepsCompleted(saga)) {
                saga.status = SagaStatus.COMPLETED
                saga.completedAt = Instant.now()
                logger.info("All steps completed for saga ${saga.id}, marking as COMPLETED")
            } else if (status == SagaStepStatus.PARTIALLY_COMPLETED) {
                saga.status = SagaStatus.PARTIALLY_COMPLETED
                saga.completedAt = Instant.now()
                logger.info("Saga ${saga.id} partially completed")
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
                    SagaStepNames.SEND_EMAIL.value -> compensateSendEmail(saga.id, step)
                    SagaStepNames.RECORD_EMAIL_LOG.value -> compensateRecordEmailLog(saga.id, step)
                    SagaStepNames.TEMPLATE_UPDATE.value -> compensateTemplateUpdate(saga.id, step)
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
     * Compensate for sending an email (for auditing/logging only, can't "unsend" an email)
     */
    private fun compensateSendEmail(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val to = payload["to"]?.toString()
            val emailId = payload["emailId"]?.toString()

            // Send compensation event to log the compensation
            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload = mapOf(
                    "sagaId" to sagaId,
                    "stepId" to step.id,
                    "action" to SagaCompensationActions.LOG_EMAIL_COMPENSATION.value,
                    "emailId" to emailId,
                    "to" to to,
                    "message" to "Email cannot be unsent, compensation logged for auditing purposes"
                ),
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for SendEmail: ${e.message}", e)
        }
    }

    /**
     * Compensate for recording an email log (delete it)
     */
    private fun compensateRecordEmailLog(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val logId = payload["logId"]?.toString()

            if (logId != null) {
                // Send compensation event to delete the email log
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.SAGA_COMPENSATION,
                    key = sagaId,
                    payload = mapOf(
                        "sagaId" to sagaId,
                        "stepId" to step.id,
                        "action" to SagaCompensationActions.DELETE_EMAIL_LOG.value,
                        "logId" to logId
                    ),
                    sagaId = sagaId
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for RecordEmailLog: ${e.message}", e)
        }
    }

    /**
     * Compensate for updating a template (mostly for logging)
     */
    private fun compensateTemplateUpdate(sagaId: String, step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val templateName = payload["templateName"]?.toString()

            // Send compensation event to log the template compensation
            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload = mapOf(
                    "sagaId" to sagaId,
                    "stepId" to step.id,
                    "action" to SagaCompensationActions.LOG_TEMPLATE_COMPENSATION.value,
                    "templateName" to templateName,
                    "message" to "Template update compensation logged"
                ),
                sagaId = sagaId
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for TemplateUpdate: ${e.message}", e)
        }
    }
}
