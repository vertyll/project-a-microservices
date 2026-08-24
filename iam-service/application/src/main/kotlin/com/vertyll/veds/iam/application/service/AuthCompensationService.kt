package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.port.inbound.AuthCompensationUseCase
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.application.port.outbound.UseCaseLogger
import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository

class AuthCompensationService(
    private val userRepository: UserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val identityProvider: IdentityProviderPort,
    private val logger: UseCaseLogger,
) : AuthCompensationUseCase {
    override fun compensate(command: AuthCompensationCommand) {
        when (command) {
            is AuthCompensationCommand.DeleteUser -> deleteUser(command)
            is AuthCompensationCommand.DeleteVerificationToken -> deleteVerificationToken(command)
            is AuthCompensationCommand.RevertPasswordUpdate -> revertPasswordUpdate(command)
            is AuthCompensationCommand.RevertEmailUpdate -> revertEmailUpdate(command)
        }
    }

    private fun deleteUser(command: AuthCompensationCommand.DeleteUser) {
        userRepository.findById(command.userId)?.let {
            logger.info("Compensating CreateUser step: deleting user with ID {}", command.userId)
            userRepository.deleteById(command.userId)
        } ?: logger.info(
            "Compensation no-op: user {} already absent (already-compensated or never persisted)",
            command.userId,
        )
    }

    private fun deleteVerificationToken(command: AuthCompensationCommand.DeleteVerificationToken) {
        verificationTokenRepository.findById(command.tokenId)?.let {
            logger.info(
                "Compensating CreateVerificationToken step: deleting token with ID {}",
                command.tokenId,
            )
            verificationTokenRepository.deleteById(command.tokenId)
        } ?: logger.info(
            "Compensation no-op: verification token {} already absent",
            command.tokenId,
        )
    }

    private fun revertPasswordUpdate(command: AuthCompensationCommand.RevertPasswordUpdate) {
        logger.warn(
            "Cannot revert password change for user {} — passwords are managed by Keycloak and the " +
                "previous credential is not retained. Manual intervention may be required.",
            command.userId,
        )
    }

    private fun revertEmailUpdate(command: AuthCompensationCommand.RevertEmailUpdate) {
        userRepository.findById(command.userId)?.let { user ->
            logger.info(
                "Compensating UpdateEmail step: reverting email for user ID {} to {}",
                command.userId,
                command.originalEmail,
            )
            user.keycloakId?.let { identityProvider.updateEmail(it, command.originalEmail) }
            userRepository.save(user.withEmail(command.originalEmail))
        } ?: logger.warn(
            "Cannot revert email for user {} — user no longer exists locally",
            command.userId,
        )
    }
}
