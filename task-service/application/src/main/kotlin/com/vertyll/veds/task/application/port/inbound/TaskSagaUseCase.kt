package com.vertyll.veds.task.application.port.inbound

import com.vertyll.veds.task.domain.model.Task

/**
 * Driving port for the saga-driven task-processing use case.
 *
 * Reference contract — replace with the real use cases when cloning the
 * task service for a new bounded context.
 */
@Suppress("kotlin:S6517")
interface TaskSagaUseCase {
    fun processTaskWithSaga(
        name: String,
        payload: String,
    ): Task
}
