package com.vertyll.projecta.sharedinfrastructure.event.user

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.vertyll.projecta.sharedinfrastructure.event.DomainEvent
import com.vertyll.projecta.sharedinfrastructure.event.EventType
import java.time.Instant

data class UserProfileUpdatedEvent @JsonCreator constructor(
    @JsonProperty("eventId")
    override val eventId: String = DomainEvent.generateEventId(),

    @JsonProperty("timestamp")
    override val timestamp: Instant = DomainEvent.now(),

    @JsonProperty("eventType")
    override val eventType: String = EventType.USER_UPDATED.value,

    @JsonProperty("userId")
    val userId: Long? = null,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("updatedFields")
    val updatedFields: Map<String, String> = emptyMap(),

    @JsonProperty("updateType")
    val updateType: UpdateType,

    @JsonProperty("sagaId")
    override val sagaId: String? = null
) : DomainEvent {
    // Secondary constructor for Jackson deserialization when no arguments are provided
    constructor() : this(
        eventId = DomainEvent.generateEventId(),
        timestamp = DomainEvent.now(),
        eventType = EventType.USER_UPDATED.value,
        email = "",
        updateType = UpdateType.PROFILE
    )

    enum class UpdateType {
        EMAIL,
        PASSWORD,
        PROFILE
    }
}
