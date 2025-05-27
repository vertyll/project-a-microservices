package com.vertyll.projecta.role.domain.model.enums

enum class SagaStepNames(val value: String) {
    CREATE_ROLE("CreateRole"),
    ASSIGN_ROLE("AssignRole"),
    REVOKE_ROLE("RevokeRole"),
    UPDATE_ROLE("UpdateRole"),
    DELETE_ROLE("DeleteRole");

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"

        fun fromString(value: String): SagaStepNames? {
            return SagaStepNames.entries.find { it.value == value }
        }

        fun compensationName(step: SagaStepNames): String = "$COMPENSATION_PREFIX${step.value}"

        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
}
