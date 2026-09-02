package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.application.command.CreateProjectCommand
import com.vertyll.veds.project.application.command.InviteMemberCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.port.inbound.MailFeedbackUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectCommandUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectInvitationCommandUseCase
import com.vertyll.veds.project.application.port.outbound.SagaProcessPort
import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import com.vertyll.veds.project.infrastructure.IntegrationTestBase
import com.vertyll.veds.project.infrastructure.kafka.ProjectKafkaTopics
import com.vertyll.veds.project.infrastructure.persistence.repository.OutboxJpaRepository
import com.vertyll.veds.shared.saga.SagaStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectInvitationSagaIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var projectCommands: ProjectCommandUseCase

    @Autowired
    private lateinit var invitationCommands: ProjectInvitationCommandUseCase

    @Autowired
    private lateinit var invitationRepository: ProjectInvitationRepository

    @Autowired
    private lateinit var outboxRepository: OutboxJpaRepository

    @Autowired
    private lateinit var mailFeedback: MailFeedbackUseCase

    @Autowired
    private lateinit var sagaProcess: SagaProcessPort

    @BeforeEach
    fun clearOutbox() {
        outboxRepository.deleteAll()
    }

    private fun owner() =
        Actor(
            id = UUID.randomUUID(),
            email = "owner@example.com",
            firstName = "Ada",
            lastName = "Lovelace",
        )

    private fun projectFor(actor: Actor) =
        projectCommands.createProject(
            CreateProjectCommand(
                name = "Apollo",
                description = null,
                isPublic = false,
                typeId = null,
                iconFileId = null,
            ),
            actor,
        )

    @Test
    fun `inviting writes the invitation and its event in one transaction`() {
        val actor = owner()
        val project = projectFor(actor)

        val invitation =
            invitationCommands.invite(
                project.id,
                InviteMemberCommand(email = "invitee@example.com", roleId = null),
                actor.id,
            )

        val stored = invitationRepository.findById(invitation.id)
        assertNotNull(stored)
        assertEquals(InvitationStatus.PENDING, stored.status)

        val queued = outboxRepository.findAll().filter { it.topic == ProjectKafkaTopics.PROJECT_MEMBER_INVITED }
        assertTrue(queued.isNotEmpty(), "the invitation event must be in the outbox, not sent directly to Kafka")
    }

    @Test
    fun `a failed delivery expires the invitation through compensation`() {
        val actor = owner()
        val project = projectFor(actor)

        val invitation =
            invitationCommands.invite(
                project.id,
                InviteMemberCommand(email = "bounces@example.com", roleId = null),
                actor.id,
            )

        val sagaId = openSagaId()

        mailFeedback.handleMailFailed(
            sagaId = sagaId,
            to = "bounces@example.com",
            error = "550 mailbox unavailable",
        )

        val saga = sagaProcess.findSagaDomainById(sagaId)
        assertNotNull(saga)
        assertTrue(
            saga.status in setOf(SagaStatus.COMPENSATING, SagaStatus.COMPENSATED, SagaStatus.FAILED),
            "a bounced invitation must not leave the saga open, was ${saga.status}",
        )
    }

    @Test
    fun `a delivered invitation closes the saga so the watchdog cannot revoke it`() {
        val actor = owner()
        val project = projectFor(actor)

        val invitation =
            invitationCommands.invite(
                project.id,
                InviteMemberCommand(email = "delivered@example.com", roleId = null),
                actor.id,
            )
        val sagaId = openSagaId()

        mailFeedback.handleMailSent(sagaId, "delivered@example.com")

        val saga = sagaProcess.findSagaDomainById(sagaId)
        assertNotNull(saga)
        assertEquals(
            SagaStatus.COMPLETED,
            saga.status,
            "a saga left open after delivery is timed out by the watchdog, which revokes the invitation",
        )

        val stored = invitationRepository.findById(invitation.id)
        assertNotNull(stored)
        assertEquals(InvitationStatus.PENDING, stored.status)
    }

    @Test
    fun `replaying the same mail feedback is a no-op`() {
        val actor = owner()
        val project = projectFor(actor)

        val invitation =
            invitationCommands.invite(
                project.id,
                InviteMemberCommand(email = "duplicate@example.com", roleId = null),
                actor.id,
            )
        val sagaId = openSagaId()

        mailFeedback.handleMailSent(sagaId, "duplicate@example.com")
        val afterFirst = sagaProcess.findSagaDomainById(sagaId)?.status

        mailFeedback.handleMailSent(sagaId, "duplicate@example.com")
        val afterSecond = sagaProcess.findSagaDomainById(sagaId)?.status

        assertEquals(afterFirst, afterSecond, "at-least-once delivery must not move a settled saga")

        val stored = invitationRepository.findById(invitation.id)
        assertNotNull(stored)
        assertEquals(InvitationStatus.PENDING, stored.status)
    }

    private fun openSagaId(): String {
        val message =
            outboxRepository
                .findAll()
                .firstOrNull { it.topic == ProjectKafkaTopics.PROJECT_MEMBER_INVITED && it.sagaId != null }
        assertNotNull(message, "no outbox message carries a sagaId")
        return message.sagaId!!
    }
}
