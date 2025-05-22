package com.vertyll.projecta.common.saga

enum class SagaStepNames(val value: String) {
    // Auth service steps
    CREATE_AUTH_USER("CreateAuthUser"),
    CREATE_USER_EVENT("CreateUserEvent"),
    CREATE_VERIFICATION_TOKEN("CreateVerificationToken"),
    CREATE_MAIL_EVENT("CreateMailEvent"),
    
    // User service steps
    CREATE_USER("CreateUser"),
    UPDATE_USER_PROFILE("UpdateUserProfile"),
    DELETE_USER("DeleteUser"),
    
    // Role service steps
    CREATE_ROLE("CreateRole"),
    UPDATE_ROLE("UpdateRole"),
    ASSIGN_ROLE("AssignRole"),
    REVOKE_ROLE("RevokeRole"),
    
    // Mail service steps
    SEND_EMAIL("SendEmail");
    
    companion object {
        const val COMPENSATION_PREFIX = "Compensate"

        fun fromString(value: String): SagaStepNames? {
            return SagaStepNames.entries.find { it.value == value }
        }

        fun compensationName(step: SagaStepNames): String = "$COMPENSATION_PREFIX${step.value}"

        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
} 