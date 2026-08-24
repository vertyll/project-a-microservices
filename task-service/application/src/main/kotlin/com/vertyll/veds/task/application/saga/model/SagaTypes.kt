package com.vertyll.veds.task.application.saga.model

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    TASK_PROCESSING("TaskProcessing"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = SagaTypes.entries.find { it.value == value }
    }
}
