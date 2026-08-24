package com.vertyll.veds.project.application.port.inbound

import com.vertyll.veds.project.application.command.UpdateMemberRoleCommand
import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.domain.model.LanguageCode
import com.vertyll.veds.project.domain.model.ProjectPermission
import java.util.UUID

interface ProjectMembershipUseCase {
    fun getMembers(
        projectId: UUID,
        actorId: UUID,
        language: LanguageCode,
    ): List<ProjectMemberResponse>

    fun updateMemberRole(
        projectId: UUID,
        memberId: UUID,
        request: UpdateMemberRoleCommand,
        actorId: UUID,
        language: LanguageCode,
        version: Long? = null,
    ): ProjectMemberResponse

    fun removeMember(
        projectId: UUID,
        memberId: UUID,
        actorId: UUID,
    )

    fun getEffectivePermissions(
        projectId: UUID,
        actorId: UUID,
    ): Set<ProjectPermission>
}
