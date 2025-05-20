package com.vertyll.projecta.common.event.user

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.vertyll.projecta.common.event.DomainEvent
import java.time.Instant

data class UserRegisteredEvent @JsonCreator constructor(
    @JsonProperty("eventId") override val eventId: String = DomainEvent.generateEventId(),
    @JsonProperty("timestamp") override val timestamp: Instant = DomainEvent.now(),
    @JsonProperty("eventType") override val eventType: String = "USER_REGISTERED",
    @JsonProperty("userId") val userId: Long,
    @JsonProperty("email") val email: String,
    @JsonProperty("firstName") val firstName: String,
    @JsonProperty("lastName") val lastName: String,
    @JsonProperty("roles") val roles: Set<String> = setOf("USER"),
    @JsonProperty("eventSource") val eventSource: String = "AUTH_SERVICE",
    @JsonProperty("sagaId") override val sagaId: String? = null
) : DomainEvent {
    // Secondary constructor for Jackson deserialization when no arguments are provided
    constructor() : this(
        eventId = DomainEvent.generateEventId(),
        timestamp = DomainEvent.now(),
        eventType = "USER_REGISTERED",
        userId = 0,
        email = "",
        firstName = "",
        lastName = ""
    )
} 