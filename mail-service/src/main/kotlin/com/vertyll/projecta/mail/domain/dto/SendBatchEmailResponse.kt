package com.vertyll.projecta.mail.domain.dto

data class SendBatchEmailResponse(
    val totalRecipients: Int,
    val successCount: Int,
    val failureCount: Int,
    val details: List<EmailResult>,
)
