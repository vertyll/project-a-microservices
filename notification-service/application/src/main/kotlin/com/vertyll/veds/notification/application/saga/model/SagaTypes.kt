package com.vertyll.veds.notification.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    NOTIFICATION_PROCESSING("NotificationProcessing"),
}
