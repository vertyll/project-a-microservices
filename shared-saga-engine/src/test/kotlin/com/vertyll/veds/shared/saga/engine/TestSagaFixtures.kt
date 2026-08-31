package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.Saga
import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStep
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.engine.persistence.SagaRepositoryPort
import com.vertyll.veds.shared.saga.engine.persistence.SagaStepRepositoryPort
import java.time.Instant

/**
 * In-memory stand-ins for the saga log.
 *
 * The engine talks to ports, so its rules can be exercised without a database — which is the
 * point of the port in the first place. What these fixtures do *not* fake is the state machine
 * itself: `markCompleted` and friends behave exactly as a persisted aggregate would.
 */
internal data class TestSaga(
    override val id: String,
    override val type: String,
    override val status: SagaStatus,
    override val payload: String,
    override val lastError: String? = null,
    override val startedAt: Instant = Instant.now(),
    override val completedAt: Instant? = null,
    override val updatedAt: Instant = Instant.now(),
    override val version: Long? = null,
) : Saga<TestSaga> {
    /**
     * Every transition refreshes [updatedAt], exactly as `BaseSaga` does. The watchdog selects on
     * that column alone, so a fixture that let it go stale would report retries the real engine
     * never performs.
     */
    private fun transition(
        status: SagaStatus,
        lastError: String? = this.lastError,
        completed: Boolean = false,
    ): TestSaga {
        val now = Instant.now()
        return copy(
            status = status,
            lastError = lastError,
            completedAt = if (completed) now else completedAt,
            updatedAt = now,
        )
    }

    override fun markCompleted() = transition(SagaStatus.COMPLETED, completed = true)

    override fun markAwaitingResponse() = transition(SagaStatus.AWAITING_RESPONSE)

    override fun markFailed(error: String) = transition(SagaStatus.FAILED, lastError = error, completed = true)

    override fun startCompensating(error: String) = transition(SagaStatus.COMPENSATING, lastError = error)

    override fun markCompensated() = transition(SagaStatus.COMPENSATED, completed = true)

    override fun markCompensationFailed() = transition(SagaStatus.COMPENSATION_FAILED, completed = true)
}

internal data class TestStep(
    override val id: Long? = null,
    override val sagaId: String,
    override val stepName: String,
    override val status: SagaStepStatus,
    override val payload: String? = null,
    override val errorMessage: String? = null,
    override val createdAt: Instant = Instant.now(),
    override val completedAt: Instant? = null,
    override val compensationStepId: Long? = null,
    override val version: Long? = null,
) : SagaStep<TestStep> {
    override fun markCompleted() = copy(status = SagaStepStatus.COMPLETED, completedAt = Instant.now())

    override fun markFailed(error: String) = copy(status = SagaStepStatus.FAILED, errorMessage = error)

    override fun markCompensated() = copy(status = SagaStepStatus.COMPENSATED, completedAt = Instant.now())

    override fun markCompensationFailed(error: String?) = copy(status = SagaStepStatus.COMPENSATION_FAILED, errorMessage = error)

    override fun linkToCompensationStep(compensationStepId: Long) = copy(compensationStepId = compensationStepId)
}

internal class InMemorySagaRepository : SagaRepositoryPort<TestSaga> {
    val stored = linkedMapOf<String, TestSaga>()

    override fun save(saga: TestSaga): TestSaga = saga.also { stored[it.id] = it }

    override fun findOneById(id: String): TestSaga? = stored[id]

    override fun findByType(type: String) = stored.values.filter { it.type == type }

    override fun findByStatus(status: SagaStatus) = stored.values.filter { it.status == status }

    override fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ) = stored.values.filter { it.type == type && it.status == status }

    override fun findByStartedAtBefore(startedAt: Instant) = stored.values.filter { it.startedAt < startedAt }

    override fun findByStatusInAndStartedAtBefore(
        statuses: List<SagaStatus>,
        startedAt: Instant,
    ) = stored.values.filter { it.status in statuses && it.startedAt < startedAt }

    override fun findByStatusInAndUpdatedAtBefore(
        statuses: List<SagaStatus>,
        updatedAt: Instant,
    ) = stored.values.filter { it.status in statuses && it.updatedAt < updatedAt }
}

internal class InMemoryStepRepository : SagaStepRepositoryPort<TestStep> {
    val stored = mutableListOf<TestStep>()
    private var nextId = 1L

    override fun save(step: TestStep): TestStep {
        val withId = step.id?.let { step } ?: step.copy(id = nextId++)
        stored.removeAll { it.id == withId.id }
        stored += withId
        return withId
    }

    override fun findOneById(id: Long) = stored.firstOrNull { it.id == id }

    override fun findBySagaId(sagaId: String) = stored.filter { it.sagaId == sagaId }

    override fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ) = stored.filter { it.sagaId == sagaId && it.stepName == stepName }

    override fun findBySagaIdAndStatus(
        sagaId: String,
        status: SagaStepStatus,
    ) = stored.filter { it.sagaId == sagaId && it.status == status }

    override fun findByStepNameAndStatus(
        stepName: String,
        status: SagaStepStatus,
    ) = stored.filter { it.stepName == stepName && it.status == status }

    override fun findByCompensationStepId(compensationStepId: Long) = stored.firstOrNull { it.compensationStepId == compensationStepId }
}

internal class TestEntityFactory : SagaEntityFactory<TestSaga, TestStep> {
    override fun createSaga(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ) = TestSaga(id = id, type = type, status = status, payload = payload, startedAt = startedAt)

    override fun createSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: String?,
        createdAt: Instant,
    ) = TestStep(sagaId = sagaId, stepName = stepName, status = status, payload = payload, createdAt = createdAt)
}
