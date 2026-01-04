package com.vertyll.projecta.sharedinfrastructure.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for Kafka topics.
 * Each microservice defines its own topics in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "kafka.topics")
class KafkaTopicsConfig {
    var publish: Map<String, String> = emptyMap()
    var subscribe: Map<String, String> = emptyMap()

    companion object {
        // Topic names - Auth/User service
        private const val TOPIC_USER_REGISTERED = "user-registered"
        private const val TOPIC_USER_ACTIVATED = "user-activated"
        private const val TOPIC_CREDENTIALS_VERIFICATION = "credentials-verification"
        private const val TOPIC_CREDENTIALS_VERIFICATION_RESULT = "credentials-verification-result"
        private const val TOPIC_USER_UPDATED = "user-updated"
        private const val TOPIC_USER_EMAIL_UPDATED = "user-email-updated"
        private const val TOPIC_USER_DELETED = "user-deleted"
        private const val TOPIC_USER_DELETION = "user-deletion"
        private const val TOPIC_USER_CREATION_CONFIRMED = "user-creation-confirmed"

        // Topic names - Role service
        private const val TOPIC_ROLE_CREATED = "role-created"
        private const val TOPIC_ROLE_UPDATED = "role-updated"
        private const val TOPIC_ROLE_DELETED = "role-deleted"
        private const val TOPIC_ROLE_ASSIGNED = "role-assigned"
        private const val TOPIC_ROLE_REVOKED = "role-revoked"

        // Topic names - Mail service
        private const val TOPIC_MAIL_REQUESTED = "mail-requested"
        private const val TOPIC_MAIL_SENT = "mail-sent"
        private const val TOPIC_MAIL_FAILED = "mail-failed"

        // Topic names - Saga
        private const val TOPIC_SAGA_COMPENSATION = "saga-compensation"
    }

    // Helper methods to retrieve topics

    // Auth/User service topics
    fun getUserRegisteredTopic(): String = publish[TOPIC_USER_REGISTERED] ?: subscribe[TOPIC_USER_REGISTERED] ?: TOPIC_USER_REGISTERED

    fun getUserActivatedTopic(): String = publish[TOPIC_USER_ACTIVATED] ?: subscribe[TOPIC_USER_ACTIVATED] ?: TOPIC_USER_ACTIVATED

    fun getCredentialsVerificationTopic(): String =
        publish[TOPIC_CREDENTIALS_VERIFICATION] ?: subscribe[TOPIC_CREDENTIALS_VERIFICATION] ?: TOPIC_CREDENTIALS_VERIFICATION

    fun getCredentialsVerificationResultTopic(): String =
        publish[TOPIC_CREDENTIALS_VERIFICATION_RESULT] ?: subscribe[TOPIC_CREDENTIALS_VERIFICATION_RESULT]
            ?: TOPIC_CREDENTIALS_VERIFICATION_RESULT

    fun getUserUpdatedTopic(): String = publish[TOPIC_USER_UPDATED] ?: subscribe[TOPIC_USER_UPDATED] ?: TOPIC_USER_UPDATED

    fun getUserEmailUpdatedTopic(): String =
        publish[TOPIC_USER_EMAIL_UPDATED] ?: subscribe[TOPIC_USER_EMAIL_UPDATED] ?: TOPIC_USER_EMAIL_UPDATED

    fun getUserDeletedTopic(): String = publish[TOPIC_USER_DELETED] ?: subscribe[TOPIC_USER_DELETED] ?: TOPIC_USER_DELETED

    fun getUserDeletionTopic(): String = publish[TOPIC_USER_DELETION] ?: subscribe[TOPIC_USER_DELETION] ?: TOPIC_USER_DELETION

    fun getUserCreationConfirmedTopic(): String =
        publish[TOPIC_USER_CREATION_CONFIRMED] ?: subscribe[TOPIC_USER_CREATION_CONFIRMED] ?: TOPIC_USER_CREATION_CONFIRMED

    // Role service topics
    fun getRoleCreatedTopic(): String = publish[TOPIC_ROLE_CREATED] ?: subscribe[TOPIC_ROLE_CREATED] ?: TOPIC_ROLE_CREATED

    fun getRoleUpdatedTopic(): String = publish[TOPIC_ROLE_UPDATED] ?: subscribe[TOPIC_ROLE_UPDATED] ?: TOPIC_ROLE_UPDATED

    fun getRoleDeletedTopic(): String = publish[TOPIC_ROLE_DELETED] ?: subscribe[TOPIC_ROLE_DELETED] ?: TOPIC_ROLE_DELETED

    fun getRoleAssignedTopic(): String = publish[TOPIC_ROLE_ASSIGNED] ?: subscribe[TOPIC_ROLE_ASSIGNED] ?: TOPIC_ROLE_ASSIGNED

    fun getRoleRevokedTopic(): String = publish[TOPIC_ROLE_REVOKED] ?: subscribe[TOPIC_ROLE_REVOKED] ?: TOPIC_ROLE_REVOKED

    // Mail service topics
    fun getMailRequestedTopic(): String = publish[TOPIC_MAIL_REQUESTED] ?: subscribe[TOPIC_MAIL_REQUESTED] ?: TOPIC_MAIL_REQUESTED

    fun getMailSentTopic(): String = publish[TOPIC_MAIL_SENT] ?: subscribe[TOPIC_MAIL_SENT] ?: TOPIC_MAIL_SENT

    fun getMailFailedTopic(): String = publish[TOPIC_MAIL_FAILED] ?: subscribe[TOPIC_MAIL_FAILED] ?: TOPIC_MAIL_FAILED

    // Saga topics
    fun getSagaCompensationTopic(): String =
        publish[TOPIC_SAGA_COMPENSATION] ?: subscribe[TOPIC_SAGA_COMPENSATION] ?: TOPIC_SAGA_COMPENSATION
}
