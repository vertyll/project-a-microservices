package com.vertyll.veds.project.infrastructure.web.controller

import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectMembershipCommandUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectMembershipQueryUseCase
import com.vertyll.veds.project.infrastructure.web.LanguageHeader
import com.vertyll.veds.project.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.http.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/project-user-roles")
@Tag(name = "Project members", description = "Legacy membership route kept for the Angular client")
internal class ProjectUserRoleController(
    private val membershipServiceCommands: ProjectMembershipCommandUseCase,
    private val membershipServiceQueries: ProjectMembershipQueryUseCase,
) {
    private companion object {
        private const val MEMBERS_RETRIEVED = "Project members retrieved successfully"
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List members of a project (legacy path)")
    fun getMembers(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable projectId: UUID,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> {
        val members =
            membershipServiceQueries.getMembers(
                projectId = projectId,
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
            )
        return ApiResponse.buildResponse(members, MEMBERS_RETRIEVED, HttpStatus.OK)
    }
}
