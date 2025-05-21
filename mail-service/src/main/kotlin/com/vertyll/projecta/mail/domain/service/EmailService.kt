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
        variables: Map<String, String>
    ): Boolean {
        try {
            logger.info("Sending email to {} with subject: {}", to, subject)

            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromEmail)
            helper.setTo(to)
            helper.setSubject(subject)

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
            saveEmailLog(to, subject, templateName, true)

            return true
        } catch (e: Exception) {
            logger.error("Failed to send email to {} with subject: {}", to, subject, e)

            // Log failed email
            saveEmailLog(to, subject, templateName, false, e.message)

            return false
        }
    }

    private fun saveEmailLog(
        recipient: String,
        subject: String,
        templateName: String,
        success: Boolean,
        errorMessage: String? = null
    ) {
        val emailLog = EmailLog(
            recipient = recipient,
            subject = subject,
            templateName = templateName,
            status = if (success) EmailLog.EmailStatus.SENT else EmailLog.EmailStatus.FAILED,
            errorMessage = errorMessage,
            sentAt = if (success) Instant.now() else null
        )

        emailLogRepository.save(emailLog)
    }
}
