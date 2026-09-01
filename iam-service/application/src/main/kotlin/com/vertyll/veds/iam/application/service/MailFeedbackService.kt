package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.port.inbound.MailFeedbackUseCase
import com.vertyll.veds.iam.application.port.outbound.SagaProcessPort
import com.vertyll.veds.iam.application.port.outbound.UseCaseLogger
import com.vertyll.veds.iam.application.saga.model.SagaTypes

class MailFeedbackService(
    private val sagaProcessPort: SagaProcessPort,
    private val logger: UseCaseLogger,
) : MailFeedbackUseCase {
    private companion object {
        val SAGA_TYPES_AWAITING_USER_CONFIRMATION =
            setOf(
                SagaTypes.EMAIL_CHANGE.value,
                SagaTypes.PASSWORD_CHANGE.value,
            )
    }

    override fun handleMailSent(
        sagaId: String?,
        to: String,
    ) {
        if (sagaId == null) {
            logger.debug("MailSent without sagaId — skipping saga step recording")
            return
        }
        logger.info("MailSentEvent for saga: {} (to: {})", sagaId, to)

        val saga = sagaProcessPort.findSagaDomainById(sagaId)
        if (saga == null) {
            logger.warn("Saga '{}' not found — skipping MailSentEvent", sagaId)
            return
        }
        if (saga.type in SAGA_TYPES_AWAITING_USER_CONFIRMATION) {
            logger.info(
                "Mail delivered for saga '{}' (type: {}) — saga remains AWAITING_RESPONSE until user confirms",
                sagaId,
                saga.type,
            )
            return
        }
        sagaProcessPort.markSagaCompleted(sagaId)
    }

    override fun handleMailFailed(
        sagaId: String?,
        to: String,
        error: String,
    ) {
        if (sagaId == null) {
            logger.debug("MailFailed without sagaId — skipping saga failure")
            return
        }
        logger.warn("MailFailedEvent for saga: {} (to: {}, error: {})", sagaId, to, error)
        sagaProcessPort.markSagaFailed(sagaId, "Mail delivery failed: $error")
    }
}
