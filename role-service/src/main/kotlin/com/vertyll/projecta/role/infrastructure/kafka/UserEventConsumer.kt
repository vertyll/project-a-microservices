package com.vertyll.projecta.role.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.common.event.user.UserRegisteredEvent
import com.vertyll.projecta.common.kafka.KafkaTopics
import com.vertyll.projecta.role.domain.service.RoleService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserEventConsumer(
    private val objectMapper: ObjectMapper,
    private val roleService: RoleService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaTopics.USER_REGISTERED])
    @Transactional
    fun consume(record: ConsumerRecord<String, String>) {
        try {
            logger.info("Received user registration event with key: ${record.key()}")

            // Deserialize the message payload
            val event = objectMapper.readValue(record.value(), UserRegisteredEvent::class.java)
            logger.info("Deserialized event for user: ${event.email} with roles: ${event.roles}")

            handleUserRegisteredEvent(event)
        } catch (e: Exception) {
            logger.error("Error processing user registration event: ${e.message}", e)
        }
    }

    private fun handleUserRegisteredEvent(event: UserRegisteredEvent) {
        logger.info("Processing user registration for roles: ${event.email} with roles: ${event.roles}")

        try {
            // Assign each role to the user
            // This assumes that the role exists - we should have a default 'USER' role created at startup
            event.roles.forEach { roleName ->
                try {
                    roleService.assignRoleToUser(event.userId, roleName)
                    logger.info("Assigned role $roleName to user ${event.userId}")
                } catch (e: Exception) {
                    logger.error("Failed to assign role $roleName to user ${event.userId}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process roles for user ${event.email}: ${e.message}", e)
        }
    }
}
