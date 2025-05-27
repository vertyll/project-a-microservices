package com.vertyll.projecta.mail.domain.service

import com.vertyll.projecta.common.mail.EmailTemplate
import com.vertyll.projecta.mail.domain.model.enums.SagaStepNames
import com.vertyll.projecta.mail.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.mail.domain.model.enums.SagaTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service that handles email sending with saga pattern
 */
@Service
class EmailSagaService(
    private val sagaManager: SagaManager,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Sends an email with saga tracking
     */
    @Transactional
    fun sendEmailWithSaga(
        to: String,
        subject: String,
        template: EmailTemplate,
        variables: Map<String, String>,
        replyTo: String? = null
    ): Boolean {
        val sagaId = sagaManager.startSaga(
            sagaType = SagaTypes.EMAIL_SENDING,
            payload = mapOf(
                "to" to to,
                "subject" to subject,
                "templateName" to template.templateName,
                "variables" to variables,
                "replyTo" to replyTo
            )
        ).id

        try {
            // Process template
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PROCESS_TEMPLATE,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf(
                    "templateName" to template.templateName,
                    "variables" to variables
                )
            )

            // Send email
            val success = emailService.sendEmail(to, subject, template, variables, replyTo)

            if (success) {
                sagaManager.recordSagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.SEND_EMAIL,
                    status = SagaStepStatus.COMPLETED,
                    payload = mapOf(
                        "to" to to,
                        "subject" to subject,
                        "emailId" to sagaId
                    )
                )
                return true
            } else {
                sagaManager.recordSagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.SEND_EMAIL,
                    status = SagaStepStatus.FAILED,
                    payload = mapOf(
                        "to" to to,
                        "subject" to subject,
                        "error" to "Failed to send email"
                    )
                )
                return false
            }
        } catch (e: Exception) {
            logger.error("Error in email saga: ${e.message}", e)
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.SEND_EMAIL,
                status = SagaStepStatus.FAILED,
                payload = mapOf(
                    "error" to e.message
                )
            )
            return false
        }
    }

    /**
     * Processes a template and sends an email with batch processing saga tracking
     */
    @Transactional
    fun processEmailBatch(
        recipients: List<String>,
        subject: String,
        template: EmailTemplate,
        commonVariables: Map<String, String>,
        specificVariables: Map<String, Map<String, String>> = emptyMap(),
        replyTo: String? = null
    ): Map<String, Boolean> {
        val sagaId = sagaManager.startSaga(
            sagaType = SagaTypes.EMAIL_BATCH_PROCESSING,
            payload = mapOf(
                "recipients" to recipients,
                "subject" to subject,
                "templateName" to template.templateName,
                "commonVariables" to commonVariables,
                "specificVariables" to specificVariables,
                "replyTo" to replyTo
            )
        ).id

        val results = mutableMapOf<String, Boolean>()

        try {
            // Process template
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PROCESS_TEMPLATE,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf(
                    "templateName" to template.templateName,
                    "variables" to commonVariables
                )
            )

            // Send emails to all recipients
            recipients.forEach { recipient ->
                val recipientVariables = commonVariables.toMutableMap()
                // Add recipient-specific variables if they exist
                specificVariables[recipient]?.let { recipientVariables.putAll(it) }

                val success = emailService.sendEmail(recipient, subject, template, recipientVariables, replyTo)
                results[recipient] = success
            }

            // Record email log
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.RECORD_EMAIL_LOG,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf(
                    "logId" to sagaId,
                    "results" to results
                )
            )

            // Record send email step
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.SEND_EMAIL,
                status = if (results.values.all { it }) SagaStepStatus.COMPLETED else SagaStepStatus.PARTIALLY_COMPLETED,
                payload = mapOf(
                    "recipients" to recipients,
                    "subject" to subject,
                    "results" to results
                )
            )

            return results
        } catch (e: Exception) {
            logger.error("Error in email batch saga: ${e.message}", e)
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.SEND_EMAIL,
                status = SagaStepStatus.FAILED,
                payload = mapOf(
                    "error" to e.message
                )
            )
            return recipients.associateWith { false }
        }
    }
}
