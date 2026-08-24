package com.vertyll.veds.iam.application.port.inbound.command

interface RoleCommandUseCase {
    fun assignRoleToUser(
        userId: Long,
        roleName: String,
        version: Long? = null,
    )

    fun removeRoleFromUser(
        userId: Long,
        roleName: String,
        version: Long? = null,
    )
}
