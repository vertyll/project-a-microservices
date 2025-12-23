package com.vertyll.projecta.user.domain.model.enums

enum class SagaStepNames(
    val value: String,
) {
    CREATE_USER_PROFILE("CreateUserProfile"),
    UPDATE_USER_PROFILE("UpdateUserProfile"),
    DELETE_USER_PROFILE("DeleteUserProfile"),
    UPDATE_USER_CREDENTIALS("UpdateUserCredentials"),
    UPDATE_USER_EMAIL("UpdateUserEmail"),
    ;

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"

        fun fromString(value: String): SagaStepNames? = SagaStepNames.entries.find { it.value == value }

        fun compensationName(step: SagaStepNames): String = "$COMPENSATION_PREFIX${step.value}"

        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
}
