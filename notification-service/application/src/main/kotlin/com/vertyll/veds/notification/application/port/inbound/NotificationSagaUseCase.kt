package com.vertyll.veds.notification.application.port.inbound

import com.vertyll.veds.notification.domain.model.Notification

/**
 * Driving port for the saga-driven notification-processing use case.
 *
 * Reference contract — replace with the real use cases when cloning the
 * notification service for a new bounded context.
 */
@Suppress("kotlin:S6517")
interface NotificationSagaUseCase {
    fun processNotificationWithSaga(
        name: String,
        payload: String,
    ): Notification
}
