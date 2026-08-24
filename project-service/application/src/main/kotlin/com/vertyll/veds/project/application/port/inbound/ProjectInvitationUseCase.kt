package com.vertyll.veds.project.application.port.inbound

import com.vertyll.veds.project.application.command.InviteMemberCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.dto.ProjectInvitationResponse
import java.util.UUID

/**
 * Driving port for the project invitation flow.
 *
 * Inviting is the one write in this context that spans services (mail delivery
 * happens in mail-service), which is why it runs as a saga rather than a plain
 * transaction — see `ProjectInvitationService`.
 */
interface ProjectInvitationUseCase {
    fun invite(
        projectId: UUID,
        request: InviteMemberCommand,
        actorId: UUID,
    ): ProjectInvitationResponse

    fun acceptInvitation(
        invitationId: UUID,
        actor: Actor,
    ): ProjectInvitationResponse

    fun rejectInvitation(
        invitationId: UUID,
        actor: Actor,
    ): ProjectInvitationResponse

    fun getMyInvitations(actorEmail: String): List<ProjectInvitationResponse>

    fun expireOverdueInvitations(): Int
}
