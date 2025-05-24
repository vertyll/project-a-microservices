package com.vertyll.projecta.common.event.role

import com.vertyll.projecta.common.event.DomainEvent
import com.vertyll.projecta.common.event.EventType
import java.time.Instant

data class RoleAssignedEvent(
    override val eventId: String = DomainEvent.generateEventId(),
    override val timestamp: Instant = DomainEvent.now(),
    override val eventType: String = EventType.ROLE_ASSIGNED.value,
    val userId: Long,
    val roleId: Long,
    val roleName: String,
    override val sagaId: String? = null
) : DomainEvent
