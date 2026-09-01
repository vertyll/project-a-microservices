package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand
import com.vertyll.veds.iam.saga.DeleteUserAction
import com.vertyll.veds.iam.saga.DeleteVerificationTokenAction
import com.vertyll.veds.iam.saga.RevertEmailUpdateAction
import com.vertyll.veds.iam.saga.RevertPasswordUpdateAction
import com.vertyll.veds.iam.saga.SagaCompensationEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.saga.engine.CompensationEventSerializer

internal class IamCompensationEventSerializer(
    private val avroPayloadSerializer: AvroPayloadSerializer,
    private val topic: String,
) : CompensationEventSerializer<AuthCompensationCommand> {
    override fun serialize(
        sagaId: String,
        stepId: Long?,
        command: AuthCompensationCommand,
    ): ByteArray {
        val action: Any =
            when (command) {
                is AuthCompensationCommand.DeleteUser ->
                    DeleteUserAction.newBuilder().setUserId(command.userId).build()
                is AuthCompensationCommand.DeleteVerificationToken ->
                    DeleteVerificationTokenAction.newBuilder().setTokenId(command.tokenId).build()
                is AuthCompensationCommand.RevertPasswordUpdate ->
                    RevertPasswordUpdateAction.newBuilder().setUserId(command.userId).build()
                is AuthCompensationCommand.RevertEmailUpdate ->
                    RevertEmailUpdateAction
                        .newBuilder()
                        .setUserId(command.userId)
                        .setOriginalEmail(command.originalEmail)
                        .build()
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
