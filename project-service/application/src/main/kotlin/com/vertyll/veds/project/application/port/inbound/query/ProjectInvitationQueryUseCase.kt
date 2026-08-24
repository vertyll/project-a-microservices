package com.vertyll.veds.project.application.port.inbound.query

import com.vertyll.veds.project.application.dto.ProjectInvitationResponse

interface ProjectInvitationQueryUseCase {
    fun getMyInvitations(actorEmail: String): List<ProjectInvitationResponse>
}
