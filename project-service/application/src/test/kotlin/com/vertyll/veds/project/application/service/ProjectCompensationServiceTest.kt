package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.InMemoryInvitationRepository
import com.vertyll.veds.project.application.InMemoryProjectRepository
import com.vertyll.veds.project.application.SilentLogger
import com.vertyll.veds.project.application.project
import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand
import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.model.ProjectInvitation
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compensation runs after another service has already refused its part of the work, and it is
 * delivered at least once — so the same command can arrive twice, or after the row it names has
 * been dealt with by hand. Undoing has to be safe in all of those cases, because there is no
 * transaction left to roll back.
 */
internal class ProjectCompensationServiceTest {
    private val invitations = InMemoryInvitationRepository()
    private val projects = InMemoryProjectRepository()

    private val service = ProjectCompensationService(invitations, projects, SilentLogger)

    // ── Revoking an invitation ──────────────────────────────────────────

    /** The mail never went out, so the invitation must not stay open for someone to accept. */
    @Test
    fun `a pending invitation is expired`() {
        val invitation =
            ProjectInvitation(
                projectId = UUID.randomUUID(),
                inviteeEmail = "a@example.com",
                inviterId = UUID.randomUUID(),
                roleId = UUID.randomUUID(),
            ).also { invitations.given(it) }

        service.compensate(ProjectCompensationCommand.RevokeInvitation(invitation.id.toString(), "mail failed"))

        assertEquals(InvitationStatus.EXPIRED, invitations.findById(invitation.id)!!.status)
    }

    /**
     * An invitation the invitee already answered is not the compensation's to reverse — the person
     * acted on it, and expiring an accepted membership would revoke access nobody asked to revoke.
     */
    @Test
    fun `an invitation that is no longer pending is left as it is`() {
        val accepted =
            ProjectInvitation(
                projectId = UUID.randomUUID(),
                inviteeEmail = "a@example.com",
                inviterId = UUID.randomUUID(),
                roleId = UUID.randomUUID(),
            ).accept(UUID.randomUUID())
                .also { invitations.given(it) }

        service.compensate(ProjectCompensationCommand.RevokeInvitation(accepted.id.toString(), "mail failed"))

        assertEquals(InvitationStatus.ACCEPTED, invitations.findById(accepted.id)!!.status)
    }

    /** Delivery is at-least-once, so the second copy of the same command must be a no-op. */
    @Test
    fun `revoking twice is harmless`() {
        val invitation =
            ProjectInvitation(
                projectId = UUID.randomUUID(),
                inviteeEmail = "a@example.com",
                inviterId = UUID.randomUUID(),
                roleId = UUID.randomUUID(),
            ).also { invitations.given(it) }
        val command = ProjectCompensationCommand.RevokeInvitation(invitation.id.toString(), "mail failed")

        service.compensate(command)
        service.compensate(command)

        assertEquals(InvitationStatus.EXPIRED, invitations.findById(invitation.id)!!.status)
    }

    /**
     * The state this undo exists to reverse is already gone, which is the outcome that was wanted.
     * Failing here would put the saga into COMPENSATION_FAILED over nothing.
     */
    @Test
    fun `an invitation that no longer exists is not an error`() {
        service.compensate(ProjectCompensationCommand.RevokeInvitation(UUID.randomUUID().toString(), "mail failed"))

        assertTrue(invitations.stored.isEmpty())
    }

    // ── Restoring a project ─────────────────────────────────────────────

    @Test
    fun `an archived project is brought back`() {
        val archived = project(isActive = false).also { projects.given(it) }

        service.compensate(ProjectCompensationCommand.RestoreProject(archived.id.toString(), "downstream refused"))

        assertTrue(projects.findById(archived.id)!!.isActive)
    }

    /** A project somebody already restored must not be touched again. */
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
        service.compensate(ProjectCompensationCommand.RestoreProject(UUID.randomUUID().toString(), "downstream refused"))

        assertTrue(projects.stored.isEmpty())
    }
}
