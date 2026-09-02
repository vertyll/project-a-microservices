package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.FakeIdentityProvider
import com.vertyll.veds.iam.application.InMemoryRoleRepository
import com.vertyll.veds.iam.application.InMemoryUserRepository
import com.vertyll.veds.iam.application.InMemoryVerificationTokenRepository
import com.vertyll.veds.iam.application.RecordingAuthEventPublisher
import com.vertyll.veds.iam.application.RecordingSagaProcess
import com.vertyll.veds.iam.application.SilentLogger
import com.vertyll.veds.iam.application.command.ChangeEmailCommand
import com.vertyll.veds.iam.application.command.ChangePasswordCommand
import com.vertyll.veds.iam.application.command.RegisterCommand
import com.vertyll.veds.iam.application.command.ResetPasswordCommand
import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.role
import com.vertyll.veds.iam.application.user
import com.vertyll.veds.iam.application.verificationToken
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.TokenTypes
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AuthCommandServiceTest {
    private val tokens = InMemoryVerificationTokenRepository()
    private val users = InMemoryUserRepository()
    private val roles = InMemoryRoleRepository()
    private val identity = FakeIdentityProvider()
    private val events = RecordingAuthEventPublisher()
    private val saga = RecordingSagaProcess()

    private val service =
        AuthCommandService(
            verificationTokenRepository = tokens,
            userRepository = users,
            roleRepository = roles,
            identityProvider = identity,
            authEventPublisher = events,
            sagaProcessPort = saga,
            logger = SilentLogger,
        )

    private val userRole = role(name = "USER").also { roles.given(it) }

    private fun registerCommand(email: String = "ada@example.com") =
        RegisterCommand(firstName = "Ada", lastName = "Lovelace", email = email, password = "correct horse")

    private fun savedToken() = tokens.stored.values.single()

    // ── Registration ────────────────────────────────────────────────────

    @Test
    fun `registering creates the identity and the local user together`() {
        service.register(registerCommand())

        val stored = users.findByEmail("ada@example.com")
        assertNotNull(stored)
        assertNotNull(stored.keycloakId)
        assertTrue(identity.calls.contains("createUser(ada@example.com,USER)"))
    }

    @Test
    fun `a new user is given the default role`() {
        service.register(registerCommand())

        assertEquals(setOf(userRole), users.findByEmail("ada@example.com")!!.roles)
    }

    @Test
    fun `registering issues an activation token and asks for the mail`() {
        service.register(registerCommand())

        assertEquals(TokenTypes.ACCOUNT_ACTIVATION.value, savedToken().tokenType)
        assertEquals(
            listOf("UserRegistered(ada@example.com)", "MailRequested(ada@example.com,ACTIVATE_ACCOUNT)"),
            events.published,
        )
    }

    @Test
    fun `the registration saga waits for the mail service`() {
        service.register(registerCommand())

        assertEquals("awaiting", saga.trail.last())
        assertEquals("start(UserRegistration)", saga.trail.first())
    }

    @Test
    fun `registering an address that already exists is refused without naming why`() {
        users.given(user(email = "ada@example.com"))

        val error = assertFailsWith<ApiException> { service.register(registerCommand()) }

        assertEquals(IamError.REGISTRATION_FAILED, error.error)
        assertTrue(saga.trail.isEmpty(), "no saga should open for a request rejected up front")
    }

    @Test
    fun `a missing default role stops registration`() {
        roles.stored.clear()

        assertFailsWith<ApiException> { service.register(registerCommand()) }

        assertTrue(users.stored.isEmpty())
    }

    // ── Activation ──────────────────────────────────────────────────────

    @Test
    fun `activating enables the identity and spends the token`() {
        val existing = user().also { users.given(it) }
        tokens.given(verificationToken(token = "t", username = existing.email, tokenType = TokenTypes.ACCOUNT_ACTIVATION.value))

        service.activateAccount("t")

        assertTrue(identity.calls.contains("enableUser(${existing.keycloakId})"))
        assertTrue(tokens.findByToken("t")!!.used)
    }

    @Test
    fun `a token that was already spent is refused`() {
        val existing = user().also { users.given(it) }
        tokens.given(
            verificationToken(token = "t", username = existing.email, tokenType = TokenTypes.ACCOUNT_ACTIVATION.value, used = true),
        )

        val error = assertFailsWith<ApiException> { service.activateAccount("t") }

        assertEquals(IamError.TOKEN_EXPIRED_OR_USED, error.error)
        assertTrue(identity.calls.isEmpty())
    }

    @Test
    fun `an expired token is refused`() {
        val existing = user().also { users.given(it) }
        tokens.given(
            verificationToken(
                token = "t",
                username = existing.email,
                tokenType = TokenTypes.ACCOUNT_ACTIVATION.value,
                expiryDate = Instant.now().minus(1, ChronoUnit.HOURS),
            ),
        )

        val error = assertFailsWith<ApiException> { service.activateAccount("t") }

        assertEquals(IamError.TOKEN_EXPIRED_OR_USED, error.error)
    }

    @Test
    fun `an unknown token is refused`() {
        val error = assertFailsWith<ApiException> { service.activateAccount("nonsense") }

        assertEquals(IamError.INVALID_TOKEN, error.error)
    }

    // ── Password reset ──────────────────────────────────────────────────

    @Test
    fun `a reset request for an unknown address succeeds silently`() {
        service.sendPasswordResetRequest("nobody@example.com")

        assertTrue(events.published.isEmpty())
        assertTrue(tokens.stored.isEmpty())
    }

    @Test
    fun `a reset request issues a reset token and asks for the mail`() {
        users.given(user())

        service.sendPasswordResetRequest("ada@example.com")

        assertEquals(TokenTypes.PASSWORD_RESET.value, savedToken().tokenType)
        assertEquals(listOf("MailRequested(ada@example.com,RESET_PASSWORD)"), events.published)
    }

    @Test
    fun `resetting a password with a valid token reaches the identity provider`() {
        val existing = user().also { users.given(it) }
        tokens.given(verificationToken(token = "t", username = existing.email, tokenType = TokenTypes.PASSWORD_RESET.value))

        service.resetPassword("t", ResetPasswordCommand(newPassword = "new one", confirmationCode = "t"))

        assertTrue(identity.calls.contains("resetPassword(${existing.keycloakId})"))
        assertTrue(tokens.findByToken("t")!!.used)
    }

    @Test
    fun `an activation token cannot be used to reset a password`() {
        val existing = user().also { users.given(it) }
        tokens.given(verificationToken(token = "t", username = existing.email, tokenType = TokenTypes.ACCOUNT_ACTIVATION.value))

        val error =
            assertFailsWith<ApiException> { service.resetPassword("t", ResetPasswordCommand("new one", "t")) }

        assertEquals(IamError.INVALID_TOKEN, error.error)
        assertTrue(identity.calls.isEmpty())
    }

    // ── Changing an email address ───────────────────────────────────────

    private fun givenAuthenticatedUser(password: String = "correct horse") =
        user().also {
            users.given(it)
            identity.validPasswords[it.email] = password
        }

    @Test
    fun `changing an email requires the current password`() {
        val existing = givenAuthenticatedUser()

        val error =
            assertFailsWith<ApiException> {
                service.requestEmailChange(existing.email, ChangeEmailCommand(password = "wrong", newEmail = "new@example.com"))
            }

        assertEquals(IamError.INVALID_CREDENTIALS, error.error)
        assertTrue(tokens.stored.isEmpty())
    }

    @Test
    fun `an email change is confirmed by mail to the new address`() {
        val existing = givenAuthenticatedUser()

        service.requestEmailChange(existing.email, ChangeEmailCommand("correct horse", "new@example.com"))

        assertEquals(listOf("MailRequested(new@example.com,CHANGE_EMAIL)"), events.published)
        assertEquals("new@example.com", savedToken().additionalData, "the new address travels with the token")
    }

    @Test
    fun `an email already in use cannot be moved to`() {
        val existing = givenAuthenticatedUser()
        users.given(user(id = 2L, email = "taken@example.com"))

        val error =
            assertFailsWith<ApiException> {
                service.requestEmailChange(existing.email, ChangeEmailCommand("correct horse", "taken@example.com"))
            }

        assertEquals(IamError.EMAIL_NOT_CHANGEABLE, error.error)
    }

    @Test
    fun `confirming an email change updates both the identity and the local record`() {
        val existing = user().also { users.given(it) }
        tokens.given(
            verificationToken(
                token = "t",
                username = existing.email,
                tokenType = TokenTypes.EMAIL_CHANGE.value,
                additionalData = "new@example.com",
                sagaId = "saga-1",
            ),
        )

        service.confirmEmailChange("t")

        assertEquals("new@example.com", users.findById(existing.id!!)!!.email)
        assertTrue(identity.calls.contains("updateEmail(${existing.keycloakId},new@example.com)"))
    }

    @Test
    fun `confirming an email change closes the saga that requested it`() {
        val existing = user().also { users.given(it) }
        tokens.given(
            verificationToken(
                token = "t",
                username = existing.email,
                tokenType = TokenTypes.EMAIL_CHANGE.value,
                additionalData = "new@example.com",
                sagaId = "saga-1",
            ),
        )

        service.confirmEmailChange("t")

        assertEquals("completed", saga.trail.last())
    }

    @Test
    fun `an email-change token with no new address is rejected`() {
        val existing = user().also { users.given(it) }
        tokens.given(
            verificationToken(token = "t", username = existing.email, tokenType = TokenTypes.EMAIL_CHANGE.value, additionalData = null),
        )

        val error = assertFailsWith<ApiException> { service.confirmEmailChange("t") }

        assertEquals(IamError.MISSING_NEW_EMAIL_DATA, error.error)
        assertEquals(existing.email, users.findById(existing.id!!)!!.email)
    }

    @Test
    fun `a password-reset token cannot confirm an email change`() {
        val existing = user().also { users.given(it) }
        tokens.given(verificationToken(token = "t", username = existing.email, tokenType = TokenTypes.PASSWORD_RESET.value))

        val error = assertFailsWith<ApiException> { service.confirmEmailChange("t") }

        assertEquals(IamError.INVALID_TOKEN, error.error)
    }

    // ── Changing a password ─────────────────────────────────────────────

    @Test
    fun `changing a password requires the current one`() {
        val existing = givenAuthenticatedUser()

        val error =
            assertFailsWith<ApiException> {
                service.changePassword(existing.email, ChangePasswordCommand(currentPassword = "wrong"))
            }

        assertEquals(IamError.INVALID_CURRENT_PASSWORD, error.error)
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `a password change is confirmed by mail before it takes effect`() {
        val existing = givenAuthenticatedUser()

        service.changePassword(existing.email, ChangePasswordCommand("correct horse"))

        assertEquals(TokenTypes.PASSWORD_CHANGE_REQUEST.value, savedToken().tokenType)
        assertEquals(listOf("MailRequested(ada@example.com,SET_NEW_PASSWORD)"), events.published)
        assertTrue(identity.calls.none { it.startsWith("resetPassword") })
    }

    @Test
    fun `confirming a password change applies it and closes the saga`() {
        val existing = user().also { users.given(it) }
        tokens.given(
            verificationToken(
                token = "t",
                username = existing.email,
                tokenType = TokenTypes.PASSWORD_CHANGE_REQUEST.value,
                sagaId = "saga-1",
            ),
        )

        service.confirmPasswordChange("t", "new one")

        assertTrue(identity.calls.contains("resetPassword(${existing.keycloakId})"))
        assertTrue(tokens.findByToken("t")!!.used)
        assertEquals("completed", saga.trail.last())
    }

    @Test
    fun `a reset token cannot confirm a password change`() {
        val existing = user().also { users.given(it) }
        tokens.given(verificationToken(token = "t", username = existing.email, tokenType = TokenTypes.PASSWORD_RESET.value))

        val error = assertFailsWith<ApiException> { service.confirmPasswordChange("t", "new one") }

        assertEquals(IamError.INVALID_TOKEN, error.error)
    }

    // ── Setting a password from a code ──────────────────────────────────

    @Test
    fun `setting a new password requires the mailed confirmation code`() {
        val existing = user().also { users.given(it) }
        tokens.given(verificationToken(id = 5L, token = "mailed", username = existing.email, tokenType = TokenTypes.PASSWORD_RESET.value))

        val error =
            assertFailsWith<ApiException> { service.setNewPassword(5L, ResetPasswordCommand("new one", confirmationCode = "guessed")) }

        assertEquals(IamError.INVALID_CONFIRMATION_CODE, error.error)
        assertTrue(identity.calls.isEmpty())
    }

    @Test
    fun `a matching confirmation code sets the new password`() {
        val existing = user().also { users.given(it) }
        tokens.given(verificationToken(id = 5L, token = "mailed", username = existing.email, tokenType = TokenTypes.PASSWORD_RESET.value))

        service.setNewPassword(5L, ResetPasswordCommand("new one", confirmationCode = "mailed"))

        assertTrue(identity.calls.contains("resetPassword(${existing.keycloakId})"))
        assertTrue(tokens.findById(5L)!!.used)
    }

    @Test
    fun `an unknown token id is refused`() {
        val error = assertFailsWith<ApiException> { service.setNewPassword(404L, ResetPasswordCommand("new one", "mailed")) }

        assertEquals(IamError.INVALID_TOKEN_ID, error.error)
    }

    // ── Resending activation ────────────────────────────────────────────

    @Test
    fun `resending activation to an unknown address succeeds silently`() {
        service.resendActivationEmail("nobody@example.com")

        assertTrue(events.published.isEmpty())
        assertTrue(saga.trail.isEmpty())
    }

    @Test
    fun `resending activation issues a fresh token`() {
        users.given(user())

        service.resendActivationEmail("ada@example.com")

        assertEquals(TokenTypes.ACCOUNT_ACTIVATION.value, savedToken().tokenType)
        assertEquals(listOf("MailRequested(ada@example.com,ACTIVATE_ACCOUNT)"), events.published)
    }
}
