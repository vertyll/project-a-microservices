package com.vertyll.veds.task.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.DecodedCompensationEvent
import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand
import com.vertyll.veds.task.saga.DeleteTaskAction
import com.vertyll.veds.task.saga.LogTaskCompensationAction
import com.vertyll.veds.task.saga.SagaCompensationEvent

internal class AvroTaskCompensationCommandTranslator(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val topic: String,
) : CompensationCommandDeserializer<TaskCompensationCommand> {
    override fun deserialize(payload: ByteArray): DecodedCompensationEvent<TaskCompensationCommand> {
        val record = avroPayloadDeserializer.deserialize(topic, payload) as SagaCompensationEvent
        val command =
            when (val action = record.action) {
                is DeleteTaskAction ->
                    TaskCompensationCommand.DeleteTask(taskId = action.taskId.toString())
                is LogTaskCompensationAction ->
                    TaskCompensationCommand.LogTaskCompensation(taskId = action.taskId.toString())
                else ->
                    error(
                        "Unknown compensation action type on saga-compensation-task: ${action?.javaClass?.name}",
                    )
            }
        return DecodedCompensationEvent(
            sagaId = record.sagaId.toString(),
            stepId = record.stepId,
            command = command,
        )
    }
}
