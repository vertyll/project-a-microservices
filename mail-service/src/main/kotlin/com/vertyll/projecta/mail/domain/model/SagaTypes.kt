package com.vertyll.projecta.mail.domain.model

enum class SagaTypes(val value: String) {
    // Email related sagas
    EMAIL_SENDING("EmailSending"),
    EMAIL_NOTIFICATION("EmailNotification"),
    TEMPLATE_PROCESSING("TemplateProcessing"),
    EMAIL_BATCH_PROCESSING("EmailBatchProcessing");

    companion object {
        fun fromString(value: String): SagaTypes? {
            return SagaTypes.entries.find { it.value == value }
        }
    }
}
