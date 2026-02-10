package com.vertyll.projecta.role.application.controller

import com.vertyll.projecta.role.domain.dto.RoleCreateDto
import com.vertyll.projecta.role.domain.dto.RoleResponseDto
import com.vertyll.projecta.role.domain.dto.RoleUpdateDto
import com.vertyll.projecta.role.domain.service.RoleService
import com.vertyll.projecta.role.infrastructure.response.ApiResponse
import com.vertyll.projecta.sharedinfrastructure.http.ETagUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Role management APIs")
class RoleController(
    private val roleService: RoleService,
) {
    private companion object {
        private const val ROLE_CREATED_SUCCESSFULLY = "Role created successfully"
        private const val ROLE_UPDATED_SUCCESSFULLY = "Role updated successfully"
        private const val ROLE_RETRIEVED_SUCCESSFULLY = "Role retrieved successfully"
        private const val USER_ROLES_RETRIEVED_SUCCESSFULLY = "User roles retrieved successfully"
        private const val ROLE_ASSIGNED_SUCCESSFULLY = "Role assigned successfully"
        private const val ROLE_REMOVED_SUCCESSFULLY = "Role removed successfully"
        private const val ROLE_USER_RETRIEVED_SUCCESSFULLY = "Role users retrieved successfully"
    }

    @PostMapping
    @Operation(summary = "Create a new role")
    fun createRole(
        @RequestBody @Valid
        dto: RoleCreateDto,
    ): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val createdRole = roleService.createRole(dto)
        return ApiResponse.buildResponse(
            data = createdRole,
            message = ROLE_CREATED_SUCCESSFULLY,
            status = HttpStatus.CREATED,
        )
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a role")
    fun updateRole(
        @PathVariable id: Long,
        @RequestBody @Valid
        dto: RoleUpdateDto,
        @RequestHeader(name = "If-Match", required = false)
        ifMatch: String?,
    ): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val headerVersion = ETagUtil.parseIfMatchToVersion(ifMatch) ?: return ApiResponse.buildResponse(
            data = null,
            message = "Missing If-Match header",
            status = HttpStatus.PRECONDITION_REQUIRED,
        )
        val updatedRole = roleService.updateRole(id, dto, headerVersion)
        val etag = ETagUtil.buildWeakETag(updatedRole.version)
        val response = ApiResponse.buildResponse(
            data = updatedRole,
            message = ROLE_UPDATED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    fun getRoleById(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val role = roleService.getRoleById(id)
        val etag = ETagUtil.buildWeakETag(role.version)
        val response = ApiResponse.buildResponse(
            data = role,
            message = ROLE_RETRIEVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get role by name")
    fun getRoleByName(
        @PathVariable name: String,
    ): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val role = roleService.getRoleByName(name)
        val etag = ETagUtil.buildWeakETag(role.version)
        val response = ApiResponse.buildResponse(
            data = role,
            message = ROLE_RETRIEVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @GetMapping
    @Operation(summary = "Get all roles")
    fun getAllRoles(): ResponseEntity<ApiResponse<List<RoleResponseDto>>> {
        val roles = roleService.getAllRoles()
        // Compute collection ETag based on versions of all roles
        val collectionVersion = roles.mapNotNull { it.version }.hashCode().toLong()
        val etag = ETagUtil.buildWeakETag(collectionVersion)
        
        val response = ApiResponse.buildResponse(
            data = roles,
            message = ROLE_RETRIEVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get roles for a user")
    fun getRolesForUser(
        @PathVariable userId: Long,
    ): ResponseEntity<ApiResponse<List<RoleResponseDto>>> {
        val roles = roleService.getRolesForUser(userId)
        // Compute collection ETag based on versions of all roles
        val collectionVersion = roles.mapNotNull { it.version }.hashCode().toLong()
        val etag = ETagUtil.buildWeakETag(collectionVersion)

        val response = ApiResponse.buildResponse(
            data = roles,
            message = USER_ROLES_RETRIEVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @PostMapping("/user/{userId}/role/{roleName}")
    @Operation(summary = "Assign a role to a user")
    fun assignRoleToUser(
        @PathVariable userId: Long,
        @PathVariable roleName: String,
    ): ResponseEntity<ApiResponse<Any>> {
        roleService.assignRoleToUser(userId, roleName)
        return ApiResponse.buildResponse(
            data = null,
            message = ROLE_ASSIGNED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @DeleteMapping("/user/{userId}/role/{roleName}")
    @Operation(summary = "Remove a role from a user")
    fun removeRoleFromUser(
        @PathVariable userId: Long,
        @PathVariable roleName: String,
    ): ResponseEntity<ApiResponse<Any>> {
        roleService.removeRoleFromUser(userId, roleName)
        return ApiResponse.buildResponse(
            data = null,
            message = ROLE_REMOVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }

    @GetMapping("/role/{roleId}/users")
    @Operation(summary = "Get users for a role")
    fun getUsersForRole(
        @PathVariable roleId: Long,
    ): ResponseEntity<ApiResponse<List<Long>>> {
        val users = roleService.getUsersForRole(roleId)
        return ApiResponse.buildResponse(
            data = users,
            message = ROLE_USER_RETRIEVED_SUCCESSFULLY,
            status = HttpStatus.OK,
        )
    }
}
