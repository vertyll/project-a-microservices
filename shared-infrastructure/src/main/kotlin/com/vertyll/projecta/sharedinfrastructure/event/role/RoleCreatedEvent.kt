package com.vertyll.projecta.sharedinfrastructure.event.role

import com.vertyll.projecta.sharedinfrastructure.event.DomainEvent
import com.vertyll.projecta.sharedinfrastructure.event.EventType
import com.vertyll.projecta.sharedinfrastructure.role.RoleType
import java.time.Instant

data class RoleCreatedEvent(
    override val eventId: String = DomainEvent.generateEventId(),
    override val timestamp: Instant = DomainEvent.now(),
    override val eventType: String = EventType.ROLE_CREATED.value,
    val roleId: Long,
    val name: RoleType,
    val description: String?,
    override val sagaId: String? = null,
) : DomainEvent
