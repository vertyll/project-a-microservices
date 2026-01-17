package com.vertyll.projecta.sharedinfrastructure.event.role

import com.vertyll.projecta.sharedinfrastructure.event.DomainEvent
import com.vertyll.projecta.sharedinfrastructure.event.EventType
import com.vertyll.projecta.sharedinfrastructure.role.RoleType
import java.time.Instant

data class RoleRevokedEvent(
    override val eventId: String = DomainEvent.generateEventId(),
    override val timestamp: Instant = DomainEvent.now(),
    override val eventType: String = EventType.ROLE_REVOKED.value,
    val userId: Long,
    val roleId: Long,
    val roleName: RoleType,
    override val sagaId: String? = null,
) : DomainEvent
