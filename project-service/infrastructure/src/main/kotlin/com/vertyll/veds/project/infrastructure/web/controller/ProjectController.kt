package com.vertyll.veds.project.infrastructure.web.controller

import com.vertyll.veds.project.application.dto.PagedResponse
import com.vertyll.veds.project.application.dto.ProjectDetailsResponse
import com.vertyll.veds.project.application.dto.ProjectListItemResponse
import com.vertyll.veds.project.application.dto.ProjectResponse
import com.vertyll.veds.project.application.dto.ProjectSearchParams
import com.vertyll.veds.project.application.port.inbound.command.ProjectCommandUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectQueryUseCase
import com.vertyll.veds.project.domain.model.ProjectSortField
import com.vertyll.veds.project.infrastructure.response.ApiResponse
import com.vertyll.veds.project.infrastructure.web.LanguageHeader
import com.vertyll.veds.project.infrastructure.web.dto.CreateProjectRequest
import com.vertyll.veds.project.infrastructure.web.dto.UpdateProjectRequest
import com.vertyll.veds.project.infrastructure.web.security.CurrentUser
import com.vertyll.veds.sharedinfrastructure.utils.ETagUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/projects")
@Tag(name = "Projects", description = "Project management APIs")
internal class ProjectController(
    private val projectServiceCommands: ProjectCommandUseCase,
    private val projectServiceQueries: ProjectQueryUseCase,
) {
    private companion object {
        private const val PROJECT_CREATED = "Project created successfully"
        private const val PROJECT_UPDATED = "Project updated successfully"
        private const val PROJECT_RETRIEVED = "Project retrieved successfully"
        private const val PROJECTS_RETRIEVED = "Projects retrieved successfully"
        private const val PROJECT_ARCHIVED = "Project archived successfully"
        private const val DEFAULT_PAGE_SIZE = "20"
    }

    @GetMapping
    @Operation(summary = "List projects visible to the current user")
    @Suppress("LongParameterList")
    fun getProjects(
        @AuthenticationPrincipal jwt: Jwt?,
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) typeId: UUID?,
        @RequestParam(defaultValue = "true") onlyActive: Boolean,
        @RequestParam(defaultValue = "true") includePublic: Boolean,
        @RequestParam(defaultValue = "CREATED_AT") sortBy: ProjectSortField,
        @RequestParam(defaultValue = "true") sortDescending: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) size: Int,
    ): ResponseEntity<ApiResponse<PagedResponse<ProjectListItemResponse>>> {
        val actorId = CurrentUser.idOf(jwt)
        val params =
            ProjectSearchParams(
                searchTerm = searchTerm,
                typeId = typeId,
                onlyActive = onlyActive,
                includePublic = includePublic,
                sortBy = sortBy,
                sortDescending = sortDescending,
                page = page,
                size = size,
            )
        val projects = projectServiceQueries.searchProjects(params, actorId)
        return ApiResponse.buildResponse(projects, PROJECTS_RETRIEVED, HttpStatus.OK)
    }

    @PostMapping
    @Operation(summary = "Create a new project")
    fun createProject(
        @AuthenticationPrincipal jwt: Jwt?,
        @Valid @RequestBody
        request: CreateProjectRequest,
    ): ResponseEntity<ApiResponse<ProjectResponse>> {
        val project = projectServiceCommands.createProject(request.toCommand(), CurrentUser.identityOf(jwt).toActor())
        return ApiResponse.buildResponse(project, PROJECT_CREATED, HttpStatus.CREATED)
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get a project by id")
    fun getProject(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable projectId: UUID,
    ): ResponseEntity<ApiResponse<ProjectResponse>> {
        val project = projectServiceQueries.getProject(projectId, CurrentUser.idOf(jwt))
        return withETag(project, project.version, PROJECT_RETRIEVED)
    }

    @GetMapping("/{projectId}/details")
    @Operation(summary = "Get a project with members, categories, statuses and the caller's permissions")
    fun getProjectDetails(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable projectId: UUID,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<ApiResponse<ProjectDetailsResponse>> {
        val details =
            projectServiceQueries.getProjectDetails(
                projectId = projectId,
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
            )
        return ApiResponse.buildResponse(details, PROJECT_RETRIEVED, HttpStatus.OK)
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update a project")
    fun updateProject(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable projectId: UUID,
        @Valid @RequestBody
        request: UpdateProjectRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ApiResponse<ProjectResponse>> {
        val project =
            projectServiceCommands.updateProject(
                projectId = projectId,
                command = request.toCommand(),
                actorId = CurrentUser.idOf(jwt),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return withETag(project, project.version, PROJECT_UPDATED)
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Archive a project")
    fun archiveProject(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable projectId: UUID,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ApiResponse<Any>> {
        projectServiceCommands.archiveProject(
            projectId = projectId,
            actorId = CurrentUser.idOf(jwt),
            version = ETagUtils.parseIfMatchToVersion(ifMatch),
        )
        return ApiResponse.buildResponse(null, PROJECT_ARCHIVED, HttpStatus.OK)
    }

    private fun <T> withETag(
        body: T,
        version: Long?,
        message: String,
    ): ResponseEntity<ApiResponse<T>> {
        val response = ApiResponse.buildResponse(body, message, HttpStatus.OK)
        val etag = ETagUtils.buildWeakETag(version) ?: return response
        return ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body)
    }
}
