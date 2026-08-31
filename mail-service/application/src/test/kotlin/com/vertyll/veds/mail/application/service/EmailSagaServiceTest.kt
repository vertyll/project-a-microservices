package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.application.port.outbound.MailFeedbackEventPublisherPort
import com.vertyll.veds.mail.application.port.outbound.SagaProcessPort
import com.vertyll.veds.mail.application.port.outbound.UseCaseLogger
import com.vertyll.veds.mail.application.saga.model.Saga
import com.vertyll.veds.mail.application.saga.model.SagaStepNames
import com.vertyll.veds.mail.application.saga.model.SagaTypes
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.shared.saga.SagaStepStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Every mail this service sends was asked for by another service that is now blocked waiting for
 * the answer. The rule these tests hold is that the requester always gets one — success, failure,
 * or a request that never even reached the mail server. Silence here strands a saga until the
 * watchdog times it out, long after the user gave up.
 */
class EmailSagaServiceTest {
    private val sagaTrail = mutableListOf<String>()
    private val feedback = mutableListOf<String>()

    private var sendSucceeds = true
    private var sendThrows: Exception? = null
    private var feedbackThrows: Exception? = null

    private val saga =
        object : SagaProcessPort {
            override fun startSaga(
                sagaType: SagaTypes,
                payload: Map<String, Any?>,
            ): Saga {
                sagaTrail += "start(${sagaType.value})"
                return Saga(id = "mail-saga-1", type = sagaType.value, payload = payload.toString())
            }

            override fun recordSagaStep(
                sagaId: String,
                stepName: SagaStepNames,
                status: SagaStepStatus,
                payload: Map<String, Any?>,
            ) {
                sagaTrail += "step(${stepName.value},$status)"
            }

            override fun markSagaCompleted(sagaId: String) {
                sagaTrail += "completed"
            }

            override fun markSagaFailed(
                sagaId: String,
                errorMessage: String,
            ) {
                sagaTrail += "failed"
            }

            override fun markAwaitingResponse(sagaId: String) {
                sagaTrail += "awaiting"
            }

            override fun findSagaDomainById(sagaId: String): Saga? = null
        }

    private val emails =
        object : EmailUseCase {
            override fun sendEmail(
                to: String,
                subject: String,
                template: EmailTemplate,
                variables: Map<String, String>,
                replyTo: String?,
            ): Boolean {
                sendThrows?.let { throw it }
                return sendSucceeds
            }

            override fun getEmailLogs() = error("not used by the saga")
        }

    private val publisher =
        object : MailFeedbackEventPublisherPort {
            override fun publishMailSent(
                originSagaId: String,
                to: String,
                subject: String,
                originalEventId: String,
            ) {
                feedbackThrows?.let { throw it }
                feedback += "sent($originSagaId,$originalEventId)"
            }

            override fun publishMailFailed(
                originSagaId: String,
                to: String,
                subject: String,
                originalEventId: String,
                error: String,
            ) {
                feedbackThrows?.let { throw it }
                feedback += "failed($originSagaId,$error)"
            }
        }

    private val logger =
        object : UseCaseLogger {
            override fun debug(
                message: String,
                vararg args: Any?,
            ) = Unit

            override fun info(
                message: String,
                vararg args: Any?,
            ) = Unit

            override fun warn(
                message: String,
                vararg args: Any?,
            ) = Unit

            override fun error(
                message: String,
                vararg args: Any?,
            ) = Unit
        }

    private val service = EmailSagaService(saga, emails, publisher, logger)

    private fun send(
        templateName: String = EmailTemplate.ACTIVATE_ACCOUNT.templateName,
        originSagaId: String? = "origin-saga",
        originalEventId: String? = "event-1",
    ) = service.sendEmailWithSaga(
        to = "ada@example.com",
        subject = "Activate your account",
        templateName = templateName,
        variables = mapOf("firstName" to "Ada"),
        replyTo = null,
        originSagaId = originSagaId,
        originalEventId = originalEventId,
    )

    @Test
    fun `a delivered mail completes its saga and tells the requester`() {
        assertTrue(send())

        assertEquals(
            listOf("start(EmailSending)", "step(ProcessTemplate,COMPLETED)", "step(SendEmail,COMPLETED)", "completed"),
            sagaTrail,
        )
        assertEquals(listOf("sent(origin-saga,event-1)"), feedback)
    }

    /**
     * A refused delivery is an outcome, not an error: the requester has to hear about it so it can
     * compensate, and the step is recorded as failed rather than silently dropped.
     */
    @Test
    fun `a refused delivery is reported back rather than swallowed`() {
        sendSucceeds = false

        assertFalse(send())

        assertTrue(sagaTrail.contains("step(SendEmail,FAILED)"))
        assertFalse(sagaTrail.contains("completed"))
        assertEquals(listOf("failed(origin-saga,Failed to send email)"), feedback)
    }

    /** An exception must not escape either — the requester would wait forever for an answer. */
    @Test
    fun `an exception during delivery still produces an answer`() {
        sendThrows = IllegalStateException("SMTP server refused the connection")

        assertFalse(send())

        assertTrue(sagaTrail.contains("step(SendEmail,FAILED)"))
        assertEquals(listOf("failed(origin-saga,SMTP server refused the connection)"), feedback)
    }

    /**
     * An unknown template name is a bad request, not a delivery problem. No saga is opened for work
     * that cannot start, but the requester is still told why nothing will arrive.
     */
    @Test
    fun `an unknown template is refused before any saga opens`() {
        assertFalse(send(templateName = "NO_SUCH_TEMPLATE"))

        assertTrue(sagaTrail.isEmpty())
        assertEquals(listOf("failed(origin-saga,Invalid template name: NO_SUCH_TEMPLATE)"), feedback)
    }

    /** Not every mail belongs to a workflow; a standalone one has nobody waiting to be told. */
    @Test
    fun `a mail sent outside any saga publishes no feedback`() {
        assertTrue(send(originSagaId = null))

        assertTrue(feedback.isEmpty())
        assertTrue(sagaTrail.contains("completed"), "the local saga is still recorded")
    }

    @Test
    fun `an unknown template outside a saga reports nothing to nobody`() {
        assertFalse(send(templateName = "NO_SUCH_TEMPLATE", originSagaId = null))

        assertTrue(feedback.isEmpty())
        assertTrue(sagaTrail.isEmpty())
    }

    /**
     * The mail was genuinely sent; only the announcement failed. Turning that into a failure would
     * make the requester compensate a delivery that actually happened — the inbox cannot be undone.
     */
    @Test
    fun `a delivered mail stays delivered even if the announcement fails`() {
        feedbackThrows = IllegalStateException("broker unavailable")

        assertTrue(send())

        assertTrue(sagaTrail.contains("completed"))
    }

    /** Without an event id of its own the local saga id identifies the delivery for deduplication. */
    @Test
    fun `a request with no event id is identified by the mail saga`() {
        assertTrue(send(originalEventId = null))

        assertEquals(listOf("sent(origin-saga,mail-saga-1)"), feedback)
    }
}
