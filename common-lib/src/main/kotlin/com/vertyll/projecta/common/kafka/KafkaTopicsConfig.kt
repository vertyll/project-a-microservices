package com.vertyll.projecta.common.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for Kafka topics.
 * This class holds the configuration properties for Kafka topics and type mappings.
 */
@Configuration
@ConfigurationProperties(prefix = "kafka")
class KafkaTopicsConfig {

    var topics: TopicsConfig = TopicsConfig()
    var typeMappings: TypeMappingsConfig = TypeMappingsConfig()

    /**
     * Class for holding topic configuration properties.
     */
    class TopicsConfig {
        var auth: Map<String, String> = emptyMap()
        var user: Map<String, String> = emptyMap()
        var role: Map<String, String> = emptyMap()
        var mail: Map<String, String> = emptyMap()
        var saga: Map<String, String> = emptyMap()

        /**
         * Helper method to get a specific topic by domain and topic name.
         */
        fun getTopic(domain: String, topicName: String): String? {
            return when (domain.lowercase()) {
                "auth" -> auth[topicName]
                "user" -> user[topicName]
                "role" -> role[topicName]
                "mail" -> mail[topicName]
                "saga" -> saga[topicName]
                else -> null
            }
        }
    }

    /**
     * Class for holding type mapping configuration properties.
     */
    class TypeMappingsConfig {
        var auth: List<String> = emptyList()
        var user: List<String> = emptyList()
        var role: List<String> = emptyList()
        var mail: List<String> = emptyList()

        /**
         * Helper method to get type mappings for a specific service.
         * This method returns a list of type mappings based on the service name.
         */
        fun getTypeMappings(serviceName: String): List<String> {
            return when (serviceName.lowercase()) {
                "auth" -> auth
                "user" -> user
                "role" -> role
                "mail" -> mail
                else -> emptyList()
            }
        }

        /**
         * Helper method to get type mappings as a comma-separated string.
         * This method returns a string of type mappings based on the service name.
         */
        fun getTypeMappingsAsString(serviceName: String): String {
            return getTypeMappings(serviceName).joinToString(",")
        }
    }

    // Helper methods to retrieve common topics

    // Auth service topics
    fun getUserRegisteredTopic(): String =
        topics.auth[KafkaTopicNames.USER_REGISTERED.value] ?: KafkaTopicNames.USER_REGISTERED.value
    fun getUserActivatedTopic(): String =
        topics.auth[KafkaTopicNames.USER_ACTIVATED.value] ?: KafkaTopicNames.USER_ACTIVATED.value
    fun getCredentialsVerificationTopic(): String =
        topics.auth[KafkaTopicNames.CREDENTIALS_VERIFICATION.value] ?: KafkaTopicNames.CREDENTIALS_VERIFICATION.value
    fun getCredentialsVerificationResultTopic(): String =
        topics.auth[KafkaTopicNames.CREDENTIALS_VERIFICATION_RESULT.value] ?: KafkaTopicNames.CREDENTIALS_VERIFICATION_RESULT.value

    // User service topics
    fun getUserUpdatedTopic(): String =
        topics.user[KafkaTopicNames.USER_UPDATED.value] ?: KafkaTopicNames.USER_UPDATED.value
    fun getUserEmailUpdatedTopic(): String =
        topics.user[KafkaTopicNames.USER_EMAIL_UPDATED.value] ?: KafkaTopicNames.USER_EMAIL_UPDATED.value
    fun getUserDeletedTopic(): String =
        topics.user[KafkaTopicNames.USER_DELETED.value] ?: KafkaTopicNames.USER_DELETED.value
    fun getUserCreationConfirmedTopic(): String =
        topics.user[KafkaTopicNames.USER_CREATION_CONFIRMED.value] ?: KafkaTopicNames.USER_CREATION_CONFIRMED.value

    // Role service topics
    fun getRoleCreatedTopic(): String =
        topics.role[KafkaTopicNames.ROLE_CREATED.value] ?: KafkaTopicNames.ROLE_CREATED.value
    fun getRoleUpdatedTopic(): String =
        topics.role[KafkaTopicNames.ROLE_UPDATED.value] ?: KafkaTopicNames.ROLE_UPDATED.value
    fun getRoleDeletedTopic(): String =
        topics.role[KafkaTopicNames.ROLE_DELETED.value] ?: KafkaTopicNames.ROLE_DELETED.value
    fun getRoleAssignedTopic(): String =
        topics.role[KafkaTopicNames.ROLE_ASSIGNED.value] ?: KafkaTopicNames.ROLE_ASSIGNED.value
    fun getRoleRevokedTopic(): String =
        topics.role[KafkaTopicNames.ROLE_REVOKED.value] ?: KafkaTopicNames.ROLE_REVOKED.value

    // Mail service topics
    fun getMailRequestedTopic(): String =
        topics.mail[KafkaTopicNames.MAIL_REQUESTED.value] ?: KafkaTopicNames.MAIL_REQUESTED.value
    fun getMailSentTopic(): String = topics.mail[KafkaTopicNames.MAIL_SENT.value] ?: KafkaTopicNames.MAIL_SENT.value
    fun getMailFailedTopic(): String =
        topics.mail[KafkaTopicNames.MAIL_FAILED.value] ?: KafkaTopicNames.MAIL_FAILED.value

    // Saga topics
    fun getSagaCompensationTopic(): String =
        topics.saga[KafkaTopicNames.SAGA_COMPENSATION.value] ?: KafkaTopicNames.SAGA_COMPENSATION.value
}
