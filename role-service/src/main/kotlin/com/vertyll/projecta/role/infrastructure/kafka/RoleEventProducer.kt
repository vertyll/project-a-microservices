package com.vertyll.projecta.role.infrastructure.kafka

import com.vertyll.projecta.role.domain.model.entity.Role
import com.vertyll.projecta.role.domain.model.entity.UserRole
import com.vertyll.projecta.sharedinfrastructure.event.role.RoleAssignedEvent
import com.vertyll.projecta.sharedinfrastructure.event.role.RoleCreatedEvent
import com.vertyll.projecta.sharedinfrastructure.event.role.RoleRevokedEvent
import com.vertyll.projecta.sharedinfrastructure.event.role.RoleUpdatedEvent
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaTopicsConfig
import com.vertyll.projecta.sharedinfrastructure.role.RoleType
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class RoleEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val kafkaTopicsConfig: KafkaTopicsConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Sends an event when a role is created using the structured event class
     */
    fun sendRoleCreatedEvent(role: Role) {
        val event =
            RoleCreatedEvent(
                roleId = role.id!!,
                name = role.name,
                description = role.description,
            )

        val eventJson = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(
            kafkaTopicsConfig.getRoleCreatedTopic(),
            event.eventId,
            eventJson,
        )
        logger.info("Sent role created event for role: {}", role.name)
    }

    /**
     * Sends an event when a role is updated using the structured event class
     */
    fun sendRoleUpdatedEvent(role: Role) {
        val event =
            RoleUpdatedEvent(
                roleId = role.id!!,
                name = role.name,
                description = role.description,
            )

        val eventJson = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(
            kafkaTopicsConfig.getRoleUpdatedTopic(),
            event.eventId,
            eventJson,
        )
        logger.info("Sent role updated event for role: {}", role.name)
    }

    /**
     * Sends an event when a role is assigned to a user using the structured event class
     */
    fun sendRoleAssignedEvent(
        userRole: UserRole,
        roleName: RoleType,
    ) {
        val event =
            RoleAssignedEvent(
                userId = userRole.userId,
                roleId = userRole.roleId,
                roleName = roleName,
            )

        val eventJson = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(
            kafkaTopicsConfig.getRoleAssignedTopic(),
            event.eventId,
            eventJson,
        )
        logger.info("Sent role assigned event: Role {} assigned to user {}", roleName, userRole.userId)
    }

    /**
     * Sends an event when a role is revoked from a user using the structured event class
     */
    fun sendRoleRevokedEvent(
        userId: Long,
        roleId: Long,
        roleName: RoleType,
    ) {
        val event =
            RoleRevokedEvent(
                userId = userId,
                roleId = roleId,
                roleName = roleName,
            )

        val eventJson = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(
            kafkaTopicsConfig.getRoleRevokedTopic(),
            event.eventId,
            eventJson,
        )
        logger.info("Sent role revoked event: Role {} revoked from user {}", roleName, userId)
    }
}
