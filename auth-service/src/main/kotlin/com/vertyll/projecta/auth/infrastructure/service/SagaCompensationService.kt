package com.vertyll.projecta.auth.infrastructure.service

import com.vertyll.projecta.auth.domain.model.entity.SagaStep
import com.vertyll.projecta.auth.domain.model.enums.SagaStepNames
import com.vertyll.projecta.auth.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.auth.domain.repository.AuthUserRepository
import com.vertyll.projecta.auth.domain.repository.SagaStepRepository
import com.vertyll.projecta.auth.domain.repository.VerificationTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

/**
 * Service that handles compensation actions for the Auth Service
 */
@Service
class SagaCompensationService(
    private val authUserRepository: AuthUserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
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
            val stepId = (event["stepId"] as Number).toLong()

            // Find the step that needs compensation
            val step =
                sagaStepRepository.findById(stepId).orElse(null) ?: run {
                    logger.error("Cannot find saga step with ID $stepId for compensation")
                    return
                }

            logger.info("Processing compensation for saga $sagaId, step ${step.stepName}")

            // Perform compensation based on step name
            when (step.stepName) {
                "CreateAuthUser" -> compensateCreateAuthUser(step)
                "CreateVerificationToken" -> compensateCreateVerificationToken(step)
                else -> logger.warn("No compensation handler for step ${step.stepName}")
            }

            // Record that compensation was completed
            val compensationStep =
                SagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.compensationNameFromString(step.stepName),
                    status = SagaStepStatus.COMPENSATED,
                    createdAt = java.time.Instant.now(),
                    completedAt = java.time.Instant.now(),
                    compensationStepId = step.id,
                )
            sagaStepRepository.save(compensationStep)
        } catch (e: Exception) {
            logger.error("Failed to process compensation event: ${e.message}", e)
        }
    }

    /**
     * Compensate for creating an auth user by deleting it
     */
    @Transactional
    fun compensateCreateAuthUser(step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val authUserId = (payload["authUserId"] as Number).toLong()

            authUserRepository.findById(authUserId).ifPresent { user ->
                logger.info("Compensating CreateAuthUser step: Deleting auth user with ID $authUserId")
                authUserRepository.delete(user)
            }
        } catch (e: Exception) {
            logger.error("Failed to compensate CreateAuthUser step: ${e.message}", e)
            throw e
        }
    }

    /**
     * Compensate for creating a verification token by deleting it
     */
    @Transactional
    fun compensateCreateVerificationToken(step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val tokenId = (payload["tokenId"] as Number).toLong()

            verificationTokenRepository.findById(tokenId).ifPresent { token ->
                logger.info("Compensating CreateVerificationToken step: Deleting token with ID $tokenId")
                verificationTokenRepository.delete(token)
            }
        } catch (e: Exception) {
            logger.error("Failed to compensate CreateVerificationToken step: ${e.message}", e)
            throw e
        }
    }
}
