package com.vertyll.projecta.sharedinfrastructure.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for Kafka topics.
 * Each microservice defines its own topics in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "projecta.shared.kafka.topics")
class KafkaTopicsConfig {
    var all: Map<String, String> = emptyMap()
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

    private fun getTopic(topicKey: String): String = publish[topicKey] ?: subscribe[topicKey] ?: all[topicKey] ?: topicKey

    // Auth/User service topics
    fun getUserRegisteredTopic(): String = getTopic(TOPIC_USER_REGISTERED)

    fun getUserActivatedTopic(): String = getTopic(TOPIC_USER_ACTIVATED)

    fun getCredentialsVerificationTopic(): String = getTopic(TOPIC_CREDENTIALS_VERIFICATION)

    fun getCredentialsVerificationResultTopic(): String = getTopic(TOPIC_CREDENTIALS_VERIFICATION_RESULT)

    fun getUserUpdatedTopic(): String = getTopic(TOPIC_USER_UPDATED)

    fun getUserEmailUpdatedTopic(): String = getTopic(TOPIC_USER_EMAIL_UPDATED)

    fun getUserDeletedTopic(): String = getTopic(TOPIC_USER_DELETED)

    fun getUserDeletionTopic(): String = getTopic(TOPIC_USER_DELETION)

    fun getUserCreationConfirmedTopic(): String = getTopic(TOPIC_USER_CREATION_CONFIRMED)

    // Role service topics
    fun getRoleCreatedTopic(): String = getTopic(TOPIC_ROLE_CREATED)

    fun getRoleUpdatedTopic(): String = getTopic(TOPIC_ROLE_UPDATED)

    fun getRoleDeletedTopic(): String = getTopic(TOPIC_ROLE_DELETED)

    fun getRoleAssignedTopic(): String = getTopic(TOPIC_ROLE_ASSIGNED)

    fun getRoleRevokedTopic(): String = getTopic(TOPIC_ROLE_REVOKED)

    // Mail service topics
    fun getMailRequestedTopic(): String = getTopic(TOPIC_MAIL_REQUESTED)

    fun getMailSentTopic(): String = getTopic(TOPIC_MAIL_SENT)

    fun getMailFailedTopic(): String = getTopic(TOPIC_MAIL_FAILED)

    // Saga topics
    fun getSagaCompensationTopic(): String = getTopic(TOPIC_SAGA_COMPENSATION)
}
