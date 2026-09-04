package com.vertyll.veds.task.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    TASK_PROCESSING("TaskProcessing"),
}
