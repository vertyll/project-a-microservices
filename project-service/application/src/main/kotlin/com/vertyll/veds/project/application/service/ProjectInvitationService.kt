package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.command.InviteMemberCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.dto.ProjectInvitationResponse
import com.vertyll.veds.project.application.exception.ApiException
import com.vertyll.veds.project.application.port.inbound.ProjectInvitationUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.application.port.outbound.SagaProcessPort
import com.vertyll.veds.project.application.port.outbound.UseCaseLogger
import com.vertyll.veds.project.application.saga.model.SagaStepNames
import com.vertyll.veds.project.application.saga.model.SagaTypes
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.ProjectInvitation
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.domain.repository.ProjectRepository
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import java.time.Instant
import java.util.UUID

/**
 * The invitation flow — the one write in this context that crosses a service
 * boundary, and therefore the one that runs as a saga.
 *
 * Steps: persist the invitation, then ask mail-service to deliver it. If mail
 * delivery ultimately fails, mail-service publishes `mail-failed`, the
 * compensation arrives on `saga-compensation-project`, and
 * [ProjectCompensationService] expires the invitation. Without that, a user
 * would hold a pending invitation they were never notified about.
 */
@Suppress("LongParameterList")
class ProjectInvitationService(
    private val invitationRepository: ProjectInvitationRepository,
    private val memberRepository: ProjectMemberRepository,
    private val roleRepository: ProjectRoleRepository,
    private val projectRepository: ProjectRepository,
    private val userDirectory: UserDirectoryRepository,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
    private val sagaProcess: SagaProcessPort,
    private val logger: UseCaseLogger,
) : ProjectInvitationUseCase {
    private companion object {
    }

    override fun invite(
        projectId: UUID,
        request: InviteMemberCommand,
        actorId: UUID,
    ): ProjectInvitationResponse {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.INVITE_USERS)

        invitationRepository.findPendingByProjectIdAndEmail(projectId, request.email)?.let {
            throw ApiException(ProjectError.INVITATION_ALREADY_SENT)
        }

        val role =
            request.roleId
                ?.let { roleRepository.findById(it) ?: throw ApiException(ProjectError.ROLE_NOT_FOUND) }
                ?: roleRepository.findByCode(ProjectRoleCode.MEMBER)
                ?: throw ApiException(ProjectError.ROLE_NOT_CONFIGURED)

        val sagaId =
            sagaProcess
                .startSaga(
                    sagaType = SagaTypes.PROJECT_INVITATION,
                    payload = mapOf("projectId" to projectId.toString(), "email" to request.email),
                ).id

        return try {
            val invitation =
                invitationRepository.save(
                    ProjectInvitation.create(
                        projectId = projectId,
                        inviteeEmail = request.email,
                        inviterId = actorId,
                        roleId = role.id,
                    ),
                )

            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PERSIST_INVITATION,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("invitationId" to invitation.id.toString()),
            )

            eventPublisher.publishMemberInvited(
                projectId = projectId,
                projectName = project.name,
                invitationId = invitation.id,
                inviteeEmail = invitation.inviteeEmail,
                inviterId = actorId,
                sagaId = sagaId,
            )

            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.REQUEST_INVITATION_MAIL,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("invitationId" to invitation.id.toString()),
            )

            // The saga stays open until mail-service reports delivery success or
            // failure; the watchdog closes it if no feedback ever arrives.
            sagaProcess.markAwaitingResponse(sagaId)

            ProjectInvitationResponse.from(invitation, project.name)
        } catch (e: ApiException) {
            failSaga(sagaId, e.error.key)
            throw e
        }
    }

    override fun acceptInvitation(
        invitationId: UUID,
        actor: Actor,
    ): ProjectInvitationResponse {
        val invitation = loadPendingInvitationFor(invitationId, actor.email)

        if (memberRepository.findByProjectIdAndUserId(invitation.projectId, actor.id) != null) {
            throw ApiException(ProjectError.MEMBER_ALREADY_JOINED)
        }

        val accepted = invitationRepository.save(invitation.accept(actor.id))

        // Write the user projection in the same transaction as the membership.
        // The member list can then require it, instead of rendering blank rows
        // while waiting for an IAM event that may be minutes away or lost.
        userDirectory.save(actor.toUserRef())

        val member =
            memberRepository.save(
                ProjectMember.create(
                    projectId = invitation.projectId,
                    userId = actor.id,
                    roleId = invitation.roleId,
                ),
            )

        // The role was validated when the invitation was created; its absence
        // now means reference data was deleted underneath us, which downstream
        // consumers must not silently receive as "MEMBER".
        val role =
            roleRepository.findById(invitation.roleId)
                ?: throw ApiException(
                    ProjectError.ROLE_NOT_FOUND,
                    mapOf("roleId" to invitation.roleId.toString()),
                )

        eventPublisher.publishMemberJoined(
            projectId = invitation.projectId,
            memberId = member.id,
            userId = actor.id,
            roleCode = role.code.name,
        )

        val projectName = projectRepository.findById(invitation.projectId)?.name
        return ProjectInvitationResponse.from(accepted, projectName)
    }

    override fun rejectInvitation(
        invitationId: UUID,
        actor: Actor,
    ): ProjectInvitationResponse {
        val invitation = loadPendingInvitationFor(invitationId, actor.email)
        val rejected = invitationRepository.save(invitation.reject(actor.id))
        val projectName = projectRepository.findById(invitation.projectId)?.name
        return ProjectInvitationResponse.from(rejected, projectName)
    }

    override fun getMyInvitations(actorEmail: String): List<ProjectInvitationResponse> {
        val invitations = invitationRepository.findAllByInviteeEmail(actorEmail).filter { it.isPending }
        val projects = projectRepository.findAllByIds(invitations.map { it.projectId }).associateBy { it.id }
        return invitations.map { ProjectInvitationResponse.from(it, projects[it.projectId]?.name) }
    }

    /**
     * Invoked by a scheduled job; kept on the use-case port so the expiry rule
     * lives with the rest of the invitation logic rather than in a scheduler.
     */
    override fun expireOverdueInvitations(): Int {
        val now = Instant.now()
        val overdue = invitationRepository.findAllPendingExpiredBefore(now)
        overdue.forEach { invitationRepository.save(it.expire()) }
        if (overdue.isNotEmpty()) {
            logger.info("Expired {} overdue project invitations", overdue.size)
        }
        return overdue.size
    }

    private fun loadPendingInvitationFor(
        invitationId: UUID,
        actorEmail: String,
    ): ProjectInvitation {
        val invitation =
            invitationRepository.findById(invitationId)
                ?: throw ApiException(ProjectError.INVITATION_NOT_FOUND)

        if (!invitation.inviteeEmail.equals(actorEmail, ignoreCase = true)) {
            throw ApiException(ProjectError.INVITATION_NOT_ADDRESSED_TO_CALLER)
        }
        if (!invitation.isPending) {
            throw ApiException(ProjectError.INVITATION_NOT_PENDING)
        }
        if (invitation.hasExpiredAt(Instant.now())) {
            invitationRepository.save(invitation.expire())
            throw ApiException(ProjectError.INVITATION_EXPIRED)
        }
        return invitation
    }

    private fun failSaga(
        sagaId: String,
        message: String,
    ) {
        logger.error("Project invitation saga {} failed: {}", sagaId, message)
        sagaProcess.recordSagaStep(
            sagaId = sagaId,
            stepName = SagaStepNames.PERSIST_INVITATION,
            status = SagaStepStatus.FAILED,
            payload = mapOf("error" to message),
        )
        sagaProcess.markSagaFailed(sagaId, message)
    }
}
