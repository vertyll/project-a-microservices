package com.vertyll.projecta.common.saga

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.kafka.KafkaOutboxProcessor
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
    fun recordSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: Any? = null
    ): SagaStep {
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
                    "CreateUser" -> createUserCompensationEvent(saga.id, step)
                    "AssignRole" -> createRoleAssignmentCompensationEvent(saga.id, step)
                    "SendEmail" -> createEmailCompensationEvent(saga.id, step)
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
     * Creates a compensation event for user creation
     */
    private fun createUserCompensationEvent(sagaId: String, step: SagaStep) {
        // Extract user ID from step payload
        val payload = objectMapper.readValue(step.payload, Map::class.java)
        val userId = payload["userId"] as Long
        
        // Create an outbox message for user deletion
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = "user-deletion",
            key = userId.toString(),
            payload = mapOf(
                "userId" to userId,
                "sagaId" to sagaId,
                "compensationFor" to step.id
            ),
            sagaId = sagaId
        )
    }
    
    /**
     * Creates a compensation event for role assignment
     */
    private fun createRoleAssignmentCompensationEvent(sagaId: String, step: SagaStep) {
        // Extract role assignment details from step payload
        val payload = objectMapper.readValue(step.payload, Map::class.java)
        val userId = payload["userId"] as Long
        val roleId = payload["roleId"] as Long
        
        // Create an outbox message for role revocation
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = "role-revoked",
            key = "$userId-$roleId",
            payload = mapOf(
                "userId" to userId,
                "roleId" to roleId,
                "sagaId" to sagaId,
                "compensationFor" to step.id
            ),
            sagaId = sagaId
        )
    }
    
    /**
     * Creates a compensation event for email sending
     */
    private fun createEmailCompensationEvent(sagaId: String, step: SagaStep) {
        // For emails, there's typically no compensation - just record that we would have sent a compensation email
        logger.info("Email compensation would be sent for step ${step.id}")
        
        // Optionally, we could send a "correction" email
        val payload = objectMapper.readValue(step.payload, Map::class.java)
        val to = payload["to"] as String
        
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = "mail-requested",
            key = UUID.randomUUID().toString(),
            payload = mapOf(
                "to" to to,
                "subject" to "Correction Notice",
                "body" to "Please disregard our previous email as it was sent in error.",
                "sagaId" to sagaId,
                "compensationFor" to step.id
            ),
            sagaId = sagaId
        )
    }
}
