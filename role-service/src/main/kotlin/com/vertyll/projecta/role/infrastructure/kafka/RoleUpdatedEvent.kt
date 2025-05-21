package com.vertyll.projecta.role.infrastructure.kafka

import com.vertyll.projecta.common.event.DomainEvent
import java.time.Instant

data class RoleUpdatedEvent(
    override val eventId: String = DomainEvent.generateEventId(),
    override val timestamp: Instant = DomainEvent.now(),
    override val eventType: String = "ROLE_UPDATED",
    val roleId: Long,
    val name: String,
    val description: String?,
    override val sagaId: String? = null
) : DomainEvent
