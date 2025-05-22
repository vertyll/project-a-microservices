package com.vertyll.projecta.auth.infrastructure.saga

enum class SagaStepName(val value: String) {
    CREATE_AUTH_USER("CreateAuthUser"),
    CREATE_USER_EVENT("CreateUserEvent"),
    CREATE_VERIFICATION_TOKEN("CreateVerificationToken"),
    CREATE_MAIL_EVENT("CreateMailEvent");
    
    companion object {
        private const val COMPENSATION_PREFIX = "Compensate"
        
        fun fromString(value: String): SagaStepName? = SagaStepName.entries.find { it.value == value }
        fun compensationName(step: SagaStepName): String = "$COMPENSATION_PREFIX${step.value}"
        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
}
