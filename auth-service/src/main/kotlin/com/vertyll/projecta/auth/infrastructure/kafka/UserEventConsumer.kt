package com.vertyll.projecta.auth.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.projecta.auth.domain.repository.AuthUserRepository
import com.vertyll.projecta.auth.domain.repository.AuthUserRoleRepository
import com.vertyll.projecta.common.event.EventSource
import com.vertyll.projecta.common.event.user.UserRegisteredEvent
import com.vertyll.projecta.common.kafka.KafkaTopicsConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserEventConsumer(
    private val objectMapper: ObjectMapper,
    private val authUserRepository: AuthUserRepository,
    private val authUserRoleRepository: AuthUserRoleRepository,
    @Suppress("unused") private val kafkaTopicsConfig: KafkaTopicsConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Listens for user creation events from User Service to update the userId in Auth Service
     */
    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getUserRegisteredTopic()}"])
    @Transactional
    fun consumeUserRegisteredEvent(record: ConsumerRecord<String, String>) {
        try {
            logger.info("Received user registration event with key: ${record.key()}")

            // Deserialize the message payload
            val event = objectMapper.readValue(
                record.value(),
                UserRegisteredEvent::class.java
            )
            logger.info("Deserialized event for user: ${event.email}")

            // Only process events from User Service (ignore our own events)
            if (event.userId > 0 && event.eventSource == EventSource.USER_SERVICE.value) {
                updateAuthUserWithUserId(event)
            } else {
                logger.debug("Ignoring event from Auth Service or with invalid userId")
            }
        } catch (e: Exception) {
            logger.error("Error processing user registration event: ${e.message}", e)
        }
    }

    /**
     * Listens for role assignment events
     */
    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getRoleAssignedTopic()}"])
    @Transactional
    fun consumeRoleAssignedEvent(record: ConsumerRecord<String, String>) {
        try {
            logger.info("Received role assigned event with key: ${record.key()}")

            // Parse the event as a map first
            val eventMap = objectMapper.readValue(
                record.value(),
                Map::class.java
            )
            val userId = (eventMap["userId"] as Number).toLong()
            val roleId = (eventMap["roleId"] as Number).toLong()
            val roleName = eventMap["roleName"] as String

            logger.info("Received role assignment: User ID $userId, Role $roleName ($roleId)")

            // Find the auth user by userId
            val authUser = authUserRepository.findByUserId(userId)

            if (authUser != null) {
                // Check if this role is already assigned
                if (authUserRoleRepository.existsByAuthUserIdAndRoleId(authUser.id!!, roleId)) {
                    logger.info("Role $roleName ($roleId) already assigned to auth user ${authUser.id}")
                    return
                }

                // Add the role to the user
                authUser.addRole(roleId, roleName)
                authUserRepository.save(authUser)

                logger.info("Successfully assigned role $roleName ($roleId) to auth user ${authUser.id}")
            } else {
                logger.warn("Auth user not found for userId $userId, can't assign role")
            }
        } catch (e: Exception) {
            logger.error("Error processing role assigned event: ${e.message}", e)
        }
    }

    /**
     * Listens for role revocation events
     */
    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getRoleRevokedTopic()}"])
    @Transactional
    fun consumeRoleRevokedEvent(record: ConsumerRecord<String, String>) {
        try {
            logger.info("Received role revoked event with key: ${record.key()}")

            // Parse the event as a map first
            val eventMap = objectMapper.readValue(
                record.value(),
                Map::class.java
            )
            val userId = (eventMap["userId"] as Number).toLong()
            val roleId = (eventMap["roleId"] as Number).toLong()
            val roleName = eventMap["roleName"] as String

            logger.info("Received role revocation: User ID $userId, Role $roleName ($roleId)")

            // Find the auth user by userId
            val authUser = authUserRepository.findByUserId(userId)

            if (authUser != null) {
                // Remove the role from the user
                authUser.removeRole(roleId)
                authUserRepository.save(authUser)

                logger.info("Successfully revoked role $roleName ($roleId) from auth user ${authUser.id}")
            } else {
                logger.warn("Auth user not found for userId $userId, can't revoke role")
            }
        } catch (e: Exception) {
            logger.error("Error processing role revoked event: ${e.message}", e)
        }
    }

    /**
     * Updates the auth_user table with the userId from User Service
     */
    @Transactional
    fun updateAuthUserWithUserId(event: UserRegisteredEvent) {
        logger.info("Updating auth user with userId: ${event.userId} for email: ${event.email}")

        val authUser = authUserRepository.findByEmail(event.email).orElse(null)
        if (authUser == null) {
            logger.error("Auth user not found for email: ${event.email}")
            return
        }

        if (authUser.userId != null) {
            logger.warn("Auth user already has userId: ${authUser.userId}. Not updating.")
            return
        }

        // Update the userId field
        authUser.userId = event.userId
        authUserRepository.save(authUser)
        logger.info("Successfully updated auth user with userId: ${event.userId}")
    }
}
