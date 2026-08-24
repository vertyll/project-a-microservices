package com.vertyll.veds.task.application.service

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.task.application.port.inbound.TaskSagaUseCase
import com.vertyll.veds.task.application.port.outbound.SagaProcessPort
import com.vertyll.veds.task.application.saga.model.SagaStepNames
import com.vertyll.veds.task.application.saga.model.SagaTypes
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reference implementation of a saga-driven use case for the task service.
 *
 * Mirrors the structure of `EmailSagaService` in mail-service — replace the
 * domain calls with your real business logic when cloning this service.
 */
@Service
internal class TaskSagaService(
    private val sagaProcess: SagaProcessPort,
    private val taskRepository: TaskRepository,
) : TaskSagaUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun processTaskWithSaga(
        name: String,
        payload: String,
    ): Task {
        val sagaId =
            sagaProcess
                .startSaga(
                    sagaType = SagaTypes.TASK_PROCESSING,
                    payload = mapOf("name" to name),
                ).id

        return try {
            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PROCESS_TASK,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("name" to name),
            )

            val saved = taskRepository.save(Task(name = name, payload = payload))

            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PERSIST_TASK,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("taskId" to saved.id),
            )

            val processed = taskRepository.save(saved.markProcessed())

            sagaProcess.markSagaCompleted(sagaId)
            processed
        } catch (e: Exception) {
            logger.error("Task saga failed: ${e.message}", e)
            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PERSIST_TASK,
                status = SagaStepStatus.FAILED,
                payload = mapOf("error" to (e.message ?: "Unknown error")),
            )
            sagaProcess.markSagaFailed(sagaId, e.message ?: "Unknown error")
            throw e
        }
    }
}
