package com.vertyll.projecta.role.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.event.DomainEvent
import com.vertyll.projecta.common.kafka.KafkaTopics
import com.vertyll.projecta.role.domain.model.Role
import com.vertyll.projecta.role.domain.model.UserRole
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RoleEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Sends an event when a role is created using the structured event class
     */
    fun sendRoleCreatedEvent(role: Role) {
        val event = RoleCreatedEvent(
            roleId = role.id!!,
            name = role.name,
            description = role.description
        )
        
        val eventJson = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(KafkaTopics.ROLE_CREATED, event.eventId, eventJson)
        logger.info("Sent role created event for role: {}", role.name)
    }
    
    /**
     * Sends an event when a role is updated using the structured event class
     */
    fun sendRoleUpdatedEvent(role: Role) {
        val event = RoleUpdatedEvent(
            roleId = role.id!!,
            name = role.name,
            description = role.description
        )
        
        val eventJson = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(KafkaTopics.ROLE_UPDATED, event.eventId, eventJson)
        logger.info("Sent role updated event for role: {}", role.name)
    }
    
    /**
     * Sends an event when a role is assigned to a user using a Map
     */
    fun sendRoleAssignedEvent(userRole: UserRole, roleName: String) {
        val eventMap = mapOf(
            "eventType" to "ROLE_ASSIGNED",
            "userId" to userRole.userId,
            "roleId" to userRole.roleId,
            "roleName" to roleName,
            "timestamp" to System.currentTimeMillis()
        )
        
        val eventJson = objectMapper.writeValueAsString(eventMap)
        kafkaTemplate.send(KafkaTopics.ROLE_ASSIGNED, userRole.userId.toString(), eventJson)
        logger.info("Sent role assigned event: Role {} assigned to user {}", roleName, userRole.userId)
    }
    
    /**
     * Sends an event when a role is revoked from a user using a Map
     */
    fun sendRoleRevokedEvent(userId: Long, roleId: Long, roleName: String) {
        val eventMap = mapOf(
            "eventType" to "ROLE_REVOKED",
            "userId" to userId,
            "roleId" to roleId,
            "roleName" to roleName,
            "timestamp" to System.currentTimeMillis()
        )
        
        val eventJson = objectMapper.writeValueAsString(eventMap)
        kafkaTemplate.send(KafkaTopics.ROLE_REVOKED, userId.toString(), eventJson)
        logger.info("Sent role revoked event: Role {} revoked from user {}", roleName, userId)
    }
}

/**
 * Event sent when a new role is created
 */
data class RoleCreatedEvent(
    override val eventId: String = DomainEvent.generateEventId(),
    override val timestamp: Instant = DomainEvent.now(),
    override val eventType: String = "ROLE_CREATED",
    val roleId: Long,
    val name: String,
    val description: String?,
    override val sagaId: String? = null
) : DomainEvent

/**
 * Event sent when a role is updated
 */
data class RoleUpdatedEvent(
    override val eventId: String = DomainEvent.generateEventId(),
    override val timestamp: Instant = DomainEvent.now(),
    override val eventType: String = "ROLE_UPDATED",
    val roleId: Long,
    val name: String,
    val description: String?,
    override val sagaId: String? = null
) : DomainEvent
