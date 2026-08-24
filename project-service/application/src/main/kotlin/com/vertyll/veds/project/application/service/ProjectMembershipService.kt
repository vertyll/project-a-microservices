package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.command.UpdateMemberRoleCommand
import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.application.exception.ApiException
import com.vertyll.veds.project.application.port.inbound.ProjectMembershipUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageCode
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.VersionGuard
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import java.util.UUID

/**
 * Manages who belongs to a project and in which role.
 *
 * Membership changes are published so that task-service can maintain the
 * membership read model it uses to authorize task access without calling back
 * into this service on every request.
 */
class ProjectMembershipService(
    private val memberRepository: ProjectMemberRepository,
    private val roleRepository: ProjectRoleRepository,
    private val memberViewAssembler: MemberViewAssembler,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
) : ProjectMembershipUseCase {
    override fun getMembers(
        projectId: UUID,
        actorId: UUID,
        language: LanguageCode,
    ): List<ProjectMemberResponse> {
        authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)
        return memberViewAssembler.assemble(memberRepository.findAllByProjectId(projectId), language)
    }

    override fun updateMemberRole(
        projectId: UUID,
        memberId: UUID,
        request: UpdateMemberRoleCommand,
        actorId: UUID,
        language: LanguageCode,
        version: Long?,
    ): ProjectMemberResponse {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.MANAGE_MEMBERS)

        val member = loadOwnedMember(projectId, memberId)

        // The owner always holds every permission (see ProjectAccessPolicy), so
        // changing their role would be a silent no-op that looks like it worked.
        if (project.isOwnedBy(member.userId)) {
            throw ApiException(ProjectError.MEMBER_OWNER_IMMUTABLE)
        }

        VersionGuard.requireMatch(member.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        val role =
            roleRepository.findById(request.roleId)
                ?: throw ApiException(ProjectError.ROLE_NOT_FOUND)

        val updated = memberRepository.save(member.reassignTo(role.id))

        eventPublisher.publishMemberJoined(
            projectId = projectId,
            memberId = updated.id,
            userId = updated.userId,
            roleCode = role.code.name,
        )

        return memberViewAssembler.assemble(listOf(updated), language).single()
    }

    override fun removeMember(
        projectId: UUID,
        memberId: UUID,
        actorId: UUID,
    ) {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.MANAGE_MEMBERS)

        val member = loadOwnedMember(projectId, memberId)

        if (project.isOwnedBy(member.userId)) {
            throw ApiException(ProjectError.MEMBER_OWNER_IMMUTABLE)
        }

        memberRepository.delete(member.id)
        eventPublisher.publishMemberRemoved(projectId, member.userId)
    }

    override fun getEffectivePermissions(
        projectId: UUID,
        actorId: UUID,
    ): Set<ProjectPermission> = authorization.effectivePermissions(projectId, actorId)

    private fun loadOwnedMember(
        projectId: UUID,
        memberId: UUID,
    ): ProjectMember {
        val member =
            memberRepository.findById(memberId)
                ?: throw ApiException(ProjectError.MEMBER_NOT_FOUND)
        if (member.projectId != projectId) {
            throw ApiException(ProjectError.MEMBER_NOT_FOUND)
        }
        return member
    }
}
