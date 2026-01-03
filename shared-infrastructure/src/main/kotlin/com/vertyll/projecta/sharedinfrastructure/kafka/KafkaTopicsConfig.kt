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

    // Helper methods to retrieve topics

    // Auth/User service topics
    fun getUserRegisteredTopic(): String = publish["user-registered"] ?: subscribe["user-registered"] ?: "user-registered"

    fun getUserActivatedTopic(): String = publish["user-activated"] ?: subscribe["user-activated"] ?: "user-activated"

    fun getCredentialsVerificationTopic(): String =
        publish["credentials-verification"] ?: subscribe["credentials-verification"] ?: "credentials-verification"

    fun getCredentialsVerificationResultTopic(): String =
        publish["credentials-verification-result"] ?: subscribe["credentials-verification-result"] ?: "credentials-verification-result"

    fun getUserUpdatedTopic(): String = publish["user-updated"] ?: subscribe["user-updated"] ?: "user-updated"

    fun getUserEmailUpdatedTopic(): String = publish["user-email-updated"] ?: subscribe["user-email-updated"] ?: "user-email-updated"

    fun getUserDeletedTopic(): String = publish["user-deleted"] ?: subscribe["user-deleted"] ?: "user-deleted"

    fun getUserDeletionTopic(): String = publish["user-deletion"] ?: subscribe["user-deletion"] ?: "user-deletion"

    fun getUserCreationConfirmedTopic(): String =
        publish["user-creation-confirmed"] ?: subscribe["user-creation-confirmed"] ?: "user-creation-confirmed"

    // Role service topics
    fun getRoleCreatedTopic(): String = publish["role-created"] ?: subscribe["role-created"] ?: "role-created"

    fun getRoleUpdatedTopic(): String = publish["role-updated"] ?: subscribe["role-updated"] ?: "role-updated"

    fun getRoleDeletedTopic(): String = publish["role-deleted"] ?: subscribe["role-deleted"] ?: "role-deleted"

    fun getRoleAssignedTopic(): String = publish["role-assigned"] ?: subscribe["role-assigned"] ?: "role-assigned"

    fun getRoleRevokedTopic(): String = publish["role-revoked"] ?: subscribe["role-revoked"] ?: "role-revoked"

    // Mail service topics
    fun getMailRequestedTopic(): String = publish["mail-requested"] ?: subscribe["mail-requested"] ?: "mail-requested"

    fun getMailSentTopic(): String = publish["mail-sent"] ?: subscribe["mail-sent"] ?: "mail-sent"

    fun getMailFailedTopic(): String = publish["mail-failed"] ?: subscribe["mail-failed"] ?: "mail-failed"

    // Saga topics
    fun getSagaCompensationTopic(): String = publish["saga-compensation"] ?: subscribe["saga-compensation"] ?: "saga-compensation"
}
