package com.vertyll.veds.project.infrastructure.web.controller

import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectMembershipCommandUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectMembershipQueryUseCase
import com.vertyll.veds.project.infrastructure.response.ApiResponse
import com.vertyll.veds.project.infrastructure.web.LanguageHeader
import com.vertyll.veds.project.infrastructure.web.dto.UpdateMemberRoleRequest
import com.vertyll.veds.project.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.http.ETagUtils
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/projects/{projectId}/users")
@Tag(name = "Project members", description = "Membership management within a project")
internal class ProjectMemberController(
    private val membershipServiceCommands: ProjectMembershipCommandUseCase,
    private val membershipServiceQueries: ProjectMembershipQueryUseCase,
) {
    private companion object {
        private const val MEMBERS_RETRIEVED = "Project members retrieved successfully"
        private const val MEMBER_UPDATED = "Project member updated successfully"
        private const val MEMBER_REMOVED = "Project member removed successfully"
        private const val PERMISSIONS_RETRIEVED = "Permissions retrieved successfully"
    }

    @GetMapping
    @Operation(summary = "List members of a project")
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

    @GetMapping("/me/permissions")
    @Operation(summary = "Get the current user's effective permissions in a project")
    fun getMyPermissions(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable projectId: UUID,
    ): ResponseEntity<ApiResponse<Set<String>>> {
        val permissions = membershipServiceQueries.getEffectivePermissions(projectId, CurrentUser.idOf(jwt))
        return ApiResponse.buildResponse(permissions, PERMISSIONS_RETRIEVED, HttpStatus.OK)
    }

    @PutMapping("/{memberId}")
    @Operation(summary = "Change a member's project role")
    @Suppress("LongParameterList")
    fun updateMemberRole(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable projectId: UUID,
        @PathVariable memberId: UUID,
        @Valid @RequestBody
        request: UpdateMemberRoleRequest,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ApiResponse<ProjectMemberResponse>> {
        val member =
            membershipServiceCommands.updateMemberRole(
                projectId = projectId,
                memberId = memberId,
                command = request.toCommand(),
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return ApiResponse.buildResponse(member, MEMBER_UPDATED, HttpStatus.OK)
    }

    @DeleteMapping("/{memberId}")
    @Operation(summary = "Remove a member from a project")
    fun removeMember(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable projectId: UUID,
        @PathVariable memberId: UUID,
    ): ResponseEntity<ApiResponse<Any>> {
        membershipServiceCommands.removeMember(projectId, memberId, CurrentUser.idOf(jwt))
        return ApiResponse.buildResponse(null, MEMBER_REMOVED, HttpStatus.OK)
    }
}
