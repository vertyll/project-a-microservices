package com.vertyll.projecta.sharedinfrastructure.event.role

import com.fasterxml.jackson.annotation.JsonTypeName
import com.vertyll.projecta.sharedinfrastructure.event.DomainEvent
import com.vertyll.projecta.sharedinfrastructure.event.EventType
import java.time.Instant

@JsonTypeName("ROLE_UPDATED")
data class RoleUpdatedEvent(
    override val eventId: String = DomainEvent.generateEventId(),
    override val timestamp: Instant = DomainEvent.now(),
    override val eventType: String = EventType.ROLE_UPDATED.value,
    val roleId: Long,
    val name: String,
    val description: String?,
    override val sagaId: String? = null,
) : DomainEvent
