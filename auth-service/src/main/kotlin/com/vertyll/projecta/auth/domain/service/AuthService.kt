package com.vertyll.projecta.auth.domain.service

import com.vertyll.projecta.auth.domain.dto.AuthRequestDto
import com.vertyll.projecta.auth.domain.dto.AuthResponseDto
import com.vertyll.projecta.auth.domain.dto.ChangeEmailRequestDto
import com.vertyll.projecta.auth.domain.dto.ChangePasswordRequestDto
import com.vertyll.projecta.auth.domain.dto.RegisterRequestDto
import com.vertyll.projecta.auth.domain.dto.ResetPasswordRequestDto
import com.vertyll.projecta.auth.domain.dto.SessionDto
import com.vertyll.projecta.auth.domain.model.AuthUser
import com.vertyll.projecta.auth.domain.model.RefreshToken
import com.vertyll.projecta.auth.domain.model.VerificationToken
import com.vertyll.projecta.auth.domain.repository.AuthUserRepository
import com.vertyll.projecta.auth.domain.repository.RefreshTokenRepository
import com.vertyll.projecta.auth.domain.repository.VerificationTokenRepository
import com.vertyll.projecta.auth.infrastructure.kafka.AuthEventProducer
import com.vertyll.projecta.common.auth.TokenTypes
import com.vertyll.projecta.common.config.JwtConstants
import com.vertyll.projecta.common.event.mail.MailRequestedEvent
import com.vertyll.projecta.common.event.user.UserProfileUpdatedEvent
import com.vertyll.projecta.common.event.user.UserRegisteredEvent
import com.vertyll.projecta.common.exception.ApiException
import com.vertyll.projecta.common.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.common.kafka.KafkaTopicNames
import com.vertyll.projecta.common.kafka.KafkaTopicsConfig
import com.vertyll.projecta.common.mail.EmailTemplate
import com.vertyll.projecta.common.saga.SagaManager
import com.vertyll.projecta.common.saga.SagaStepNames
import com.vertyll.projecta.common.saga.SagaStepStatus
import com.vertyll.projecta.common.saga.SagaTypes
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val authUserRepository: AuthUserRepository,
    private val jwtService: JwtService,
    private val authEventProducer: AuthEventProducer,
    private val passwordEncoder: PasswordEncoder,
    private val credentialVerificationService: CredentialVerificationService,
    private val sagaManager: SagaManager,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val kafkaTopicsConfig: KafkaTopicsConfig
) {
    private val logger: Logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun register(request: RegisterRequestDto) {
        val existingUser = authUserRepository.findByEmail(request.email).orElse(null)

        if (existingUser != null) {
            if (!existingUser.isEnabled()) {
                logger.warn("Registration attempted for existing unactivated account: {}", request.email)
                throw ApiException(
                    "An account with this email already exists but hasn't been activated. " +
                        "Please check your email for the activation link or request a new one.",
                    HttpStatus.BAD_REQUEST
                )
            } else {
                logger.warn("Registration attempted with existing email: {}", request.email)
                throw ApiException(
                    "An account with this email already exists. Please use a different email or try logging in.",
                    HttpStatus.BAD_REQUEST
                )
            }
        }

        logger.info("Creating new user with email: {}", request.email)

        val saga = sagaManager.startSaga(
            sagaType = SagaTypes.USER_REGISTRATION.value,
            payload = mapOf(
                "email" to request.email,
                "firstName" to request.firstName,
                "lastName" to request.lastName
            )
        )

        try {
            val authUser = AuthUser.create(
                email = request.email,
                password = passwordEncoder.encode(request.password),
                enabled = false // Will be enabled after verification
            )

            val savedAuthUser = authUserRepository.save(authUser)
            logger.info("Created auth user with ID: {}", savedAuthUser.id)

            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_AUTH_USER.value,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf(
                    "authUserId" to savedAuthUser.id,
                    "email" to savedAuthUser.username
                )
            )

            // Send user registration event to create profile in User Service using the outbox pattern
            try {
                val event = UserRegisteredEvent(
                    userId = savedAuthUser.id ?: 0,
                    email = request.email,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    roles = savedAuthUser.getRoles()
                )

                // Save the event to the outbox table
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.USER_REGISTERED,
                    key = event.eventId,
                    payload = event,
                    sagaId = saga.id
                )

                // Record successful event creation step
                sagaManager.recordSagaStep(
                    sagaId = saga.id,
                    stepName = SagaStepNames.CREATE_USER_EVENT.value,
                    status = SagaStepStatus.COMPLETED,
                    payload = event
                )

                logger.info("Created user registration event for: {}", request.email)
            } catch (e: Exception) {
                logger.error("Failed to create user registration event: {}", e.message, e)
                // Mark step as failed and trigger saga compensation
                sagaManager.recordSagaStep(
                    sagaId = saga.id,
                    stepName = SagaStepNames.CREATE_USER_EVENT.value,
                    status = SagaStepStatus.FAILED,
                    payload = mapOf("error" to e.message)
                )
                // Re-throw to roll back transaction
                throw e
            }

            val verificationToken = generateVerificationToken()

            val savedToken = saveVerificationToken(
                request.email,
                verificationToken,
                TokenTypes.ACCOUNT_ACTIVATION.value
            )

            // Record successful token creation step
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = SagaStepNames.CREATE_VERIFICATION_TOKEN.value,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf(
                    "tokenId" to savedToken.id,
                    "token" to verificationToken
                )
            )

            // Send email verification request using the outbox pattern
            try {
                val mailEvent = MailRequestedEvent(
                    to = request.email,
                    subject = "Account Activation",
                    templateName = EmailTemplate.ACTIVATE_ACCOUNT.templateName,
                    variables = mapOf(
                        "username" to request.firstName,
                        "activation_code" to verificationToken
                    )
                )

                // Save the event to the outbox table
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.MAIL_REQUESTED,
                    key = mailEvent.eventId,
                    payload = mailEvent,
                    sagaId = saga.id
                )

                // Record successful mail event creation step
                sagaManager.recordSagaStep(
                    sagaId = saga.id,
                    stepName = SagaStepNames.CREATE_MAIL_EVENT.value,
                    status = SagaStepStatus.COMPLETED,
                    payload = mailEvent
                )

                logger.info("Created activation email event for: {}", request.email)
            } catch (e: Exception) {
                logger.error("Failed to create activation email event: {}", e.message, e)
                // Since email is not critical, we'll just log the error but not fail the saga
                sagaManager.recordSagaStep(
                    sagaId = saga.id,
                    stepName = SagaStepNames.CREATE_MAIL_EVENT.value,
                    status = SagaStepStatus.FAILED,
                    payload = mapOf("error" to e.message)
                )
                // Continue with registration - user can request another activation email
            }

            // Mark saga as completed
            sagaManager.completeSaga(saga.id)
        } catch (e: Exception) {
            // Mark the saga as failed and initiate compensation
            sagaManager.failSaga(saga.id, e.message ?: "Registration failed")

            // Re-throw the exception
            throw ApiException(
                "Registration failed: ${e.message}",
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @Transactional
    fun activateAccount(token: String) {
        val verificationToken =
            verificationTokenRepository.findByToken(token).orElseThrow {
                ApiException("Invalid activation token", HttpStatus.BAD_REQUEST)
            }

        if (verificationToken.used) {
            throw ApiException("Token already used", HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException("Token expired", HttpStatus.BAD_REQUEST)
        }

        if (!verificationToken.isTokenType(TokenTypes.ACCOUNT_ACTIVATION)) {
            throw ApiException("Invalid token type", HttpStatus.BAD_REQUEST)
        }

        val user = authUserRepository.findByEmail(verificationToken.username).orElseThrow {
            ApiException("User not found", HttpStatus.NOT_FOUND)
        }

        user.enabled = true
        authUserRepository.save(user)

        // Mark token as used
        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)

        // Send welcome email
        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = verificationToken.username,
                subject = "Welcome to Project A",
                templateName = EmailTemplate.WELCOME_EMAIL.templateName,
                variables =
                mapOf("username" to verificationToken.username.substringBefore('@'))
            )
        )
    }

    @Transactional
    fun authenticate(request: AuthRequestDto, response: HttpServletResponse): AuthResponseDto {
        try {
            val credentialsResult = credentialVerificationService.verifyCredentials(request.email, request.password)

            if (!credentialsResult.valid) {
                val errorMessage = credentialsResult.message
                if (errorMessage != null && errorMessage.contains("not been activated")) {
                    throw ApiException(errorMessage, HttpStatus.FORBIDDEN)
                }
                throw ApiException(errorMessage ?: "Invalid credentials", HttpStatus.UNAUTHORIZED)
            }

            val user = authUserRepository.findByEmail(request.email).orElseThrow {
                ApiException("User not found", HttpStatus.NOT_FOUND)
            }

            // Double-check that the account is enabled (defensive check)
            if (!user.isEnabled()) {
                logger.warn("Attempted login to inactive account: {}", user.username)
                throw ApiException(
                    "Your account has not been activated. Please check your email for the activation link or request a new one.",
                    HttpStatus.FORBIDDEN
                )
            }

            // Manually set the authenticated user
            val authentication =
                UsernamePasswordAuthenticationToken(user.username, null, user.authorities)
            SecurityContextHolder.getContext().authentication = authentication

            val jwtToken = jwtService.generateToken(user)

            val deviceInfo = request.deviceInfo ?: extractDeviceInfo(request.userAgent)

            val refreshToken = createRefreshToken(user, deviceInfo)

            addRefreshTokenCookie(response, refreshToken)

            return AuthResponseDto(
                token = jwtToken,
                type = JwtConstants.BEARER_PREFIX,
                expiresIn = jwtService.getAccessTokenExpirationTime()
            )
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            logger.error("Authentication failed: {}", e.message, e)
            throw ApiException("Authentication failed. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Transactional
    fun changePassword(email: String, request: ChangePasswordRequestDto) {
        val credentialsResult = credentialVerificationService.verifyCredentials(email, request.currentPassword)

        if (!credentialsResult.valid) {
            throw ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST)
        }

        val token = generateVerificationToken()

        // First stage - save the token
        saveVerificationToken(email, token, TokenTypes.PASSWORD_CHANGE_REQUEST.value)

        // Send password change confirmation email
        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = email,
                subject = "Password Change Confirmation",
                templateName = EmailTemplate.CHANGE_PASSWORD.templateName,
                variables =
                mapOf(
                    "username" to email.substringBefore('@'),
                    "activation_code" to token
                )
            )
        )
    }

    @Transactional
    fun confirmPasswordChange(token: String) {
        val verificationToken =
            verificationTokenRepository.findByToken(token).orElseThrow {
                ApiException("Invalid token", HttpStatus.BAD_REQUEST)
            }

        if (verificationToken.used) {
            throw ApiException("Token already used", HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException("Token expired", HttpStatus.BAD_REQUEST)
        }

        if (!verificationToken.isTokenType(TokenTypes.PASSWORD_CHANGE_REQUEST)) {
            throw ApiException("Invalid token type", HttpStatus.BAD_REQUEST)
        }

        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)

        verificationToken.id?.let { tokenId ->
            authUserRepository.findByEmail(verificationToken.username).orElseThrow {
                ApiException("User not found", HttpStatus.NOT_FOUND)
            }

            authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                    to = verificationToken.username,
                    subject = "Set Your New Password",
                    templateName = EmailTemplate.WELCOME_EMAIL.templateName,
                    variables = mapOf(
                        "username" to verificationToken.username.substringBefore('@'),
                        "message" to "Your password change request has been verified. Please proceed to set your new password with the following code: $tokenId"
                    )
                )
            )
        } ?: throw ApiException("Invalid token", HttpStatus.BAD_REQUEST)
    }

    @Transactional
    fun setNewPassword(tokenId: Long, newPassword: String) {
        val verificationToken = verificationTokenRepository.findById(tokenId).orElseThrow {
            ApiException("Invalid token", HttpStatus.BAD_REQUEST)
        }

        if (!verificationToken.used) {
            throw ApiException("Token not verified", HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.updatedAt.plusSeconds(1800).isBefore(Instant.now())) {
            throw ApiException("Token expired", HttpStatus.BAD_REQUEST)
        }

        if (!verificationToken.isTokenType(TokenTypes.PASSWORD_CHANGE_REQUEST)) {
            throw ApiException("Invalid token type", HttpStatus.BAD_REQUEST)
        }

        val user = authUserRepository.findByEmail(verificationToken.username).orElseThrow {
            ApiException("User not found", HttpStatus.NOT_FOUND)
        }

        user.setPassword(passwordEncoder.encode(newPassword))
        authUserRepository.save(user)

        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = verificationToken.username,
                subject = "Password Changed Successfully",
                templateName = EmailTemplate.WELCOME_EMAIL.templateName,
                variables = mapOf(
                    "username" to verificationToken.username.substringBefore('@'),
                    "message" to "Your password has been changed successfully."
                )
            )
        )

        try {
            authEventProducer.sendUserProfileUpdatedEvent(
                UserProfileUpdatedEvent(
                    userId = user.id,
                    email = user.username,
                    updatedFields = emptyMap(), // Don't send actual password data
                    updateType = UserProfileUpdatedEvent.UpdateType.PASSWORD
                )
            )
        } catch (e: Exception) {
            // Log but don't fail - auth is the source of truth for credentials
            logger.error("Failed to notify profile service of password change: ${e.message}", e)
        }
    }

    @Transactional
    fun sendPasswordResetRequest(email: String) {
        if (!authUserRepository.existsByEmail(email)) {
            return
        }

        val resetToken = generateVerificationToken()

        saveVerificationToken(email, resetToken, TokenTypes.PASSWORD_RESET.value)

        // Send password reset email
        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = email,
                subject = "Password Reset",
                templateName = EmailTemplate.RESET_PASSWORD.templateName,
                variables =
                mapOf(
                    "username" to email.substringBefore('@'),
                    "activation_code" to resetToken
                )
            )
        )
    }

    @Transactional
    fun resetPassword(token: String, request: ResetPasswordRequestDto) {
        val verificationToken =
            verificationTokenRepository.findByToken(token).orElseThrow {
                ApiException("Invalid token", HttpStatus.BAD_REQUEST)
            }

        if (verificationToken.used) {
            throw ApiException("Token already used", HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException("Token expired", HttpStatus.BAD_REQUEST)
        }

        if (!verificationToken.isTokenType(TokenTypes.PASSWORD_RESET)) {
            throw ApiException("Invalid token type", HttpStatus.BAD_REQUEST)
        }

        val user = authUserRepository.findByEmail(verificationToken.username).orElseThrow {
            ApiException("User not found", HttpStatus.NOT_FOUND)
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.newPassword))
        authUserRepository.save(user)

        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)

        // Send confirmation email
        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = verificationToken.username,
                subject = "Password Reset Successful",
                templateName = EmailTemplate.WELCOME_EMAIL.templateName,
                variables =
                mapOf(
                    "username" to
                        verificationToken.username.substringBefore('@'),
                    "message" to "Your password has been reset successfully."
                )
            )
        )
    }

    private fun createRefreshToken(userDetails: UserDetails, deviceInfo: String?): String {
        val jwtRefreshToken = jwtService.generateRefreshToken(userDetails)

        val refreshToken =
            RefreshToken(
                token = jwtRefreshToken,
                username = userDetails.username,
                expiryDate =
                Instant.now()
                    .plusMillis(jwtService.getRefreshTokenExpirationTime()),
                revoked = false,
                deviceInfo = deviceInfo
            )

        refreshTokenRepository.save(refreshToken)
        return jwtRefreshToken
    }

    private fun generateVerificationToken(): String {
        return UUID.randomUUID().toString()
    }

    private fun saveVerificationToken(
        email: String,
        token: String,
        tokenType: String,
        additionalData: String? = null
    ): VerificationToken {
        val verificationToken =
            VerificationToken(
                token = token,
                username = email,
                expiryDate = LocalDateTime.now().plusHours(24),
                used = false,
                tokenType = tokenType,
                additionalData = additionalData
            )

        return verificationTokenRepository.save(verificationToken)
    }

    private fun extractDeviceInfo(userAgent: String?): String? {
        if (userAgent.isNullOrBlank()) {
            return "Unknown device"
        }

        val deviceInfo = StringBuilder()

        when {
            userAgent.contains("Firefox", ignoreCase = true) -> deviceInfo.append("Firefox")
            userAgent.contains("Chrome", ignoreCase = true) && !userAgent.contains("Edg", ignoreCase = true) ->
                deviceInfo.append("Chrome")

            userAgent.contains("Safari", ignoreCase = true) && !userAgent.contains("Chrome", ignoreCase = true) ->
                deviceInfo.append("Safari")

            userAgent.contains("Edg", ignoreCase = true) -> deviceInfo.append("Edge")
            userAgent.contains("MSIE", ignoreCase = true) || userAgent.contains("Trident", ignoreCase = true) ->
                deviceInfo.append("Internet Explorer")

            userAgent.contains("Opera", ignoreCase = true) || userAgent.contains("OPR", ignoreCase = true) ->
                deviceInfo.append("Opera")

            else -> deviceInfo.append("Unknown browser")
        }

        deviceInfo.append(" on ")
        when {
            userAgent.contains("Windows", ignoreCase = true) -> deviceInfo.append("Windows")
            userAgent.contains("Mac", ignoreCase = true) -> deviceInfo.append("Mac")
            userAgent.contains("Android", ignoreCase = true) -> deviceInfo.append("Android")
            userAgent.contains("iOS", ignoreCase = true) || userAgent.contains("iPhone", ignoreCase = true) ||
                userAgent.contains("iPad", ignoreCase = true) -> deviceInfo.append("iOS")

            userAgent.contains("Linux", ignoreCase = true) -> deviceInfo.append("Linux")
            else -> deviceInfo.append("Unknown OS")
        }

        deviceInfo.append(" at ${java.time.format.DateTimeFormatter.ISO_INSTANT.format(Instant.now())}")

        return deviceInfo.toString()
    }

    @Transactional
    fun refreshToken(request: HttpServletRequest, response: HttpServletResponse): AuthResponseDto {
        val refreshTokenString =
            extractRefreshTokenFromCookies(request)
                ?: throw ApiException("Refresh token not found", HttpStatus.UNAUTHORIZED)

        try {
            val username = jwtService.extractUsername(refreshTokenString)

            val storedToken =
                refreshTokenRepository.findByToken(refreshTokenString).orElseThrow {
                    ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED)
                }

            if (storedToken.isRevoked || storedToken.expiryDate.isBefore(Instant.now())) {
                throw ApiException("Refresh token is revoked or expired", HttpStatus.UNAUTHORIZED)
            }

            if (username != storedToken.username) {
                throw ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED)
            }

            val user = authUserRepository.findByEmail(username).orElseThrow {
                ApiException("User not found", HttpStatus.NOT_FOUND)
            }

            val accessToken = jwtService.generateToken(user)

            // Rotate refresh token for security
            storedToken.revoked = true
            refreshTokenRepository.save(storedToken)

            val newRefreshToken = createRefreshToken(user, storedToken.deviceInfo)
            addRefreshTokenCookie(response, newRefreshToken)

            return AuthResponseDto(
                token = accessToken,
                type = "Bearer",
                expiresIn = jwtService.getRefreshTokenExpirationTime()
            )
        } catch (e: io.jsonwebtoken.JwtException) {
            logger.error("Invalid JWT refresh token: {}", e.message)
            throw ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED)
        }
    }

    @Transactional
    fun logout(request: HttpServletRequest, response: HttpServletResponse) {
        val refreshToken = extractRefreshTokenFromCookies(request)

        if (refreshToken != null) {
            val token = refreshTokenRepository.findByToken(refreshToken)
            token.ifPresent {
                it.revoked = true
                refreshTokenRepository.save(it)
            }
        }

        deleteRefreshTokenCookie(response)
    }

    @Transactional(readOnly = true)
    fun getActiveSessions(username: String): List<SessionDto> {
        val refreshTokens = refreshTokenRepository.findByUsername(username)
            .filter { !it.isRevoked && it.expiryDate.isAfter(Instant.now()) }

        // Get the current refresh token if it exists in the request context
        val currentToken = SecurityContextHolder.getContext()?.authentication?.credentials as? String

        return refreshTokens.map { token ->
            SessionDto(
                id = token.id,
                deviceInfo = token.deviceInfo,
                createdAt = token.createdAt,
                expiryDate = token.expiryDate,
                isCurrentSession = currentToken != null && token.token == currentToken
            )
        }
    }

    @Transactional
    fun revokeSession(sessionId: Long, username: String): Boolean {
        val refreshToken = refreshTokenRepository.findById(sessionId).orElse(null) ?: return false

        if (refreshToken.username != username) {
            logger.warn(
                "Attempted to revoke session belonging to another user: {} tried to revoke session of {}",
                username,
                refreshToken.username
            )
            return false
        }

        val currentToken = SecurityContextHolder.getContext()?.authentication?.credentials as? String
        if (currentToken != null && refreshToken.token == currentToken) {
            logger.warn("Attempted to revoke current session via revokeSession method: {}", username)
            return false
        }

        refreshToken.revoked = true
        refreshTokenRepository.save(refreshToken)
        logger.info("Revoked session {} for user {}", sessionId, username)
        return true
    }

    private fun addRefreshTokenCookie(response: HttpServletResponse, token: String) {
        val cookie = Cookie(jwtService.getRefreshTokenCookieNameFromConfig(), token)
        cookie.isHttpOnly = true
        cookie.secure = true
        cookie.path = "/"
        cookie.maxAge = (jwtService.getRefreshTokenExpirationTime() / 1000).toInt()
        response.addCookie(cookie)
    }

    private fun deleteRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = Cookie(jwtService.getRefreshTokenCookieNameFromConfig(), "")
        cookie.isHttpOnly = true
        cookie.secure = true
        cookie.path = "/"
        cookie.maxAge = 0
        response.addCookie(cookie)
    }

    private fun extractRefreshTokenFromCookies(request: HttpServletRequest): String? {
        val cookies = request.cookies ?: return null

        return cookies.firstOrNull { it.name == jwtService.getRefreshTokenCookieNameFromConfig() }?.value
    }

    @Transactional
    fun requestEmailChange(email: String, request: ChangeEmailRequestDto) {
        val credentialsResult = credentialVerificationService.verifyCredentials(email, request.password)

        if (!credentialsResult.valid) {
            throw ApiException("Invalid password", HttpStatus.BAD_REQUEST)
        }

        val token = generateVerificationToken()

        saveVerificationToken(email, token, TokenTypes.EMAIL_CHANGE.value, request.newEmail)

        // Send email change confirmation email
        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = email,
                subject = "Email Change Confirmation",
                templateName = EmailTemplate.CHANGE_EMAIL.templateName,
                variables =
                mapOf(
                    "username" to email.substringBefore('@'),
                    "current_email" to email,
                    "new_email" to request.newEmail,
                    "activation_code" to token
                )
            )
        )

        // Send notification to new email as well
        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = request.newEmail,
                subject = "Email Change Request Verification",
                templateName = EmailTemplate.WELCOME_EMAIL.templateName,
                variables =
                mapOf(
                    "username" to request.newEmail.substringBefore('@'),
                    "message" to
                        "Someone has requested to change their email to this address. If this was you, please confirm by clicking the link sent to your current email address."
                )
            )
        )
    }

    @Transactional
    fun confirmEmailChange(token: String) {
        val verificationToken =
            verificationTokenRepository.findByToken(token).orElseThrow {
                ApiException("Invalid token", HttpStatus.BAD_REQUEST)
            }

        if (verificationToken.used) {
            throw ApiException("Token already used", HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException("Token expired", HttpStatus.BAD_REQUEST)
        }

        if (!verificationToken.isTokenType(TokenTypes.EMAIL_CHANGE)) {
            throw ApiException("Invalid token type", HttpStatus.BAD_REQUEST)
        }

        // Mark token as used
        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)

        val oldEmail = verificationToken.username
        val newEmail =
            verificationToken.additionalData
                ?: throw ApiException(
                    "Missing new email data",
                    HttpStatus.INTERNAL_SERVER_ERROR
                )

        val user = authUserRepository.findByEmail(oldEmail).orElseThrow {
            ApiException("User not found", HttpStatus.NOT_FOUND)
        }

        user.setEmail(newEmail)
        authUserRepository.save(user)

        // Send event to user service to update profile email
        try {
            authEventProducer.sendUserProfileUpdatedEvent(
                UserProfileUpdatedEvent(
                    userId = user.id,
                    email = newEmail,
                    updatedFields = mapOf("email" to newEmail),
                    updateType = UserProfileUpdatedEvent.UpdateType.EMAIL
                )
            )
        } catch (e: Exception) {
            // Log but don't fail - auth is the source of truth for credentials
            logger.error("Failed to notify profile service of email change: ${e.message}", e)
        }

        // Send confirmation email to both addresses
        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = oldEmail,
                subject = "Email Change Completed",
                templateName = EmailTemplate.WELCOME_EMAIL.templateName,
                variables =
                mapOf(
                    "username" to oldEmail.substringBefore('@'),
                    "message" to "Your email has been changed to $newEmail."
                )
            )
        )

        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = newEmail,
                subject = "Welcome to Your Updated Account",
                templateName = EmailTemplate.WELCOME_EMAIL.templateName,
                variables =
                mapOf(
                    "username" to newEmail.substringBefore('@'),
                    "message" to
                        "Your account email has been updated successfully."
                )
            )
        )
    }

    @Transactional
    fun resendActivationEmail(email: String) {
        val user = authUserRepository.findByEmail(email).orElse(null) ?: run {
            // Don't reveal user existence, just return silently
            logger.info("Activation email requested for non-existent user: {}", email)
            return
        }

        // If user is already activated, no need to send email
        if (user.isEnabled()) {
            logger.info("Activation email requested for already activated account: {}", email)
            throw ApiException("Account is already activated", HttpStatus.BAD_REQUEST)
        }

        // Check if there's an existing unused activation token
        val existingToken = verificationTokenRepository
            .findByUsernameAndTokenType(email, TokenTypes.ACCOUNT_ACTIVATION.value)
            .orElse(null)

        // If token exists and hasn't expired, invalidate it
        if (existingToken != null && !existingToken.used) {
            if (existingToken.expiryDate.isAfter(LocalDateTime.now())) {
                logger.info("Invalidating existing activation token for: {}", email)
                existingToken.used = true
                verificationTokenRepository.save(existingToken)
            }
        }

        val verificationToken = generateVerificationToken()

        saveVerificationToken(email, verificationToken, TokenTypes.ACCOUNT_ACTIVATION.value)

        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = email,
                subject = "Account Activation",
                templateName = EmailTemplate.ACTIVATE_ACCOUNT.templateName,
                variables = mapOf(
                    "username" to email.substringBefore('@'),
                    "activation_code" to verificationToken
                )
            )
        )

        logger.info("Resent activation email to: {}", email)
    }
}
