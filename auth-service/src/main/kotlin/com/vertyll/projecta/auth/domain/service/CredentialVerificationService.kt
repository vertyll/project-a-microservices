package com.vertyll.projecta.auth.domain.service

import com.vertyll.projecta.auth.domain.repository.AuthUserRepository
import com.vertyll.projecta.sharedinfrastructure.event.user.CredentialsVerificationResultEvent
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CredentialVerificationService(
    private val authUserRepository: AuthUserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val INVALID_EMAIL_OR_PASSWORD_MESSAGE = "Invalid email or password. Please try again."
        private const val YOUR_ACCOUNT_NOT_ACTIVATED_MESSAGE = "Your account has not been activated."
        private const val INVALID_EMAIL_OR_PASSWORD = "Invalid email or password."
        private const val CREDENTIALS_VERIFIED_SUCCESSFULLY = "Credentials verified successfully"
        private const val AUTHENTICATION_FAILED = "Authentication failed. Please try again later."
    }

    fun verifyCredentials(
        email: String,
        password: String,
    ): CredentialsVerificationResultEvent {
        logger.info("Verifying credentials for user: {}", email)
        val requestId = UUID.randomUUID().toString()

        return try {
            val userOptional = authUserRepository.findByEmail(email)

            if (userOptional.isEmpty) {
                logger.warn("Login attempt with non-existent email: {}", email)
                return CredentialsVerificationResultEvent(
                    requestId = requestId,
                    valid = false,
                    message = INVALID_EMAIL_OR_PASSWORD_MESSAGE,
                )
            }

            val user = userOptional.get()

            if (!user.isEnabled()) {
                logger.warn("Login attempt with unactivated account: {}", email)
                return CredentialsVerificationResultEvent(
                    requestId = requestId,
                    valid = false,
                    message = YOUR_ACCOUNT_NOT_ACTIVATED_MESSAGE,
                )
            }

            val isValid = passwordEncoder.matches(password, user.password)

            if (!isValid) {
                logger.warn("Login attempt with invalid password for user: {}", email)
                CredentialsVerificationResultEvent(
                    requestId = requestId,
                    valid = false,
                    message = INVALID_EMAIL_OR_PASSWORD,
                )
            } else {
                logger.info("Credentials verified successfully for user: {}", email)
                CredentialsVerificationResultEvent(
                    requestId = requestId,
                    valid = true,
                    userId = user.id,
                    email = user.username,
                    roles = user.getRoles().toList(),
                    message = CREDENTIALS_VERIFIED_SUCCESSFULLY,
                )
            }
        } catch (e: Exception) {
            logger.error("Error verifying credentials for user {}: {}", email, e.message, e)
            CredentialsVerificationResultEvent(
                requestId = requestId,
                valid = false,
                message = AUTHENTICATION_FAILED,
            )
        }
    }
}
