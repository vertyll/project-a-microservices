package com.vertyll.projecta.common.event.user

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.vertyll.projecta.common.event.DomainEvent
import com.vertyll.projecta.common.event.EventType
import java.time.Instant

data class CredentialsVerificationResultEvent @JsonCreator constructor(
    @JsonProperty("eventId")
    override val eventId: String = DomainEvent.generateEventId(),

    @JsonProperty("timestamp")
    override val timestamp: Instant = DomainEvent.now(),

    @JsonProperty("eventType")
    override val eventType: String = EventType.CREDENTIALS_VERIFICATION_RESULT.value,

    @JsonProperty("requestId")
    val requestId: String,

    @JsonProperty("valid")
    val valid: Boolean,

    @JsonProperty("userId")
    val userId: Long? = null,

    @JsonProperty("email")
    val email: String? = null,

    @JsonProperty("roles")
    val roles: List<String> = emptyList(),

    @JsonProperty("message")
    val message: String? = null,

    @JsonProperty("sagaId")
    override val sagaId: String? = null
) : DomainEvent {
    // Secondary constructor for Jackson deserialization when no arguments are provided
    constructor() : this(
        eventId = DomainEvent.generateEventId(),
        timestamp = DomainEvent.now(),
        eventType = EventType.CREDENTIALS_VERIFICATION_RESULT.value,
        requestId = "",
        valid = false
    )
} 