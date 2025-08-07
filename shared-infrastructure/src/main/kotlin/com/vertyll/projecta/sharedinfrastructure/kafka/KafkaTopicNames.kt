package com.vertyll.projecta.sharedinfrastructure.kafka

/**
 * Enum class representing the names of Kafka topics used in the application.
 */
enum class KafkaTopicNames(val value: String) {
    // Auth service topics
    USER_REGISTERED("user-registered"),
    USER_ACTIVATED("user-activated"),
    USER_PASSWORD_RESET("user-password-reset"),
    CREDENTIALS_VERIFICATION("credentials-verification"),
    CREDENTIALS_VERIFICATION_RESULT("credentials-verification-result"),

    // User service topics
    USER_UPDATED("user-updated"),
    USER_EMAIL_UPDATED("user-email-updated"),
    USER_PASSWORD_UPDATED("user-password-updated"),
    USER_DELETED("user-deleted"),
    USER_DELETION("user-deletion"),
    USER_CREATION_CONFIRMED("user-creation-confirmed"),

    // Role service topics
    ROLE_CREATED("role-created"),
    ROLE_UPDATED("role-updated"),
    ROLE_DELETED("role-deleted"),
    ROLE_ASSIGNED("role-assigned"),
    ROLE_REVOKED("role-revoked"),

    // Mail service topics
    MAIL_REQUESTED("mail-requested"),
    MAIL_SENT("mail-sent"),
    MAIL_FAILED("mail-failed"),

    // Saga topics
    SAGA_COMPENSATION("saga-compensation");

    companion object {
        fun fromString(value: String): KafkaTopicNames? {
            return KafkaTopicNames.entries.find { it.value == value }
        }
    }
}
