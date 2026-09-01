package com.vertyll.veds.iam.application.saga.model

sealed interface AuthCompensationCommand {
    data class DeleteUser(
        val userId: Long,
    ) : AuthCompensationCommand

    data class DeleteVerificationToken(
        val tokenId: Long,
    ) : AuthCompensationCommand

    data class RevertPasswordUpdate(
        val userId: Long,
    ) : AuthCompensationCommand

    data class RevertEmailUpdate(
        val userId: Long,
        val originalEmail: String,
    ) : AuthCompensationCommand
}
