package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.port.inbound.AuthCompensationUseCase
import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand
import com.vertyll.veds.shared.saga.engine.CompensationCommandHandler

internal class IamSagaCompensationHandler(
    private val authCompensationService: AuthCompensationUseCase,
) : CompensationCommandHandler<AuthCompensationCommand> {
    override fun handle(
        sagaId: String,
        command: AuthCompensationCommand,
    ) = authCompensationService.compensate(command)
}
