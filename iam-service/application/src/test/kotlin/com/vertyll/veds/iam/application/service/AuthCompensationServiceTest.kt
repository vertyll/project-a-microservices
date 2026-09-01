package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.FakeIdentityProvider
import com.vertyll.veds.iam.application.InMemoryUserRepository
import com.vertyll.veds.iam.application.InMemoryVerificationTokenRepository
import com.vertyll.veds.iam.application.SilentLogger
import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand
import com.vertyll.veds.iam.application.user
import com.vertyll.veds.iam.application.verificationToken
import com.vertyll.veds.iam.domain.model.TokenTypes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AuthCompensationServiceTest {
    private val users = InMemoryUserRepository()
    private val tokens = InMemoryVerificationTokenRepository()
    private val identity = FakeIdentityProvider()

    private val service = AuthCompensationService(users, tokens, identity, SilentLogger)

    @Test
    fun `a half-registered user is deleted`() {
        val existing = user().also { users.given(it) }

        service.compensate(AuthCompensationCommand.DeleteUser(existing.id!!))

        assertNull(users.findById(existing.id!!))
    }

    @Test
    fun `deleting a user that is already gone is harmless`() {
        service.compensate(AuthCompensationCommand.DeleteUser(404L))
        service.compensate(AuthCompensationCommand.DeleteUser(404L))

        assertTrue(users.stored.isEmpty())
    }

    @Test
    fun `an orphaned verification token is deleted`() {
        val token = verificationToken(tokenType = TokenTypes.ACCOUNT_ACTIVATION.value).also { tokens.given(it) }

        service.compensate(AuthCompensationCommand.DeleteVerificationToken(token.id!!))

        assertNull(tokens.findById(token.id!!))
    }

    @Test
    fun `deleting a token that is already gone is harmless`() {
        service.compensate(AuthCompensationCommand.DeleteVerificationToken(404L))

        assertTrue(tokens.stored.isEmpty())
    }

    @Test
    fun `an email change is reverted in both the identity provider and the local record`() {
        val existing = user(email = "new@example.com").also { users.given(it) }

        service.compensate(AuthCompensationCommand.RevertEmailUpdate(existing.id!!, originalEmail = "old@example.com"))

        assertEquals("old@example.com", users.findById(existing.id!!)!!.email)
        assertTrue(identity.calls.contains("updateEmail(${existing.keycloakId},old@example.com)"))
    }

    @Test
    fun `reverting the email of a user that no longer exists is not an error`() {
        service.compensate(AuthCompensationCommand.RevertEmailUpdate(404L, originalEmail = "old@example.com"))

        assertTrue(identity.calls.isEmpty())
    }

    @Test
    fun `a password change cannot be reverted and nothing is touched`() {
        val existing = user().also { users.given(it) }

        service.compensate(AuthCompensationCommand.RevertPasswordUpdate(existing.id!!))

        assertTrue(identity.calls.isEmpty())
        assertEquals(existing, users.findById(existing.id!!))
    }
}
