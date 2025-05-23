package com.vertyll.projecta.mail.domain.model

enum class SagaStepNames(val value: String) {
    SEND_EMAIL("SendEmail"),
    PROCESS_TEMPLATE("ProcessTemplate"),
    RECORD_EMAIL_LOG("RecordEmailLog");

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"

        fun fromString(value: String): SagaStepNames? {
            return SagaStepNames.entries.find { it.value == value }
        }

        fun compensationName(step: SagaStepNames): String = "$COMPENSATION_PREFIX${step.value}"

        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
}
