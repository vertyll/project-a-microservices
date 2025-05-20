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
import com.vertyll.projecta.auth.domain.model.TokenType
import com.vertyll.projecta.auth.domain.model.VerificationToken
import com.vertyll.projecta.auth.domain.repository.AuthUserRepository
import com.vertyll.projecta.auth.domain.repository.RefreshTokenRepository
import com.vertyll.projecta.auth.domain.repository.VerificationTokenRepository
import com.vertyll.projecta.auth.infrastructure.kafka.AuthEventProducer
import com.vertyll.projecta.common.event.mail.MailRequestedEvent
import com.vertyll.projecta.common.event.user.UserRegisteredEvent
import com.vertyll.projecta.common.event.user.UserProfileUpdatedEvent
import com.vertyll.projecta.common.exception.ApiException
import com.vertyll.projecta.common.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.common.kafka.KafkaTopics
import com.vertyll.projecta.common.saga.SagaManager
import com.vertyll.projecta.common.saga.SagaStepStatus
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    private val kafkaOutboxProcessor: KafkaOutboxProcessor
) {
    private val logger: Logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun register(request: RegisterRequestDto) {
        // Check if user already exists
        val existingUser = authUserRepository.findByEmail(request.email).orElse(null)
        
        if (existingUser != null) {
            // Check if account exists but not activated
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

        // Start a new registration saga
        val saga = sagaManager.startSaga(
            sagaType = "UserRegistration",
            payload = mapOf(
                "email" to request.email,
                "firstName" to request.firstName,
                "lastName" to request.lastName
            )
        )
        
        try {
            // Create auth user with encoded password
            val authUser = AuthUser.create(
                email = request.email,
                password = passwordEncoder.encode(request.password),
                enabled = false // Will be enabled after verification
            )
            
            // Save auth user
            val savedAuthUser = authUserRepository.save(authUser)
            logger.info("Created auth user with ID: {}", savedAuthUser.id)
            
            // Record successful auth user creation step
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = "CreateAuthUser",
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
                    topic = KafkaTopics.USER_REGISTERED,
                    key = event.eventId,
                    payload = event,
                    sagaId = saga.id
                )
                
                // Record successful event creation step
                sagaManager.recordSagaStep(
                    sagaId = saga.id,
                    stepName = "CreateUserEvent",
                    status = SagaStepStatus.COMPLETED,
                    payload = event
                )
                
                logger.info("Created user registration event for: {}", request.email)
            } catch (e: Exception) {
                logger.error("Failed to create user registration event: {}", e.message, e)
                // Mark step as failed and trigger saga compensation
                sagaManager.recordSagaStep(
                    sagaId = saga.id,
                    stepName = "CreateUserEvent",
                    status = SagaStepStatus.FAILED,
                    payload = mapOf("error" to e.message)
                )
                // Re-throw to roll back transaction
                throw e
            }

            // Create verification token
            val verificationToken = generateVerificationToken()
            
            // Save the verification token
            val savedToken = saveVerificationToken(
                request.email, 
                verificationToken, 
                TokenType.ACCOUNT_ACTIVATION.value
            )
            
            // Record successful token creation step
            sagaManager.recordSagaStep(
                sagaId = saga.id,
                stepName = "CreateVerificationToken",
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
                    templateName = "ACTIVATE_ACCOUNT",
                    variables = mapOf(
                        "username" to request.firstName,
                        "activation_code" to verificationToken
                    )
                )
                
                // Save the event to the outbox table
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopics.MAIL_REQUESTED,
                    key = mailEvent.eventId,
                    payload = mailEvent,
                    sagaId = saga.id
                )
                
                // Record successful mail event creation step
                sagaManager.recordSagaStep(
                    sagaId = saga.id,
                    stepName = "CreateMailEvent",
                    status = SagaStepStatus.COMPLETED,
                    payload = mailEvent
                )
                
                logger.info("Created activation email event for: {}", request.email)
            } catch (e: Exception) {
                logger.error("Failed to create activation email event: {}", e.message, e)
                // Since email is not critical, we'll just log the error but not fail the saga
                sagaManager.recordSagaStep(
                    sagaId = saga.id,
                    stepName = "CreateMailEvent",
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

        if (!verificationToken.isTokenType(TokenType.ACCOUNT_ACTIVATION)) {
            throw ApiException("Invalid token type", HttpStatus.BAD_REQUEST)
        }

        // Find and enable the user
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
                        templateName = "WELCOME_EMAIL",
                        variables =
                                mapOf("username" to verificationToken.username.substringBefore('@'))
                )
        )
    }

    @Transactional
    fun authenticate(request: AuthRequestDto, response: HttpServletResponse): AuthResponseDto {
        try {
            // Use credential verification service to verify credentials
            val credentialsResult = credentialVerificationService.verifyCredentials(request.email, request.password)

            if (!credentialsResult.valid) {
                // Check if this is an unactivated account error
                val errorMessage = credentialsResult.message
                if (errorMessage != null && errorMessage.contains("not been activated")) {
                    throw ApiException(errorMessage, HttpStatus.FORBIDDEN)
                }
                throw ApiException(errorMessage ?: "Invalid credentials", HttpStatus.UNAUTHORIZED)
            }

            // Get the user
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

            // Generate JWT token
            val jwtToken = jwtService.generateToken(user)

            // Process device info - use provided deviceInfo or extract from request if available
            val deviceInfo = request.deviceInfo ?: extractDeviceInfo(request.userAgent)

            // Create refresh token
            val refreshToken = createRefreshToken(user, deviceInfo)

            // Add refresh token to cookie
            addRefreshTokenCookie(response, refreshToken)

            return AuthResponseDto(token = jwtToken)
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            logger.error("Authentication failed: {}", e.message, e)
            throw ApiException("Authentication failed. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Transactional
    fun changePassword(email: String, request: ChangePasswordRequestDto) {
        // Verify current password
        val credentialsResult = credentialVerificationService.verifyCredentials(email, request.currentPassword)

        if (!credentialsResult.valid) {
            throw ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST)
        }

        // Generate change password token
        val token = generateVerificationToken()

        // First stage - tylko wysyłamy token do użytkownika bez przechowywania nowego hasła
        saveVerificationToken(email, token, TokenType.PASSWORD_CHANGE_REQUEST.value)

        // Send password change confirmation email
        authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                        to = email,
                        subject = "Password Change Confirmation",
                        templateName = "CHANGE_PASSWORD",
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

        if (!verificationToken.isTokenType(TokenType.PASSWORD_CHANGE_REQUEST)) {
            throw ApiException("Invalid token type", HttpStatus.BAD_REQUEST)
        }

        // Mark token as used
        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)

        // Wracamy informację o weryfikacji tokenu, drugi etap będzie wymagał podania nowego hasła
        // Zwracamy identyfikator tokenu, który będzie potrzebny do drugiego etapu
        verificationToken.id?.let { tokenId ->
            // Znajdź użytkownika, aby zweryfikować czy istnieje
            authUserRepository.findByEmail(verificationToken.username).orElseThrow {
                ApiException("User not found", HttpStatus.NOT_FOUND)
            }
            
            // Zapisz informację o zweryfikowanym tokenie do późniejszego użycia
            // możemy bezpiecznie zachować informację że token został zweryfikowany
            authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                        to = verificationToken.username,
                        subject = "Set Your New Password",
                        templateName = "WELCOME_EMAIL", // Reusing welcome template for this example
                        variables = mapOf(
                            "username" to verificationToken.username.substringBefore('@'),
                            "message" to "Your password change request has been verified. Please proceed to set your new password with the following code: $tokenId"
                        )
                )
            )
        } ?: throw ApiException("Invalid token", HttpStatus.BAD_REQUEST)
    }
    
    /**
     * Drugi etap zmiany hasła - użytkownik podaje nowe hasło i ID tokena weryfikacyjnego
     */
    @Transactional
    fun setNewPassword(tokenId: Long, newPassword: String) {
        // Znajdź token weryfikacyjny po ID
        val verificationToken = verificationTokenRepository.findById(tokenId).orElseThrow {
            ApiException("Invalid token", HttpStatus.BAD_REQUEST)
        }
        
        // Sprawdź czy token został użyty (musi być użyty w pierwszym etapie weryfikacji)
        if (!verificationToken.used) {
            throw ApiException("Token not verified", HttpStatus.BAD_REQUEST)
        }
        
        // Sprawdź czy token nie jest starszy niż 30 minut od momentu weryfikacji
        if (verificationToken.updatedAt.plusSeconds(1800).isBefore(Instant.now())) {
            throw ApiException("Token expired", HttpStatus.BAD_REQUEST)
        }
        
        // Weryfikacja typu tokena
        if (!verificationToken.isTokenType(TokenType.PASSWORD_CHANGE_REQUEST)) {
            throw ApiException("Invalid token type", HttpStatus.BAD_REQUEST)
        }
        
        // Znajdź użytkownika
        val user = authUserRepository.findByEmail(verificationToken.username).orElseThrow {
            ApiException("User not found", HttpStatus.NOT_FOUND)
        }
        
        // Zaktualizuj hasło
        user.setPassword(passwordEncoder.encode(newPassword))
        authUserRepository.save(user)
        
        // Wyślij potwierdzenie
        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = verificationToken.username,
                subject = "Password Changed Successfully",
                templateName = "WELCOME_EMAIL", // Reusing welcome template for this example
                variables = mapOf(
                    "username" to verificationToken.username.substringBefore('@'),
                    "message" to "Your password has been changed successfully."
                )
            )
        )
        
        // Wyślij powiadomienie do User service
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
        // Verify user exists
        if (!authUserRepository.existsByEmail(email)) {
            // Don't reveal user existence, just return silently
            return
        }
        
        // Generate reset token
        val resetToken = generateVerificationToken()

        // Save token
        saveVerificationToken(email, resetToken, TokenType.PASSWORD_RESET.value)

        // Send password reset email
        authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                        to = email,
                        subject = "Password Reset",
                        templateName = "RESET_PASSWORD",
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

        if (!verificationToken.isTokenType(TokenType.PASSWORD_RESET)) {
            throw ApiException("Invalid token type", HttpStatus.BAD_REQUEST)
        }

        // Find the user
        val user = authUserRepository.findByEmail(verificationToken.username).orElseThrow {
            ApiException("User not found", HttpStatus.NOT_FOUND)
        }
        
        // Update password - we don't need to store the password in the token 
        // for PASSWORD_RESET type as the new password comes directly from the request
        user.setPassword(passwordEncoder.encode(request.newPassword))
        authUserRepository.save(user)

        // Mark token as used
        verificationToken.used = true
        verificationTokenRepository.save(verificationToken)

        // Send confirmation email
        authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                        to = verificationToken.username,
                        subject = "Password Reset Successful",
                        templateName = "WELCOME_EMAIL", // Reusing welcome template for this example
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
        // Generate JWT-based refresh token instead of UUID
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
    
    /**
     * Extracts basic device information from User-Agent header
     * 
     * @param userAgent The User-Agent header from the request
     * @return A string containing basic device information
     */
    private fun extractDeviceInfo(userAgent: String?): String? {
        if (userAgent.isNullOrBlank()) {
            return "Unknown device"
        }
        
        // Extract basic information from User-Agent
        val deviceInfo = StringBuilder()
        
        // Check for common browsers
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
        
        // Check for operating system
        deviceInfo.append(" on ")
        when {
            userAgent.contains("Windows", ignoreCase = true) -> deviceInfo.append("Windows")
            userAgent.contains("Mac", ignoreCase = true) -> deviceInfo.append("Mac")
            userAgent.contains("Android", ignoreCase = true) -> deviceInfo.append("Android")
            userAgent.contains("iOS", ignoreCase = true) || userAgent.contains("iPhone", ignoreCase = true) 
                || userAgent.contains("iPad", ignoreCase = true) -> deviceInfo.append("iOS")
            userAgent.contains("Linux", ignoreCase = true) -> deviceInfo.append("Linux")
            else -> deviceInfo.append("Unknown OS")
        }
        
        // Add the login time
        deviceInfo.append(" at ${java.time.format.DateTimeFormatter.ISO_INSTANT.format(Instant.now())}")
        
        return deviceInfo.toString()
    }

    @Transactional
    fun refreshToken(request: HttpServletRequest, response: HttpServletResponse): AuthResponseDto {
        val refreshTokenString =
                extractRefreshTokenFromCookies(request)
                        ?: throw ApiException("Refresh token not found", HttpStatus.UNAUTHORIZED)

        // Verify that the refresh token is a valid JWT
        try {
            // First verify the token signature and expiration
            val username = jwtService.extractUsername(refreshTokenString)
            
            // Find the stored token
            val storedToken =
                    refreshTokenRepository.findByToken(refreshTokenString).orElseThrow {
                        ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED)
                    }
    
            if (storedToken.isRevoked || storedToken.expiryDate.isBefore(Instant.now())) {
                throw ApiException("Refresh token is revoked or expired", HttpStatus.UNAUTHORIZED)
            }
    
            // Verify username matches
            if (username != storedToken.username) {
                throw ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED)
            }
    
            // Get the user
            val user = authUserRepository.findByEmail(username).orElseThrow {
                ApiException("User not found", HttpStatus.NOT_FOUND)
            }
    
            // Generate new access token
            val accessToken = jwtService.generateToken(user)
    
            // Rotate refresh token for security
            storedToken.revoked = true
            refreshTokenRepository.save(storedToken)
    
            // Create new refresh token
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

        // Clear the refresh token cookie
        deleteRefreshTokenCookie(response)
    }
    
    /**
     * Gets all active sessions for a user by username
     * 
     * @param username The user's email address
     * @return A list of active sessions for the user
     */
    @Transactional(readOnly = true)
    fun getActiveSessions(username: String): List<SessionDto> {
        // Get all non-revoked refresh tokens for the user
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

    /**
     * Revokes a specific session by ID
     * 
     * @param sessionId The ID of the session (refresh token) to revoke
     * @param username The username of the currently authenticated user
     * @return true if session was successfully revoked, false otherwise
     */
    @Transactional
    fun revokeSession(sessionId: Long, username: String): Boolean {
        val refreshToken = refreshTokenRepository.findById(sessionId).orElse(null) ?: return false
        
        // Security check - only allow users to revoke their own sessions
        if (refreshToken.username != username) {
            logger.warn("Attempted to revoke session belonging to another user: {} tried to revoke session of {}", 
                username, refreshToken.username)
            return false
        }
        
        // Don't allow revoking the current session via this method
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
        val cookie = Cookie(jwtService.getRefreshTokenCookieName(), token)
        cookie.isHttpOnly = true
        cookie.secure = true
        cookie.path = "/"
        cookie.maxAge = (jwtService.getRefreshTokenExpirationTime() / 1000).toInt()
        response.addCookie(cookie)
    }

    private fun deleteRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = Cookie(jwtService.getRefreshTokenCookieName(), "")
        cookie.isHttpOnly = true
        cookie.secure = true
        cookie.path = "/"
        cookie.maxAge = 0
        response.addCookie(cookie)
    }

    private fun extractRefreshTokenFromCookies(request: HttpServletRequest): String? {
        val cookies = request.cookies ?: return null

        return cookies.firstOrNull { it.name == jwtService.getRefreshTokenCookieName() }?.value
    }

    @Transactional
    fun requestEmailChange(email: String, request: ChangeEmailRequestDto) {
        // Verify the current password
        val credentialsResult = credentialVerificationService.verifyCredentials(email, request.password)

        if (!credentialsResult.valid) {
            throw ApiException("Invalid password", HttpStatus.BAD_REQUEST)
        }

        // Generate change email token
        val token = generateVerificationToken()

        // Save token
        saveVerificationToken(email, token, TokenType.EMAIL_CHANGE.value, request.newEmail)

        // Send email change confirmation email
        authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                        to = email,
                        subject = "Email Change Confirmation",
                        templateName = "CHANGE_EMAIL",
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
                        templateName = "WELCOME_EMAIL", // Reusing welcome template for this example
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

        if (!verificationToken.isTokenType(TokenType.EMAIL_CHANGE)) {
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
                        
        // Find the user
        val user = authUserRepository.findByEmail(oldEmail).orElseThrow {
            ApiException("User not found", HttpStatus.NOT_FOUND)
        }
        
        // Update email
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
                        templateName = "WELCOME_EMAIL", // Reusing welcome template for this example
                        variables =
                                mapOf(
                                        "username" to oldEmail.substringBefore('@'),
                                        "message" to "Your email has been changed to ${newEmail}."
                                )
                )
        )

        authEventProducer.sendMailRequestedEvent(
                MailRequestedEvent(
                        to = newEmail,
                        subject = "Welcome to Your Updated Account",
                        templateName = "WELCOME_EMAIL", // Reusing welcome template for this example
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
        // Check if user exists
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
            .findByUsernameAndTokenType(email, TokenType.ACCOUNT_ACTIVATION.value)
            .orElse(null)
            
        // If token exists and hasn't expired, invalidate it
        if (existingToken != null && !existingToken.used) {
            if (existingToken.expiryDate.isAfter(LocalDateTime.now())) {
                logger.info("Invalidating existing activation token for: {}", email)
                existingToken.used = true
                verificationTokenRepository.save(existingToken)
            }
        }
        
        // Generate new verification token
        val verificationToken = generateVerificationToken()
        
        // Save new token
        saveVerificationToken(email, verificationToken, TokenType.ACCOUNT_ACTIVATION.value)
        
        // Send activation email
        authEventProducer.sendMailRequestedEvent(
            MailRequestedEvent(
                to = email,
                subject = "Account Activation",
                templateName = "ACTIVATE_ACCOUNT",
                variables = mapOf(
                    "username" to email.substringBefore('@'),
                    "activation_code" to verificationToken
                )
            )
        )
        
        logger.info("Resent activation email to: {}", email)
    }
}
