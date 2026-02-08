package com.vertyll.projecta.sharedinfrastructure.event.role

import com.fasterxml.jackson.annotation.JsonTypeName
import com.vertyll.projecta.sharedinfrastructure.event.DomainEvent
import com.vertyll.projecta.sharedinfrastructure.event.EventType
import java.time.Instant

@JsonTypeName("ROLE_CREATED")
data class RoleCreatedEvent(
    override val eventId: String = DomainEvent.generateEventId(),
    override val timestamp: Instant = DomainEvent.now(),
    override val eventType: String = EventType.ROLE_CREATED.value,
    val roleId: Long,
    val name: String,
    val description: String?,
    override val sagaId: String? = null,
) : DomainEvent
