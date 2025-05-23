package com.vertyll.projecta.common.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.vertyll.projecta.common.event.mail.MailRequestedEvent
import com.vertyll.projecta.common.event.role.RoleCreatedEvent
import com.vertyll.projecta.common.event.role.RoleUpdatedEvent
import com.vertyll.projecta.common.event.user.CredentialsVerificationEvent
import com.vertyll.projecta.common.event.user.CredentialsVerificationResultEvent
import com.vertyll.projecta.common.event.user.UserProfileUpdatedEvent
import com.vertyll.projecta.common.event.user.UserRegisteredEvent
import java.time.Instant
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UserRegisteredEvent::class, name = "USER_REGISTERED"),
    JsonSubTypes.Type(value = UserProfileUpdatedEvent::class, name = "USER_UPDATED"),
    JsonSubTypes.Type(value = MailRequestedEvent::class, name = "MAIL_REQUESTED"),
    JsonSubTypes.Type(value = CredentialsVerificationEvent::class, name = "CREDENTIALS_VERIFICATION"),
    JsonSubTypes.Type(value = CredentialsVerificationResultEvent::class, name = "CREDENTIALS_VERIFICATION_RESULT"),
    JsonSubTypes.Type(value = RoleCreatedEvent::class, name = "ROLE_CREATED"),
    JsonSubTypes.Type(value = RoleUpdatedEvent::class, name = "ROLE_UPDATED")

)
interface DomainEvent {
    val eventId: String
    val timestamp: Instant
    val eventType: String
    val sagaId: String?

    companion object {
        fun generateEventId(): String = UUID.randomUUID().toString()
        fun now(): Instant = Instant.now()
    }
}
