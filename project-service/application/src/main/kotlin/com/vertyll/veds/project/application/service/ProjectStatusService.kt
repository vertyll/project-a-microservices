package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.command.CreateStatusCommand
import com.vertyll.veds.project.application.command.UpdateStatusCommand
import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.application.exception.ApiException
import com.vertyll.veds.project.application.port.inbound.ProjectStatusUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageCode
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectStatus
import com.vertyll.veds.project.domain.model.VersionGuard
import com.vertyll.veds.project.domain.repository.ProjectStatusRepository
import java.util.UUID

/**
 * Manages the workflow statuses owned by a project.
 *
 * Mirrors [ProjectCategoryService]: same permission model, same read-model
 * notification to task-service via `project-status-changed`.
 */
class ProjectStatusService(
    private val statusRepository: ProjectStatusRepository,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
) : ProjectStatusUseCase {
    override fun getStatuses(
        projectId: UUID,
        actorId: UUID,
        language: LanguageCode,
    ): List<ProjectStatusResponse> {
        authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)
        return statusRepository
            .findAllByProjectId(projectId)
            .map { ProjectStatusResponse.from(it, language) }
    }

    override fun createStatus(
        projectId: UUID,
        request: CreateStatusCommand,
        actorId: UUID,
        language: LanguageCode,
    ): ProjectStatusResponse {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        val status =
            statusRepository.save(
                ProjectStatus.create(
                    projectId = projectId,
                    color = request.color,
                    translations = request.translations,
                ),
            )

        eventPublisher.publishStatusChanged(
            projectId = projectId,
            statusId = status.id,
            names = status.translations.associate { it.language.name to it.name },
            color = status.color,
            removed = false,
        )

        return ProjectStatusResponse.from(status, language)
    }

    override fun updateStatus(
        projectId: UUID,
        statusId: UUID,
        request: UpdateStatusCommand,
        actorId: UUID,
        language: LanguageCode,
        version: Long?,
    ): ProjectStatusResponse {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        val status = loadOwnedStatus(projectId, statusId)

        VersionGuard.requireMatch(status.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        val updated =
            statusRepository.save(
                status
                    .recolor(request.color)
                    .retranslate(request.translations)
                    .let { if (request.isActive) it.activate() else it.deactivate() },
            )

        eventPublisher.publishStatusChanged(
            projectId = projectId,
            statusId = updated.id,
            names = updated.translations.associate { it.language.name to it.name },
            color = updated.color,
            removed = !updated.isActive,
        )

        return ProjectStatusResponse.from(updated, language)
    }

    override fun deleteStatus(
        projectId: UUID,
        statusId: UUID,
        actorId: UUID,
    ) {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        val status = loadOwnedStatus(projectId, statusId)
        statusRepository.delete(status.id)

        eventPublisher.publishStatusChanged(
            projectId = projectId,
            statusId = status.id,
            names = status.translations.associate { it.language.name to it.name },
            color = status.color,
            removed = true,
        )
    }

    private fun loadOwnedStatus(
        projectId: UUID,
        statusId: UUID,
    ): ProjectStatus {
        val status =
            statusRepository.findById(statusId)
                ?: throw ApiException(ProjectError.STATUS_NOT_FOUND)
        if (status.projectId != projectId) {
            throw ApiException(ProjectError.STATUS_NOT_FOUND)
        }
        return status
    }
}
