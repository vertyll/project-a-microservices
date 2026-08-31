package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.RecordingSagaProcess
import com.vertyll.veds.iam.application.SilentLogger
import com.vertyll.veds.iam.application.saga.model.SagaTypes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mail delivery answers back on a topic every service listens to. Whether a delivered mail finishes
 * the workflow depends on what the mail was for: an activation notice ends there, but a mail asking
 * the user to confirm something has only just handed the workflow over to them.
 */
internal class MailFeedbackServiceTest {
    private val saga = RecordingSagaProcess()
    private val service = MailFeedbackService(saga, SilentLogger)

    private fun givenSaga(type: SagaTypes) = saga.startSaga(type, emptyMap()).also { saga.trail.clear() }

    @Test
    fun `a delivered registration mail completes the saga`() {
        val own = givenSaga(SagaTypes.USER_REGISTRATION)

        service.handleMailSent(own.id, to = "ada@example.com")

        assertEquals(listOf("completed"), saga.trail)
    }

    /**
     * The mail only asks the user to confirm; the change itself has not happened yet. Completing
     * here would close a workflow whose last step is still outstanding, and the watchdog would
     * never notice a confirmation that never arrives.
     */
    @Test
    fun `a confirmation mail leaves the saga waiting for the user`() {
        listOf(SagaTypes.EMAIL_CHANGE, SagaTypes.PASSWORD_CHANGE).forEach { type ->
            saga.trail.clear()
            val own = givenSaga(type)

            service.handleMailSent(own.id, to = "ada@example.com")

            assertTrue(saga.trail.isEmpty(), "$type was completed before the user confirmed")
        }
    }

    @Test
    fun `a failed delivery fails the saga and keeps the reason`() {
        val own = givenSaga(SagaTypes.USER_REGISTRATION)

        service.handleMailFailed(own.id, to = "ada@example.com", error = "mailbox full")

        assertEquals(listOf("failed(Mail delivery failed: mailbox full)"), saga.trail)
    }

    @Test
    fun `a saga this service does not own is ignored`() {
        service.handleMailSent("someone-elses-saga", to = "ada@example.com")

        assertTrue(saga.trail.isEmpty())
    }

    /** Not every mail belongs to a workflow; a plain notification carries no saga at all. */
    @Test
    fun `a mail sent outside any saga is ignored`() {
        service.handleMailSent(null, to = "ada@example.com")
        service.handleMailFailed(null, to = "ada@example.com", error = "mailbox full")

        assertTrue(saga.trail.isEmpty())
    }
}
