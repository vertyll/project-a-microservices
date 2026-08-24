package com.vertyll.veds.project.application.port.inbound

import com.vertyll.veds.project.application.command.CreateProjectCommand
import com.vertyll.veds.project.application.command.UpdateProjectCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.dto.PagedResponse
import com.vertyll.veds.project.application.dto.ProjectDetailsResponse
import com.vertyll.veds.project.application.dto.ProjectListItemResponse
import com.vertyll.veds.project.application.dto.ProjectResponse
import com.vertyll.veds.project.application.dto.ProjectSearchParams
import com.vertyll.veds.project.domain.model.LanguageCode
import java.util.UUID

/**
 * Driving port for project lifecycle operations.
 *
 * Every method takes the acting user explicitly instead of reading a security
 * context: the application layer must remain callable from a test, a Kafka
 * consumer or a scheduler, none of which have an HTTP request attached.
 */
interface ProjectUseCase {
    fun createProject(
        request: CreateProjectCommand,
        actor: Actor,
    ): ProjectResponse

    fun updateProject(
        projectId: UUID,
        request: UpdateProjectCommand,
        actorId: UUID,
        version: Long? = null,
    ): ProjectResponse

    fun getProject(
        projectId: UUID,
        actorId: UUID,
    ): ProjectResponse

    fun getProjectDetails(
        projectId: UUID,
        actorId: UUID,
        language: LanguageCode,
    ): ProjectDetailsResponse

    fun searchProjects(
        params: ProjectSearchParams,
        actorId: UUID,
    ): PagedResponse<ProjectListItemResponse>

    fun archiveProject(
        projectId: UUID,
        actorId: UUID,
        version: Long? = null,
    )
}
