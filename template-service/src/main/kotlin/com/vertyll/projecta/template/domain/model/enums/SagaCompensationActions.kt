package com.vertyll.projecta.template.domain.model.enums

enum class SagaCompensationActions(val value: String) {
    EXAMPLE_COMPENSATION("exampleCompensation");

    companion object {
        fun fromString(value: String): SagaCompensationActions? {
            return SagaCompensationActions.entries.find { it.value == value }
        }
    }
}
