package com.vertyll.veds.task.infrastructure.saga

import com.vertyll.veds.shared.saga.engine.SagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensator
import com.vertyll.veds.task.application.saga.model.SagaStepNames
import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand
import com.vertyll.veds.task.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.SagaStepJpaEntity
import org.slf4j.LoggerFactory

internal class TaskSagaCompensator : SagaCompensator<SagaJpaEntity, SagaStepJpaEntity, TaskCompensationCommand> {
    private val logger = LoggerFactory.getLogger(TaskSagaCompensator::class.java)

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext<TaskCompensationCommand>,
    ) {
        val command =
            when (step.stepName) {
                SagaStepNames.PERSIST_TASK.value ->
                    TaskCompensationCommand.DeleteTask(readTaskId(context, step))
                SagaStepNames.PUBLISH_TASK_EVENT.value ->
                    TaskCompensationCommand.LogTaskCompensation(readTaskId(context, step))
                SagaStepNames.PROCESS_TASK.value -> {
                    logger.info(
                        "No compensation needed for step '{}' on saga '{}' (effect not externally observable)",
                        step.stepName,
                        saga.id,
                    )
                    return
                }
                else -> {
                    logger.warn("No compensation defined for step '{}' on saga '{}'", step.stepName, saga.id)
                    return
                }
            }

        context.publishCompensationEvent(
            sagaId = saga.id,
            stepId = step.id,
            command = command,
        )
    }

    private fun readTaskId(
        context: SagaCompensationContext<TaskCompensationCommand>,
        step: SagaStepJpaEntity,
    ): String {
        val payload = context.readStepPayload(step.payload)
        val raw =
            payload["taskId"]
                ?: error("Missing 'taskId' in step payload for step ${step.id} (keys: ${payload.keys})")
        return raw.toString()
    }
}
