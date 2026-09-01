package com.vertyll.veds.iam.application.port.inbound.command

import com.vertyll.veds.iam.application.dto.AuthenticatedIdentity

interface ProvisionCurrentUserUseCase {
    fun provision(identity: AuthenticatedIdentity)
}
