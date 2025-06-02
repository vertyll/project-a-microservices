package com.vertyll.projecta.template.domain.model.enums

enum class SagaTypes(val value: String) {
    EXAMPLE_SAGA("exampleSaga");

    companion object {
        fun fromString(value: String): SagaTypes? {
            return SagaTypes.entries.find { it.value == value }
        }
    }
}
