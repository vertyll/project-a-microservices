package com.vertyll.projecta.sharedinfrastructure.event

enum class EventType(
    val value: String,
) {
    // User events
    USER_REGISTERED("USER_REGISTERED"),
    USER_UPDATED("USER_UPDATED"),

    // Authentication events
    CREDENTIALS_VERIFICATION("CREDENTIALS_VERIFICATION"),
    CREDENTIALS_VERIFICATION_RESULT("CREDENTIALS_VERIFICATION_RESULT"),

    // Mail events
    MAIL_REQUESTED("MAIL_REQUESTED"),
    MAIL_SENT("MAIL_SENT"),
    MAIL_FAILED("MAIL_FAILED"),

    // Role events
    ROLE_CREATED("ROLE_CREATED"),
    ROLE_UPDATED("ROLE_UPDATED"),
    ROLE_ASSIGNED("ROLE_ASSIGNED"),
    ROLE_REVOKED("ROLE_REVOKED"),
    ;

    companion object {
        fun fromString(value: String): EventType? = EventType.entries.find { it.value == value }
    }
}
