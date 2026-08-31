package com.vertyll.veds.template.application

import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.template.application.port.outbound.SagaProcessPort
import com.vertyll.veds.template.application.port.outbound.UseCaseLogger
import com.vertyll.veds.template.application.saga.model.Saga
import com.vertyll.veds.template.application.saga.model.SagaStepNames
import com.vertyll.veds.template.application.saga.model.SagaTypes
import com.vertyll.veds.template.domain.model.Template
import com.vertyll.veds.template.domain.repository.TemplateRepository

internal class InMemoryTemplateRepository : TemplateRepository {
    val stored = linkedMapOf<String, Template>()

    /** Set to make the next save fail, standing in for a constraint violation. */
    var saveFails: Exception? = null

    fun given(vararg templates: Template) = templates.forEach { stored[it.id] = it }

    override fun save(template: Template): Template {
        saveFails?.let { throw it }
        return template.also { stored[it.id] = it }
    }

    override fun findById(id: String) = stored[id]

    override fun deleteById(id: String) {
        stored.remove(id)
    }
}

internal class RecordingSagaProcess : SagaProcessPort {
    val trail = mutableListOf<String>()
    private val sagas = linkedMapOf<String, Saga>()

    override fun startSaga(
        sagaType: SagaTypes,
        payload: Map<String, Any?>,
    ): Saga =
        Saga(id = "saga-1", type = sagaType.value, payload = payload.toString())
            .also {
                sagas[it.id] = it
                trail += "start(${sagaType.value})"
            }

    override fun recordSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Map<String, Any?>,
    ) {
        trail += "step(${stepName.value},$status)"
    }

    override fun markSagaCompleted(sagaId: String) {
        trail += "completed"
    }

    override fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    ) {
        trail += "failed($errorMessage)"
    }

    override fun markAwaitingResponse(sagaId: String) {
        trail += "awaiting"
    }

    override fun findSagaDomainById(sagaId: String) = sagas[sagaId]
}

internal object SilentLogger : UseCaseLogger {
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
