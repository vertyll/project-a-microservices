@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.InMemoryInvitationRepository
import com.vertyll.veds.project.application.InMemoryProjectRepository
import com.vertyll.veds.project.application.SilentLogger
import com.vertyll.veds.project.application.project
import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand
import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.model.ProjectInvitation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectCompensationServiceTest {
    private val invitations = InMemoryInvitationRepository()
    private val projects = InMemoryProjectRepository()

    private val service = ProjectCompensationService(invitations, projects, SilentLogger)

    // ── Revoking an invitation ──────────────────────────────────────────

    @Test
    fun `a pending invitation is expired`() {
        val invitation =
            ProjectInvitation(
                projectId = Uuid.generateV7().toJavaUuid(),
                inviteeEmail = "a@example.com",
                inviterId = Uuid.generateV7().toJavaUuid(),
                roleId = Uuid.generateV7().toJavaUuid(),
            ).also { invitations.given(it) }

        service.compensate(ProjectCompensationCommand.RevokeInvitation(invitation.id.toString(), "mail failed"))

        assertEquals(InvitationStatus.EXPIRED, invitations.findById(invitation.id)!!.status)
    }

    @Test
    fun `an invitation that is no longer pending is left as it is`() {
        val accepted =
            ProjectInvitation(
                projectId = Uuid.generateV7().toJavaUuid(),
                inviteeEmail = "a@example.com",
                inviterId = Uuid.generateV7().toJavaUuid(),
                roleId = Uuid.generateV7().toJavaUuid(),
            ).accept(Uuid.generateV7().toJavaUuid())
                .also { invitations.given(it) }

        service.compensate(ProjectCompensationCommand.RevokeInvitation(accepted.id.toString(), "mail failed"))

        assertEquals(InvitationStatus.ACCEPTED, invitations.findById(accepted.id)!!.status)
    }

    @Test
    fun `revoking twice is harmless`() {
        val invitation =
            ProjectInvitation(
                projectId = Uuid.generateV7().toJavaUuid(),
                inviteeEmail = "a@example.com",
                inviterId = Uuid.generateV7().toJavaUuid(),
                roleId = Uuid.generateV7().toJavaUuid(),
            ).also { invitations.given(it) }
        val command = ProjectCompensationCommand.RevokeInvitation(invitation.id.toString(), "mail failed")

        service.compensate(command)
        service.compensate(command)

        assertEquals(InvitationStatus.EXPIRED, invitations.findById(invitation.id)!!.status)
    }

    @Test
    fun `an invitation that no longer exists is not an error`() {
        service.compensate(ProjectCompensationCommand.RevokeInvitation(Uuid.generateV7().toString(), "mail failed"))

        assertTrue(invitations.stored.isEmpty())
    }

    // ── Restoring a project ─────────────────────────────────────────────

    @Test
    fun `an archived project is brought back`() {
        val archived = project(isActive = false).also { projects.given(it) }

        service.compensate(ProjectCompensationCommand.RestoreProject(archived.id.toString(), "downstream refused"))

        assertTrue(projects.findById(archived.id)!!.isActive)
    }

    @Test
    fun `an active project is left alone`() {
        val active = project(isActive = true).also { projects.given(it) }

        service.compensate(ProjectCompensationCommand.RestoreProject(active.id.toString(), "downstream refused"))

        assertTrue(projects.findById(active.id)!!.isActive)
    }

    @Test
    fun `restoring twice is harmless`() {
        val archived = project(isActive = false).also { projects.given(it) }
        val command = ProjectCompensationCommand.RestoreProject(archived.id.toString(), "downstream refused")

        service.compensate(command)
        service.compensate(command)

        assertTrue(projects.findById(archived.id)!!.isActive)
    }

    @Test
    fun `a project that no longer exists is not an error`() {
        service.compensate(ProjectCompensationCommand.RestoreProject(Uuid.generateV7().toString(), "downstream refused"))

        assertTrue(projects.stored.isEmpty())
    }
}
