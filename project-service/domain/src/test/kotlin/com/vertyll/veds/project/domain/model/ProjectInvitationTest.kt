package com.vertyll.veds.project.domain.model

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectInvitationTest {
    private val projectId = UUID.randomUUID()
    private val inviterId = UUID.randomUUID()
    private val inviteeId = UUID.randomUUID()
    private val roleId = UUID.randomUUID()

    private fun pending() =
        ProjectInvitation.create(
            projectId = projectId,
            inviteeEmail = "invitee@example.com",
            inviterId = inviterId,
            roleId = roleId,
        )

    @Test
    fun `starts pending and records who accepted it`() {
        val accepted = pending().accept(inviteeId)

        assertEquals(InvitationStatus.ACCEPTED, accepted.status)
        assertEquals(inviteeId, accepted.inviteeId)
    }

    @Test
    fun `cannot be accepted twice`() {
        val accepted = pending().accept(inviteeId)

        assertFailsWith<IllegalStateException> { accepted.accept(inviteeId) }
    }

    @Test
    fun `cannot be accepted after rejection`() {
        val rejected = pending().reject(inviteeId)

        assertFailsWith<IllegalStateException> { rejected.accept(inviteeId) }
    }

    @Test
    fun `cannot be expired once it is no longer pending`() {
        val accepted = pending().accept(inviteeId)

        assertFailsWith<IllegalStateException> { accepted.expire() }
    }

    @Test
    fun `reports expiry only while pending`() {
        val invitation = pending()
        val afterValidity = invitation.expiresAt.plus(1, ChronoUnit.DAYS)

        assertTrue(invitation.hasExpiredAt(afterValidity))
        assertFalse(invitation.hasExpiredAt(Instant.now()))

        assertFalse(invitation.accept(inviteeId).hasExpiredAt(afterValidity))
    }

    @Test
    fun `rejects a blank invitee address`() {
        assertFailsWith<IllegalArgumentException> {
            ProjectInvitation.create(
                projectId = projectId,
                inviteeEmail = "  ",
                inviterId = inviterId,
                roleId = roleId,
            )
        }
    }
}
