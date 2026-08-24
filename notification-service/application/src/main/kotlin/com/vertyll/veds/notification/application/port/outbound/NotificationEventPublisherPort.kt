package com.vertyll.veds.notification.application.port.outbound

/**
 * Outbound port for publishing notification-related domain events.
 *
 * Replace method signatures with concrete events when cloning this service for a new microservice.
 * Implementation lives in `infrastructure/kafka/KafkaNotificationEventPublisherAdapter`.
 */
interface NotificationEventPublisherPort {
    fun publishNotificationProcessed(
        sagaId: String,
        notificationId: Long,
        payload: Map<String, Any?> = emptyMap(),
    )

    fun publishNotificationFailed(
        sagaId: String,
        notificationId: Long?,
        error: String,
    )
}
