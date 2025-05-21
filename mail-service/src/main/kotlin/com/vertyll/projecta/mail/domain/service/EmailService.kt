package com.vertyll.projecta.mail.domain.service

import com.vertyll.projecta.mail.domain.model.EmailLog
import com.vertyll.projecta.mail.domain.repository.EmailLogRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.time.Instant
import jakarta.mail.internet.MimeMessage

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
        // Email charset
        private const val CHARSET_UTF8 = "UTF-8"
        
        // Log messages
        private const val LOG_SENDING_EMAIL = "Sending email to {} with subject: {}"
        private const val LOG_SEND_FAILURE = "Failed to send email to {} with subject: {}"
    }

    /**
     * Sends an email using a template and variables
     *
     * @param to email recipient
     * @param subject email subject
     * @param templateName name of the template to use
     * @param variables variables to use in the template
     * @return true if email was sent successfully, false otherwise
     */
    fun sendEmail(
        to: String,
        subject: String,
        templateName: String,
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

            val htmlContent = templateEngine.process(templateName, context)
            helper.setText(htmlContent, true)

            // Send message
            mailSender.send(message)

            // Log email
            saveEmailLog(
                recipient = to,
                subject = subject,
                templateName = templateName,
                variables = formatVariablesForStorage(variables),
                replyTo = replyTo,
                success = true
            )

            return true
        } catch (e: Exception) {
            logger.error(LOG_SEND_FAILURE, to, subject, e)

            // Log failed email
            saveEmailLog(
                recipient = to,
                subject = subject,
                templateName = templateName,
                variables = formatVariablesForStorage(variables),
                replyTo = replyTo,
                success = false,
                errorMessage = e.message
            )

            return false
        }
    }
    
    /**
     * Formats variables map as a string for storage in database
     */
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
            status = if (success) EmailLog.EmailStatus.SENT else EmailLog.EmailStatus.FAILED,
            errorMessage = errorMessage,
            sentAt = if (success) Instant.now() else null
        )

        emailLogRepository.save(emailLog)
    }
}
