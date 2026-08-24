package com.vertyll.veds.notification.application.saga.model

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    NOTIFICATION_PROCESSING("NotificationProcessing"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = SagaTypes.entries.find { it.value == value }
    }
}
