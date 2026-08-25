package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.command.ChangeEmailCommand
import com.vertyll.veds.iam.application.command.ChangePasswordCommand
import com.vertyll.veds.iam.application.command.RegisterCommand
import com.vertyll.veds.iam.application.command.ResetPasswordCommand
import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.port.inbound.command.AuthCommandUseCase
import com.vertyll.veds.iam.application.port.outbound.AuthEventPublisherPort
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.application.port.outbound.SagaProcessPort
import com.vertyll.veds.iam.application.port.outbound.UseCaseLogger
import com.vertyll.veds.iam.application.saga.model.SagaStepNames
import com.vertyll.veds.iam.application.saga.model.SagaTypes
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.EmailTemplate
import com.vertyll.veds.iam.domain.model.RoleType
import com.vertyll.veds.iam.domain.model.TokenTypes
import com.vertyll.veds.iam.domain.model.VerificationToken
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import java.time.LocalDateTime
import java.util.UUID
import com.vertyll.veds.iam.domain.model.User as DomainUser

class AuthCommandService(
    private val verificationTokenRepository: VerificationTokenRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val identityProvider: IdentityProviderPort,
    private val authEventPublisher: AuthEventPublisherPort,
    private val sagaProcessPort: SagaProcessPort,
    private val logger: UseCaseLogger,
) : AuthCommandUseCase {
    private companion object {
        private const val SUBJECT_ACTIVATE_ACCOUNT = "Activate your account"
        private const val SUBJECT_PASSWORD_RESET_REQUEST = "Password Reset Request"
        private const val SUBJECT_CONFIRM_EMAIL_CHANGE = "Confirm Email Change"
        private const val SUBJECT_CONFIRM_PASSWORD_CHANGE = "Confirm Password Change"
        private const val DEFAULT_VERIFICATION_TOKEN_EXPIRY_HOURS = 24L
    }

    override fun register(request: RegisterCommand) {
        if (userRepository.existsByEmail(request.email)) {
            logger.warn("Registration attempted with existing email: {}", request.email)
            throw ApiException(IamError.REGISTRATION_FAILED)
        }

        val saga =
            sagaProcessPort.startSaga(
                sagaType = SagaTypes.USER_REGISTRATION,
                payload =
                    mapOf(
                        "email" to request.email,
                        "firstName" to request.firstName,
                        "lastName" to request.lastName,
                    ),
            )

        try {
            logger.info("Creating new user with email: {}", request.email)

            val keycloakId =
                identityProvider.createUser(
                    email = request.email,
                    password = request.password,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    roleName = RoleType.USER.value,
                )

            val userRole =
                roleRepository.findByName(RoleType.USER.value)
                    ?: throw ApiException(IamError.DEFAULT_ROLE_NOT_CONFIGURED)

            val newUser =
                DomainUser
                    .create(
                        keycloakId = keycloakId,
                        email = request.email,
                        firstName = request.firstName,
                        lastName = request.lastName,
                    ).withRole(userRole)

            val savedUser = userRepository.save(newUser)

            authEventPublisher.publishUserRegistered(
                userId = keycloakId,
                email = savedUser.email,
                firstName = savedUser.firstName,
                lastName = savedUser.lastName,
            )

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_USER,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to savedUser.id!!,
                        "email" to savedUser.email,
                        "keycloakId" to keycloakId.toString(),
                    ),
            )

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.PUBLISH_USER_REGISTERED_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(savedUser.email, token, TokenTypes.ACCOUNT_ACTIVATION.value, sagaId = saga.id)

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                to = savedUser.email,
                subject = SUBJECT_ACTIVATE_ACCOUNT,
                templateName = EmailTemplate.ACTIVATE_ACCOUNT.name,
                variables =
                    mapOf(
                        "firstName" to savedUser.firstName,
                        "activationToken" to token,
                    ),
                sagaId = saga.id,
            )

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.PUBLISH_MAIL_REQUESTED_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("User registration failed for {}: {}", request.email, e.message, e)
            throw e
        }
    }

    override fun activateAccount(token: String) {
        val verificationToken =
            verificationTokenRepository.findByToken(token)
                ?: throw ApiException(IamError.INVALID_TOKEN)

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(IamError.USER_NOT_FOUND)

        user.keycloakId?.let { identityProvider.enableUser(it) }

        verificationTokenRepository.save(verificationToken.markUsed())
    }

    override fun resendActivationEmail(email: String) {
        val user = userRepository.findByEmail(email)
        if (user == null) {
            logger.info("Activation email resend requested for non-existent email: {}", email)
            return
        }

        val saga =
            sagaProcessPort.startSaga(
                sagaType = SagaTypes.EMAIL_VERIFICATION,
                payload =
                    mapOf(
                        "email" to email,
                        "userId" to user.id!!,
                    ),
            )

        try {
            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(user.email, token, TokenTypes.ACCOUNT_ACTIVATION.value, sagaId = saga.id)

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                to = user.email,
                subject = SUBJECT_ACTIVATE_ACCOUNT,
                templateName = EmailTemplate.ACTIVATE_ACCOUNT.name,
                variables =
                    mapOf(
                        "firstName" to user.firstName,
                        "activationToken" to token,
                    ),
                sagaId = saga.id,
            )

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.PUBLISH_MAIL_REQUESTED_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Resend activation email failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    override fun sendPasswordResetRequest(email: String) {
        val user = userRepository.findByEmail(email)
        if (user == null) {
            logger.info("Password reset requested for non-existent email: {}", email)
            return
        }

        val saga =
            sagaProcessPort.startSaga(
                sagaType = SagaTypes.PASSWORD_RESET,
                payload =
                    mapOf(
                        "email" to email,
                        "userId" to user.id!!,
                    ),
            )

        try {
            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(user.email, token, TokenTypes.PASSWORD_RESET.value, sagaId = saga.id)

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_RESET_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                to = user.email,
                subject = SUBJECT_PASSWORD_RESET_REQUEST,
                templateName = EmailTemplate.RESET_PASSWORD.name,
                variables =
                    mapOf(
                        "firstName" to user.firstName,
                        "resetToken" to token,
                    ),
                sagaId = saga.id,
            )

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.PUBLISH_MAIL_REQUESTED_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Password reset request failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    override fun resetPassword(
        token: String,
        request: ResetPasswordCommand,
    ) {
        val verificationToken =
            verificationTokenRepository.findByToken(token)
                ?: throw ApiException(IamError.INVALID_TOKEN)
        if (verificationToken.tokenType != TokenTypes.PASSWORD_RESET.value) {
            throw ApiException(IamError.INVALID_TOKEN)
        }

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(IamError.USER_NOT_FOUND)

        user.keycloakId?.let { identityProvider.resetPassword(it, request.newPassword) }

        verificationTokenRepository.save(verificationToken.markUsed())
    }

    override fun requestEmailChange(
        email: String,
        request: ChangeEmailCommand,
    ) {
        val user =
            userRepository.findByEmail(email)
                ?: throw ApiException(IamError.USER_NOT_FOUND)

        if (!identityProvider.validatePassword(email, request.password)) {
            throw ApiException(IamError.INVALID_CREDENTIALS)
        }

        if (userRepository.existsByEmail(request.newEmail)) {
            logger.warn("Email change requested to already existing email: {}", request.newEmail)
            throw ApiException(IamError.EMAIL_NOT_CHANGEABLE)
        }

        val saga =
            sagaProcessPort.startSaga(
                sagaType = SagaTypes.EMAIL_CHANGE,
                payload =
                    mapOf(
                        "email" to email,
                        "newEmail" to request.newEmail,
                        "userId" to user.id!!,
                    ),
            )

        try {
            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(user.email, token, TokenTypes.EMAIL_CHANGE.value, request.newEmail, saga.id)

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                        "sagaId" to saga.id,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                to = request.newEmail,
                subject = SUBJECT_CONFIRM_EMAIL_CHANGE,
                templateName = EmailTemplate.CHANGE_EMAIL.name,
                variables =
                    mapOf(
                        "firstName" to user.firstName,
                        "confirmationToken" to token,
                    ),
                sagaId = saga.id,
            )

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.PUBLISH_MAIL_REQUESTED_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Email change request failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    override fun confirmEmailChange(token: String) {
        val verificationToken =
            verificationTokenRepository.findByToken(token)
                ?: throw ApiException(IamError.INVALID_TOKEN)
        if (verificationToken.tokenType != TokenTypes.EMAIL_CHANGE.value) {
            throw ApiException(IamError.INVALID_TOKEN)
        }

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(IamError.USER_NOT_FOUND)

        val newEmail =
            verificationToken.additionalData
                ?: throw ApiException(IamError.MISSING_NEW_EMAIL_DATA)

        val originalEmail = user.email

        user.keycloakId?.let { identityProvider.updateEmail(it, newEmail) }
        userRepository.save(user.withEmail(newEmail))

        verificationTokenRepository.save(verificationToken.markUsed())

        verificationToken.sagaId?.let { sagaId ->
            sagaProcessPort.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.UPDATE_EMAIL,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to user.id!!,
                        "originalEmail" to originalEmail,
                        "newEmail" to newEmail,
                    ),
            )
            sagaProcessPort.markSagaCompleted(sagaId)
        }
    }

    override fun changePassword(
        email: String,
        request: ChangePasswordCommand,
    ) {
        val user =
            userRepository.findByEmail(email)
                ?: throw ApiException(IamError.USER_NOT_FOUND)

        val saga =
            sagaProcessPort.startSaga(
                sagaType = SagaTypes.PASSWORD_CHANGE,
                payload =
                    mapOf(
                        "email" to email,
                        "userId" to user.id!!,
                    ),
            )

        try {
            if (!identityProvider.validatePassword(email, request.currentPassword)) {
                throw ApiException(IamError.INVALID_CURRENT_PASSWORD)
            }

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.VERIFY_CURRENT_PASSWORD,
                status = SagaStepStatus.COMPLETED,
            )

            val token = generateRandomToken()
            val verificationToken = saveVerificationToken(user.email, token, TokenTypes.PASSWORD_CHANGE_REQUEST.value, null, saga.id)

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "tokenId" to verificationToken.id!!,
                        "token" to token,
                        "sagaId" to saga.id,
                    ),
            )

            authEventPublisher.sendMailRequestedEvent(
                to = user.email,
                subject = SUBJECT_CONFIRM_PASSWORD_CHANGE,
                templateName = EmailTemplate.SET_NEW_PASSWORD.name,
                variables =
                    mapOf(
                        "firstName" to user.firstName,
                        "confirmationToken" to token,
                    ),
                sagaId = saga.id,
            )

            sagaProcessPort.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.PUBLISH_MAIL_REQUESTED_EVENT,
                status = SagaStepStatus.COMPLETED,
            )

            sagaProcessPort.markAwaitingResponse(saga.id)
        } catch (e: Exception) {
            logger.error("Password change request failed for {}: {}", email, e.message, e)
            throw e
        }
    }

    override fun confirmPasswordChange(
        token: String,
        newPassword: String,
    ) {
        val verificationToken =
            verificationTokenRepository.findByToken(token)
                ?: throw ApiException(IamError.INVALID_TOKEN)
        if (verificationToken.tokenType != TokenTypes.PASSWORD_CHANGE_REQUEST.value) {
            throw ApiException(IamError.INVALID_TOKEN)
        }

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(IamError.USER_NOT_FOUND)

        user.keycloakId?.let { identityProvider.resetPassword(it, newPassword) }

        verificationTokenRepository.save(verificationToken.markUsed())

        verificationToken.sagaId?.let { sagaId ->
            sagaProcessPort.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.UPDATE_PASSWORD,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "userId" to user.id!!,
                    ),
            )
            sagaProcessPort.markSagaCompleted(sagaId)
        }
    }

    override fun setNewPassword(
        tokenId: Long,
        request: ResetPasswordCommand,
    ) {
        val verificationToken =
            verificationTokenRepository.findById(tokenId)
                ?: throw ApiException(IamError.INVALID_TOKEN_ID)

        if (verificationToken.token != request.confirmationCode) {
            throw ApiException(IamError.INVALID_CONFIRMATION_CODE)
        }

        validateVerificationToken(verificationToken)

        val user =
            userRepository.findByEmail(verificationToken.username)
                ?: throw ApiException(IamError.USER_NOT_FOUND)

        user.keycloakId?.let { identityProvider.resetPassword(it, request.newPassword) }

        verificationTokenRepository.save(verificationToken.markUsed())
    }

    private fun saveVerificationToken(
        email: String,
        token: String,
        tokenType: String,
        additionalData: String? = null,
        sagaId: String? = null,
    ): VerificationToken {
        val expiryDate = LocalDateTime.now().plusHours(DEFAULT_VERIFICATION_TOKEN_EXPIRY_HOURS)
        val verificationToken =
            VerificationToken(
                token = token,
                username = email,
                expiryDate = expiryDate,
                tokenType = tokenType,
                additionalData = additionalData,
                sagaId = sagaId,
            )
        return verificationTokenRepository.save(verificationToken)
    }

    private fun validateVerificationToken(verificationToken: VerificationToken) {
        if (verificationToken.used || verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(IamError.TOKEN_EXPIRED_OR_USED)
        }
    }

    private fun generateRandomToken(): String = UUID.randomUUID().toString()
}
