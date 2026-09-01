package com.vertyll.veds.iam.application.port.inbound

import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand

@Suppress("kotlin:S6517")
fun interface AuthCompensationUseCase {
    fun compensate(command: AuthCompensationCommand)
}
