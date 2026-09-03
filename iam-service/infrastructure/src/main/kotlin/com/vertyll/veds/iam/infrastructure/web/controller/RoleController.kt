package com.vertyll.veds.iam.infrastructure.web.controller

import com.vertyll.veds.iam.application.command.CreateRoleCommand
import com.vertyll.veds.iam.application.command.UpdateRoleCommand
import com.vertyll.veds.iam.application.dto.RoleResponse
import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.port.inbound.command.RoleCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.query.RoleQueryUseCase
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.iam.infrastructure.response.ApiResponse
import com.vertyll.veds.iam.infrastructure.web.dto.CreateRoleRequest
import com.vertyll.veds.iam.infrastructure.web.dto.UpdateRoleRequest
import com.vertyll.veds.shared.web.http.ETagUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Role management APIs")
internal class RoleController(
    private val roleServiceCommands: RoleCommandUseCase,
    private val roleServiceQueries: RoleQueryUseCase,
) {
    private companion object {
        private const val ROLE_RETRIEVED_SUCCESSFULLY = "Role retrieved successfully"
        private const val USER_ROLES_RETRIEVED_SUCCESSFULLY = "User roles retrieved successfully"
        private const val ROLE_ASSIGNED_SUCCESSFULLY = "Role assigned successfully"
        private const val ROLE_REMOVED_SUCCESSFULLY = "Role removed successfully"
        private const val ROLE_CREATED_SUCCESSFULLY = "Role created successfully"
        private const val ROLE_UPDATED_SUCCESSFULLY = "Role updated successfully"
        private const val ROLE_DELETED_SUCCESSFULLY = "Role deleted successfully"
    }

    @PreAuthorize("@authz.has('ROLES_VIEW')")
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    fun getRoleById(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<RoleResponse>> {
        val role = roleServiceQueries.getRoleById(id)
        val etag = ETagUtils.buildWeakETag(role.version)
        val response = ApiResponse.buildResponse(role, ROLE_RETRIEVED_SUCCESSFULLY, HttpStatus.OK)
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @PreAuthorize("@authz.has('ROLES_VIEW')")
    @GetMapping("/name/{name}")
    @Operation(summary = "Get role by name")
    fun getRoleByName(
        @PathVariable name: String,
    ): ResponseEntity<ApiResponse<RoleResponse>> {
        val role = roleServiceQueries.getRoleByName(name)
        val etag = ETagUtils.buildWeakETag(role.version)
        val response = ApiResponse.buildResponse(role, ROLE_RETRIEVED_SUCCESSFULLY, HttpStatus.OK)
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @PreAuthorize("@authz.has('ROLES_VIEW')")
    @GetMapping
    @Operation(summary = "Get all roles, optionally only those held in one scope")
    fun getAllRoles(
        @RequestParam(required = false) scope: String?,
    ): ResponseEntity<ApiResponse<List<RoleResponse>>> {
        val roles =
            when (scope) {
                null -> roleServiceQueries.getAllRoles()
                else ->
                    roleServiceQueries.getRolesInScope(
                        RoleScope.fromString(scope.uppercase()) ?: throw ApiException(IamError.ROLE_NOT_FOUND),
                    )
            }
        return ApiResponse.buildResponse(roles, ROLE_RETRIEVED_SUCCESSFULLY, HttpStatus.OK)
    }

    @PreAuthorize("@authz.has('ROLES_MANAGE')")
    @PostMapping
    @Operation(summary = "Create a role")
    fun createRole(
        @Valid @RequestBody request: CreateRoleRequest,
    ): ResponseEntity<ApiResponse<RoleResponse>> {
        val role =
            roleServiceCommands.createRole(
                CreateRoleCommand(
                    name = request.name,
                    description = request.description,
                    permissions = request.permissions,
                    scope = RoleScope.fromString(request.scope) ?: throw ApiException(IamError.ROLE_NOT_FOUND),
                ),
            )
        return ApiResponse.buildResponse(role, ROLE_CREATED_SUCCESSFULLY, HttpStatus.CREATED)
    }

    @PreAuthorize("@authz.has('ROLES_MANAGE')")
    @PutMapping("/name/{name}")
    @Operation(summary = "Change what a role grants")
    fun updateRole(
        @PathVariable name: String,
        @Valid @RequestBody request: UpdateRoleRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ApiResponse<RoleResponse>> {
        val version = ETagUtils.parseIfMatchToVersion(ifMatch)
        val role =
            roleServiceCommands.updateRole(
                name = name,
                command = UpdateRoleCommand(description = request.description, permissions = request.permissions),
                version = version,
            )
        return ApiResponse.buildResponse(role, ROLE_UPDATED_SUCCESSFULLY, HttpStatus.OK)
    }

    @PreAuthorize("@authz.has('ROLES_MANAGE')")
    @DeleteMapping("/name/{name}")
    @Operation(summary = "Delete a role")
    fun deleteRole(
        @PathVariable name: String,
    ): ResponseEntity<ApiResponse<Any>> {
        roleServiceCommands.deleteRole(name)
        return ApiResponse.buildResponse(null, ROLE_DELETED_SUCCESSFULLY, HttpStatus.OK)
    }

    @PreAuthorize("@authz.has('USERS_VIEW')")
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get roles for a user")
    fun getRolesForUser(
        @PathVariable userId: Long,
    ): ResponseEntity<ApiResponse<List<RoleResponse>>> {
        val roles = roleServiceQueries.getRolesForUser(userId)
        return ApiResponse.buildResponse(roles, USER_ROLES_RETRIEVED_SUCCESSFULLY, HttpStatus.OK)
    }

    @PreAuthorize("@authz.has('USERS_MANAGE')")
    @PostMapping("/user/{userId}/role/{roleName}")
    @Operation(summary = "Assign a role to a user")
    fun assignRoleToUser(
        @PathVariable userId: Long,
        @PathVariable roleName: String,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ApiResponse<Any>> {
        val version = ETagUtils.parseIfMatchToVersion(ifMatch)
        roleServiceCommands.assignRoleToUser(userId, roleName, version)
        return ApiResponse.buildResponse(null, ROLE_ASSIGNED_SUCCESSFULLY, HttpStatus.OK)
    }

    @PreAuthorize("@authz.has('USERS_MANAGE')")
    @DeleteMapping("/user/{userId}/role/{roleName}")
    @Operation(summary = "Remove a role from a user")
    fun removeRoleFromUser(
        @PathVariable userId: Long,
        @PathVariable roleName: String,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ApiResponse<Any>> {
        val version = ETagUtils.parseIfMatchToVersion(ifMatch)
        roleServiceCommands.removeRoleFromUser(userId, roleName, version)
        return ApiResponse.buildResponse(null, ROLE_REMOVED_SUCCESSFULLY, HttpStatus.OK)
    }
}
