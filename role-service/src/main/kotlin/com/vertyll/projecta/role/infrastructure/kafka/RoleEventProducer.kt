package com.vertyll.projecta.role.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.event.EventType
import com.vertyll.projecta.common.kafka.KafkaTopicsConfig
import com.vertyll.projecta.role.domain.model.Role
import com.vertyll.projecta.role.domain.model.UserRole
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class RoleEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val kafkaTopicsConfig: KafkaTopicsConfig
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
        kafkaTemplate.send(
            kafkaTopicsConfig.getRoleCreatedTopic(),
            event.eventId,
            eventJson
        )
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
        kafkaTemplate.send(
            kafkaTopicsConfig.getRoleUpdatedTopic(),
            event.eventId,
            eventJson
        )
        logger.info("Sent role updated event for role: {}", role.name)
    }

    /**
     * Sends an event when a role is assigned to a user using a Map
     */
    fun sendRoleAssignedEvent(userRole: UserRole, roleName: String) {
        val eventMap = mapOf(
            "eventType" to EventType.ROLE_ASSIGNED.value,
            "userId" to userRole.userId,
            "roleId" to userRole.roleId,
            "roleName" to roleName,
            "timestamp" to System.currentTimeMillis()
        )

        val eventJson = objectMapper.writeValueAsString(eventMap)
        kafkaTemplate.send(
            kafkaTopicsConfig.getRoleAssignedTopic(),
            userRole.userId.toString(),
            eventJson
        )
        logger.info("Sent role assigned event: Role {} assigned to user {}", roleName, userRole.userId)
    }

    /**
     * Sends an event when a role is revoked from a user using a Map
     */
    fun sendRoleRevokedEvent(userId: Long, roleId: Long, roleName: String) {
        val eventMap = mapOf(
            "eventType" to EventType.ROLE_REVOKED.value,
            "userId" to userId,
            "roleId" to roleId,
            "roleName" to roleName,
            "timestamp" to System.currentTimeMillis()
        )

        val eventJson = objectMapper.writeValueAsString(eventMap)
        kafkaTemplate.send(
            kafkaTopicsConfig.getRoleRevokedTopic(),
            userId.toString(),
            eventJson
        )
        logger.info("Sent role revoked event: Role {} revoked from user {}", roleName, userId)
    }
}
