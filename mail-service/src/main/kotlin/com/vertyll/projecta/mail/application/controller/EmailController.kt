package com.vertyll.projecta.mail.application.controller

import com.vertyll.projecta.common.mail.EmailTemplate
import com.vertyll.projecta.common.response.ApiResponse
import com.vertyll.projecta.mail.domain.dto.EmailResult
import com.vertyll.projecta.mail.domain.dto.SendBatchEmailRequest
import com.vertyll.projecta.mail.domain.dto.SendBatchEmailResponse
import com.vertyll.projecta.mail.domain.dto.SendEmailRequest
import com.vertyll.projecta.mail.domain.dto.SendEmailResponse
import com.vertyll.projecta.mail.domain.service.EmailSagaService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for managing emails
 */
@RestController
@RequestMapping("/mail")
class EmailController(
    private val emailSagaService: EmailSagaService
) {
    /**
     * Endpoint for manually sending an email
     */
    @PostMapping("/send")
    fun sendEmail(
        @Valid @RequestBody
        request: SendEmailRequest
    ): ResponseEntity<ApiResponse<SendEmailResponse>> {
        val template = EmailTemplate.fromTemplateName(request.templateName)
            ?: return ApiResponse.buildResponse(
                data = null,
                message = "Invalid template name: ${request.templateName}",
                status = HttpStatus.BAD_REQUEST
            )

        val success = emailSagaService.sendEmailWithSaga(
            to = request.to,
            subject = request.subject,
            template = template,
            variables = request.variables,
            replyTo = request.replyTo
        )

        return if (success) {
            ApiResponse.buildResponse(
                data = SendEmailResponse(
                    success = true,
                    message = "Email successfully sent to ${request.to}"
                ),
                message = "Email successfully sent to ${request.to}",
                status = HttpStatus.OK
            )
        } else {
            ApiResponse.buildResponse(
                data = SendEmailResponse(
                    success = false,
                    message = "Failed to send email to ${request.to}"
                ),
                message = "Failed to send email to ${request.to}",
                status = HttpStatus.OK
            )
        }
    }

    /**
     * Endpoint for sending batch emails
     */
    @PostMapping("/send-batch")
    fun sendBatchEmail(
        @Valid @RequestBody
        request: SendBatchEmailRequest
    ): ResponseEntity<ApiResponse<SendBatchEmailResponse>> {
        val template = EmailTemplate.fromTemplateName(request.templateName)
            ?: return ApiResponse.buildResponse(
                data = null,
                message = "Invalid template name: ${request.templateName}",
                status = HttpStatus.BAD_REQUEST
            )

        val results = emailSagaService.processEmailBatch(
            recipients = request.recipients,
            subject = request.subject,
            template = template,
            commonVariables = request.commonVariables,
            specificVariables = request.specificVariables,
            replyTo = request.replyTo
        )

        val successCount = results.count { it.value }
        val failureCount = results.size - successCount

        return ApiResponse.buildResponse(
            data = SendBatchEmailResponse(
                totalRecipients = results.size,
                successCount = successCount,
                failureCount = failureCount,
                details = results.map { (recipient, success) ->
                    EmailResult(recipient, success)
                }
            ),
            message = "Batch email processing completed. Success: $successCount, Failed: $failureCount",
            status = HttpStatus.OK
        )
    }
}
