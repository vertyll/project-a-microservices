package com.vertyll.projecta.common.event.role

import com.vertyll.projecta.common.event.DomainEvent
import com.vertyll.projecta.common.event.EventType
import java.time.Instant

data class RoleCreatedEvent(
    override val eventId: String = DomainEvent.generateEventId(),
    override val timestamp: Instant = DomainEvent.now(),
    override val eventType: String = EventType.ROLE_CREATED.value,
    val roleId: Long,
    val name: String,
    val description: String?,
    override val sagaId: String? = null
) : DomainEvent
