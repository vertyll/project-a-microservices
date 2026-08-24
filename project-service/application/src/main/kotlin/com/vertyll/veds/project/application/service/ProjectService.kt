package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.command.CreateProjectCommand
import com.vertyll.veds.project.application.command.UpdateProjectCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.dto.PagedResponse
import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.application.dto.ProjectDetailsResponse
import com.vertyll.veds.project.application.dto.ProjectListItemResponse
import com.vertyll.veds.project.application.dto.ProjectResponse
import com.vertyll.veds.project.application.dto.ProjectSearchParams
import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.application.dto.ProjectTypeResponse
import com.vertyll.veds.project.application.exception.ApiException
import com.vertyll.veds.project.application.port.inbound.ProjectUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageCode
import com.vertyll.veds.project.domain.model.PageRequest
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.model.ProjectSearchCriteria
import com.vertyll.veds.project.domain.model.VersionGuard
import com.vertyll.veds.project.domain.repository.ProjectCategoryRepository
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.domain.repository.ProjectRepository
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.domain.repository.ProjectStatusRepository
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import java.util.UUID

/**
 * Project lifecycle use cases.
 *
 * Creation and archival publish integration events through the transactional
 * outbox, so the event and the state change share one transaction.
 */
@Suppress("LongParameterList")
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val memberRepository: ProjectMemberRepository,
    private val roleRepository: ProjectRoleRepository,
    private val typeRepository: ProjectTypeRepository,
    private val categoryRepository: ProjectCategoryRepository,
    private val statusRepository: ProjectStatusRepository,
    private val userDirectory: UserDirectoryRepository,
    private val memberViewAssembler: MemberViewAssembler,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
) : ProjectUseCase {
    /**
     * Creates the project and immediately enrolls its creator as manager.
     *
     * Both writes share a transaction: a project whose owner is not a member
     * would be invisible in every membership-driven listing.
     */
    override fun createProject(
        request: CreateProjectCommand,
        actor: Actor,
    ): ProjectResponse {
        request.typeId?.let {
            typeRepository.findById(it) ?: throw ApiException(ProjectError.TYPE_NOT_FOUND)
        }

        val project =
            projectRepository.save(
                Project.create(
                    name = request.name,
                    description = request.description,
                    isPublic = request.isPublic,
                    typeId = request.typeId,
                    ownerId = actor.id,
                    iconFileId = request.iconFileId,
                ),
            )

        val managerRole =
            roleRepository.findByCode(ProjectRoleCode.MANAGER)
                ?: throw ApiException(ProjectError.ROLE_NOT_CONFIGURED)

        // Same transaction as the membership: see ProjectInvitationService.
        userDirectory.save(actor.toUserRef())

        memberRepository.save(
            ProjectMember.create(
                projectId = project.id,
                userId = actor.id,
                roleId = managerRole.id,
            ),
        )

        eventPublisher.publishProjectCreated(project.id, project.name, actor.id)

        return ProjectResponse.from(project)
    }

    override fun updateProject(
        projectId: UUID,
        request: UpdateProjectCommand,
        actorId: UUID,
        version: Long?,
    ): ProjectResponse {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        VersionGuard.requireMatch(project.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        request.typeId?.let {
            typeRepository.findById(it) ?: throw ApiException(ProjectError.TYPE_NOT_FOUND)
        }

        val updated =
            projectRepository.save(
                project
                    .rename(request.name)
                    .describe(request.description)
                    .changeVisibility(request.isPublic)
                    .changeType(request.typeId)
                    .changeIcon(request.iconFileId),
            )

        eventPublisher.publishProjectUpdated(updated.id, updated.name)

        return ProjectResponse.from(updated)
    }

    override fun getProject(
        projectId: UUID,
        actorId: UUID,
    ): ProjectResponse {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)
        return ProjectResponse.from(project)
    }

    override fun getProjectDetails(
        projectId: UUID,
        actorId: UUID,
        language: LanguageCode,
    ): ProjectDetailsResponse {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)

        val members = memberRepository.findAllByProjectId(projectId)

        return ProjectDetailsResponse(
            project = ProjectResponse.from(project),
            type = project.typeId?.let { typeRepository.findById(it) }?.let { ProjectTypeResponse.from(it, language) },
            members = memberViewAssembler.assemble(members, language),
            categories =
                categoryRepository
                    .findAllByProjectId(projectId)
                    .map { ProjectCategoryResponse.from(it, language) },
            statuses =
                statusRepository
                    .findAllByProjectId(projectId)
                    .map { ProjectStatusResponse.from(it, language) },
            permissions = authorization.effectivePermissions(projectId, actorId),
            currentUserId = actorId,
        )
    }

    override fun searchProjects(
        params: ProjectSearchParams,
        actorId: UUID,
    ): PagedResponse<ProjectListItemResponse> {
        val criteria =
            ProjectSearchCriteria(
                requesterId = actorId,
                searchTerm = params.searchTerm?.takeIf { it.isNotBlank() },
                typeId = params.typeId,
                onlyActive = params.onlyActive,
                includePublic = params.includePublic,
                sortBy = params.sortBy,
                sortDescending = params.sortDescending,
            )

        val page = projectRepository.search(criteria, PageRequest(params.page, params.size))

        // One aggregate query for the whole page instead of a query per row.
        val memberCounts = memberRepository.countByProjectIds(page.content.map { it.id })

        return PagedResponse.from(page) { project ->
            val count =
                memberCounts[project.id]
                    ?: error("member count missing for project ${project.id}; counting query is inconsistent")
            ProjectListItemResponse.from(project, count)
        }
    }

    /**
     * Archives rather than deletes — see [Project.archive].
     */
    override fun archiveProject(
        projectId: UUID,
        actorId: UUID,
        version: Long?,
    ) {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.DELETE_PROJECT)

        VersionGuard.requireMatch(project.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        if (!project.isActive) return

        projectRepository.save(project.archive())
        eventPublisher.publishProjectArchived(project.id)
    }
}
