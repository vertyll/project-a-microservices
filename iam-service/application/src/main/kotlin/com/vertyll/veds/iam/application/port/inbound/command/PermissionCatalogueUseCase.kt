package com.vertyll.veds.iam.application.port.inbound.command

import com.vertyll.veds.iam.application.command.RegisterPermissionCatalogueCommand

interface PermissionCatalogueUseCase {
    fun register(command: RegisterPermissionCatalogueCommand)
}
