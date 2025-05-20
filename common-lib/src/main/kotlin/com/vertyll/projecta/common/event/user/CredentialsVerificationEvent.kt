package com.vertyll.projecta.common.event.user

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.vertyll.projecta.common.event.DomainEvent
import java.time.Instant

data class CredentialsVerificationEvent @JsonCreator constructor(
    @JsonProperty("eventId") override val eventId: String = DomainEvent.generateEventId(),
    @JsonProperty("timestamp") override val timestamp: Instant = DomainEvent.now(),
    @JsonProperty("eventType") override val eventType: String = "CREDENTIALS_VERIFICATION",
    @JsonProperty("email") val email: String,
    @JsonProperty("password") val password: String,
    @JsonProperty("requestId") val requestId: String = DomainEvent.generateEventId(),
    @JsonProperty("sagaId") override val sagaId: String? = null
) : DomainEvent {
    // Secondary constructor for Jackson deserialization when no arguments are provided
    constructor() : this(
        eventId = DomainEvent.generateEventId(),
        timestamp = DomainEvent.now(),
        eventType = "CREDENTIALS_VERIFICATION",
        email = "",
        password = ""
    )
} 