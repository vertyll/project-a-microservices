package com.vertyll.projecta.common.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for Kafka topics.
 * This loads values from the kafka.topics section in the configuration files.
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
        
        // Helper method to retrieve a topic by its name within a domain
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
        
        // Helper method to get type mappings for a specific service
        fun getTypeMappings(serviceName: String): List<String> {
            return when (serviceName.lowercase()) {
                "auth" -> auth
                "user" -> user
                "role" -> role
                "mail" -> mail
                else -> emptyList()
            }
        }
        
        // Helper method to get type mappings as a string for configuration
        fun getTypeMappingsAsString(serviceName: String): String {
            return getTypeMappings(serviceName).joinToString(",")
        }
    }
    
    // Helper methods to retrieve common topics
    
    // Auth service topics
    fun getUserRegisteredTopic(): String = topics.auth["user-registered"] ?: "user-registered"
    fun getUserActivatedTopic(): String = topics.auth["user-activated"] ?: "user-activated"
    fun getCredentialsVerificationTopic(): String = topics.auth["credentials-verification"] ?: "credentials-verification"
    fun getCredentialsVerificationResultTopic(): String = topics.auth["credentials-verification-result"] ?: "credentials-verification-result"
    
    // User service topics
    fun getUserUpdatedTopic(): String = topics.user["user-updated"] ?: "user-updated"
    fun getUserEmailUpdatedTopic(): String = topics.user["user-email-updated"] ?: "user-email-updated"
    fun getUserDeletedTopic(): String = topics.user["user-deleted"] ?: "user-deleted"
    
    // Role service topics
    fun getRoleCreatedTopic(): String = topics.role["role-created"] ?: "role-created"
    fun getRoleUpdatedTopic(): String = topics.role["role-updated"] ?: "role-updated"
    fun getRoleDeletedTopic(): String = topics.role["role-deleted"] ?: "role-deleted"
    fun getRoleAssignedTopic(): String = topics.role["role-assigned"] ?: "role-assigned"
    fun getRoleRevokedTopic(): String = topics.role["role-revoked"] ?: "role-revoked"
    
    // Mail service topics
    fun getMailRequestedTopic(): String = topics.mail["mail-requested"] ?: "mail-requested"
    fun getMailSentTopic(): String = topics.mail["mail-sent"] ?: "mail-sent"
    fun getMailFailedTopic(): String = topics.mail["mail-failed"] ?: "mail-failed"
    
    // Saga topics
    fun getSagaCompensationTopic(): String = topics.saga["saga-compensation"] ?: "saga-compensation"
} 