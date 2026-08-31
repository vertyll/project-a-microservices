package com.vertyll.veds.mail.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The log is the only evidence that a mail was attempted. Support reads it to answer "did it go
 * out?", so an attempt that ends without a status, or a failure without a reason, leaves that
 * question unanswerable.
 */
class EmailLogTest {
    private fun log() =
        EmailLog(
            recipient = "ada@example.com",
            subject = "Activate your account",
            templateName = EmailTemplate.ACTIVATE_ACCOUNT.templateName,
        )

    @Test
    fun `a new entry is pending and has not been sent`() {
        val entry = log()

        assertEquals(EmailStatus.PENDING, entry.status)
        assertNull(entry.sentAt)
        assertNull(entry.errorMessage)
    }

    @Test
    fun `a sent mail records when it went out`() {
        val entry = log().markAsSent()

        assertEquals(EmailStatus.SENT, entry.status)
        assertNotNull(entry.sentAt)
    }

    @Test
    fun `a failed mail keeps the reason it failed`() {
        val entry = log().markAsFailed("mailbox does not exist")

        assertEquals(EmailStatus.FAILED, entry.status)
        assertEquals("mailbox does not exist", entry.errorMessage)
    }

    /** Nothing was delivered, so the entry must not claim a send time. */
    @Test
    fun `a failed mail carries no send time`() {
        assertNull(log().markAsFailed("mailbox does not exist").sentAt)
    }

    /** What the mail was and who it was for stays put; only the outcome moves. */
    @Test
    fun `recording an outcome does not disturb what was sent`() {
        val entry = log()

        listOf(entry.markAsSent(), entry.markAsFailed("refused")).forEach { after ->
            assertEquals(entry.recipient, after.recipient)
            assertEquals(entry.subject, after.subject)
            assertEquals(entry.templateName, after.templateName)
            assertEquals(entry.createdAt, after.createdAt)
        }
    }
}
