package com.vertyll.projecta.sharedinfrastructure.event.user

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.vertyll.projecta.sharedinfrastructure.event.DomainEvent
import com.vertyll.projecta.sharedinfrastructure.event.EventType
import java.time.Instant

@JsonTypeName("CREDENTIALS_VERIFICATION")
data class CredentialsVerificationEvent
    @JsonCreator
    constructor(
        @JsonProperty("eventId")
        override val eventId: String = DomainEvent.generateEventId(),
        @JsonProperty("timestamp")
        override val timestamp: Instant = DomainEvent.now(),
        @JsonProperty("eventType")
        override val eventType: String = EventType.CREDENTIALS_VERIFICATION.value,
        @JsonProperty("email")
        val email: String,
        @JsonProperty("password")
        val password: String,
        @JsonProperty("requestId")
        val requestId: String = DomainEvent.generateEventId(),
        @JsonProperty("sagaId")
        override val sagaId: String? = null,
    ) : DomainEvent {
        // Secondary constructor for Jackson deserialization when no arguments are provided
        constructor() : this(
            eventId = DomainEvent.generateEventId(),
            timestamp = DomainEvent.now(),
            eventType = EventType.CREDENTIALS_VERIFICATION.value,
            email = "",
            password = "",
        )
    }
