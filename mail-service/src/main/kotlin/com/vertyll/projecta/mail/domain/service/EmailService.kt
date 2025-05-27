package com.vertyll.projecta.mail.domain.service

import com.vertyll.projecta.common.mail.EmailTemplate
import com.vertyll.projecta.mail.domain.model.entity.EmailLog
import com.vertyll.projecta.mail.domain.model.enums.EmailStatus
import com.vertyll.projecta.mail.domain.repository.EmailLogRepository
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.time.Instant

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    private val emailLogRepository: EmailLogRepository
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    @Value("\${spring.mail.from}")
    private lateinit var fromEmail: String

    companion object {
        private const val CHARSET_UTF8 = "UTF-8"

        // Log messages
        private const val LOG_SENDING_EMAIL = "Sending email to {} with subject: {}"
        private const val LOG_SEND_FAILURE = "Failed to send email to {} with subject: {}"
    }

    /**
     * Sends an email using a template specified by the EmailTemplate enum and variables
     *
     * @param to email recipient
     * @param subject email subject
     * @param template the EmailTemplate to use
     * @param variables variables to use in the template
     * @param replyTo optional reply-to address
     * @return true if email was sent successfully, false otherwise
     */
    fun sendEmail(
        to: String,
        subject: String,
        template: EmailTemplate,
        variables: Map<String, String>,
        replyTo: String? = null
    ): Boolean {
        try {
            logger.info(LOG_SENDING_EMAIL, to, subject)

            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, CHARSET_UTF8)

            helper.setFrom(fromEmail)
            helper.setTo(to)
            helper.setSubject(subject)

            // Set reply-to address if provided
            if (replyTo != null) {
                helper.setReplyTo(replyTo)
            }

            // Process template
            val context = Context()
            variables.forEach { (key, value) ->
                context.setVariable(key, value)
            }

            val htmlContent = templateEngine.process(template.templateName, context)
            helper.setText(htmlContent, true)

            mailSender.send(message)

            saveEmailLog(
                recipient = to,
                subject = subject,
                templateName = template.templateName,
                variables = formatVariablesForStorage(variables),
                replyTo = replyTo,
                success = true
            )

            return true
        } catch (e: Exception) {
            logger.error(LOG_SEND_FAILURE, to, subject, e)

            saveEmailLog(
                recipient = to,
                subject = subject,
                templateName = template.templateName,
                variables = formatVariablesForStorage(variables),
                replyTo = replyTo,
                success = false,
                errorMessage = e.message
            )

            return false
        }
    }

    private fun formatVariablesForStorage(variables: Map<String, String>): String? {
        if (variables.isEmpty()) {
            return null
        }

        return variables.entries.joinToString(", ") { (key, value) ->
            if (value.length > 50) {
                "$key: ${value.take(50)}..."
            } else {
                "$key: $value"
            }
        }
    }

    private fun saveEmailLog(
        recipient: String,
        subject: String,
        templateName: String,
        variables: String? = null,
        replyTo: String? = null,
        success: Boolean,
        errorMessage: String? = null
    ) {
        val emailLog = EmailLog(
            recipient = recipient,
            subject = subject,
            templateName = templateName,
            variables = variables,
            replyTo = replyTo,
            status = if (success) EmailStatus.SENT else EmailStatus.FAILED,
            errorMessage = errorMessage,
            sentAt = if (success) Instant.now() else null
        )

        emailLogRepository.save(emailLog)
    }
}
