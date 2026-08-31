package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The saga log is the only record of a workflow spanning services, so the engine's job is to make
 * every transition either happen or be refused — never to half-apply one. These pin the rules a
 * participant relies on when it reacts to an event it did not originate.
 */
class SagaEngineTest {
    private val sagas = InMemorySagaRepository()
    private val steps = InMemoryStepRepository()
    private val compensated = mutableListOf<String>()

    private val engine =
        SagaEngine(
            sagaRepository = sagas,
            sagaStepRepository = steps,
            objectMapper = ObjectMapper(),
            entityFactory = TestEntityFactory(),
            compensationRunner = { sagaId -> compensated += sagaId },
        )

    private fun startedSaga() = engine.startSaga("invitation", mapOf("email" to "a@example.com"))

    @Test
    fun `a new saga starts in STARTED and serialises its payload`() {
        val saga = startedSaga()

        assertEquals(SagaStatus.STARTED, saga.status)
        assertContains(saga.payload, "a@example.com")
        assertEquals(saga, sagas.findOneById(saga.id))
    }

    @Test
    fun `each saga gets its own identity`() {
        assertTrue(startedSaga().id != startedSaga().id)
    }

    @Test
    fun `an unknown saga id is an error, not a silent no-op`() {
        listOf<(String) -> Unit>(
            { engine.completeSaga(it) },
            { engine.awaitResponse(it) },
            { engine.failSaga(it, "boom") },
        ).forEach { operation ->
            val failure = assertFailsWith<IllegalArgumentException> { operation("no-such-saga") }
            assertContains(failure.message.orEmpty(), "no-such-saga")
        }
    }

    @Test
    fun `awaiting a response leaves the saga open`() {
        val saga = engine.awaitResponse(startedSaga().id)

        assertEquals(SagaStatus.AWAITING_RESPONSE, saga.status)
        assertTrue(!saga.status.isTerminal())
    }

    /**
     * Late feedback is normal in a choreographed flow: a mail bounce can arrive after the saga
     * already finished. Reopening it would restart work that was deliberately concluded.
     */
    @Test
    fun `a terminal saga ignores further transitions`() {
        val id = startedSaga().id
        engine.completeSaga(id)

        assertEquals(SagaStatus.COMPLETED, engine.awaitResponse(id).status)
        assertEquals(SagaStatus.COMPLETED, engine.failSaga(id, "too late").status)
        assertTrue(compensated.isEmpty(), "a completed saga is not compensated by a late failure")
    }

    /**
     * Compensation is scheduled rather than run inline: it must not happen if the business
     * transaction that failed the saga is itself rolled back.
     */
    @Test
    fun `failing a saga moves it to COMPENSATING and schedules compensation`() {
        val id = startedSaga().id

        val failed = engine.failSaga(id, "mail bounced")

        assertEquals(SagaStatus.COMPENSATING, failed.status)
        assertEquals("mail bounced", failed.lastError)
        assertEquals(listOf(id), compensated)
    }

    @Test
    fun `recording a step stores it against its saga`() {
        val id = startedSaga().id

        engine.recordSagaStep(id, "REQUEST_MAIL", SagaStepStatus.COMPLETED, mapOf("invitationId" to "1"))

        val step = steps.findBySagaId(id).single()
        assertEquals("REQUEST_MAIL", step.stepName)
        assertEquals(SagaStepStatus.COMPLETED, step.status)
        assertContains(step.payload.orEmpty(), "invitationId")
    }

    /**
     * The same event can be delivered twice, so re-recording a step in the state it already holds
     * must not add a second row — `uk_saga_step (saga_id, step_name)` would reject it anyway.
     */
    @Test
    fun `re-recording a step in the same status is a no-op`() {
        val id = startedSaga().id

        engine.recordSagaStep(id, "REQUEST_MAIL", SagaStepStatus.COMPLETED)
        engine.recordSagaStep(id, "REQUEST_MAIL", SagaStepStatus.COMPLETED)

        assertEquals(1, steps.findBySagaId(id).size)
    }

    @Test
    fun `a failed step drives the saga into compensation`() {
        val id = startedSaga().id

        engine.recordSagaStep(id, "REQUEST_MAIL", SagaStepStatus.FAILED)

        assertEquals(SagaStatus.COMPENSATING, sagas.findOneById(id)!!.status)
        assertEquals(listOf(id), compensated)
    }

    @Test
    fun `findSagaById returns null for an unknown id rather than throwing`() {
        assertEquals(null, engine.findSagaById("absent"))
    }
}
