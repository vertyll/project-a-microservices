package com.vertyll.veds.notification.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaStepNames(
    override val value: String,
) : SagaTypeValue {
    PROCESS_NOTIFICATION("ProcessNotification"),
    PERSIST_NOTIFICATION("PersistNotification"),
    PUBLISH_NOTIFICATION_EVENT("PublishNotificationEvent"),
    ;

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"
    }
}
