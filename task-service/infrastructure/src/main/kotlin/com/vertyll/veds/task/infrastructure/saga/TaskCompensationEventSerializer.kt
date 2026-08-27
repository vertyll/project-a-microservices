package com.vertyll.veds.task.infrastructure.saga

import com.vertyll.veds.shared.saga.engine.CompensationEventSerializer
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand
import com.vertyll.veds.task.saga.DeleteTaskAction
import com.vertyll.veds.task.saga.LogTaskCompensationAction
import com.vertyll.veds.task.saga.SagaCompensationEvent

internal class TaskCompensationEventSerializer(
    private val avroPayloadSerializer: AvroPayloadSerializer,
    private val topic: String,
) : CompensationEventSerializer<TaskCompensationCommand> {
    override fun serialize(
        sagaId: String,
        stepId: Long?,
        command: TaskCompensationCommand,
    ): ByteArray {
        val action: Any =
            when (command) {
                is TaskCompensationCommand.DeleteTask ->
                    DeleteTaskAction.newBuilder().setTaskId(command.taskId).build()
                is TaskCompensationCommand.LogTaskCompensation ->
                    LogTaskCompensationAction.newBuilder().setTaskId(command.taskId).build()
            }
        val record =
            SagaCompensationEvent
                .newBuilder()
                .setSagaId(sagaId)
                .setStepId(stepId)
                .setAction(action)
                .build()
        return avroPayloadSerializer.serialize(topic, record)
    }
}
