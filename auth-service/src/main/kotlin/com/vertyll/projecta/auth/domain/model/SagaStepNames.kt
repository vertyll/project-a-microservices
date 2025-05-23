package com.vertyll.projecta.auth.domain.model

enum class SagaStepNames(val value: String) {
    // Auth service steps
    CREATE_AUTH_USER("CreateAuthUser"),
    CREATE_USER_EVENT("CreateUserEvent"),
    CREATE_VERIFICATION_TOKEN("CreateVerificationToken"),
    CREATE_MAIL_EVENT("CreateMailEvent");

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"

        fun fromString(value: String): SagaStepNames? {
            return SagaStepNames.entries.find { it.value == value }
        }

        fun compensationName(step: SagaStepNames): String = "$COMPENSATION_PREFIX${step.value}"

        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
}
