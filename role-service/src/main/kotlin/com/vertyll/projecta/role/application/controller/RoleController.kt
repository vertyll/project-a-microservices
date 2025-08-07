package com.vertyll.projecta.role.application.controller

import com.vertyll.projecta.role.domain.dto.RoleCreateDto
import com.vertyll.projecta.role.domain.dto.RoleResponseDto
import com.vertyll.projecta.role.domain.dto.RoleUpdateDto
import com.vertyll.projecta.role.domain.service.RoleService
import com.vertyll.projecta.role.infrastructure.response.ApiResponse
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Role management APIs")
class RoleController(private val roleService: RoleService) {
    @PostMapping
    @Operation(summary = "Create a new role")
    fun createRole(
        @RequestBody @Valid
        dto: RoleCreateDto
    ): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val createdRole = roleService.createRole(dto)
        return ApiResponse.buildResponse(
            data = createdRole,
            message = "Role created successfully",
            status = HttpStatus.CREATED
        )
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a role")
    fun updateRole(
        @PathVariable id: Long,
        @RequestBody @Valid
        dto: RoleUpdateDto
    ): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val updatedRole = roleService.updateRole(id, dto)
        return ApiResponse.buildResponse(
            data = updatedRole,
            message = "Role updated successfully",
            status = HttpStatus.OK
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    fun getRoleById(@PathVariable id: Long): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val role = roleService.getRoleById(id)
        return ApiResponse.buildResponse(
            data = role,
            message = "Role retrieved successfully",
            status = HttpStatus.OK
        )
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get role by name")
    fun getRoleByName(@PathVariable name: String): ResponseEntity<ApiResponse<RoleResponseDto>> {
        val role = roleService.getRoleByName(name)
        return ApiResponse.buildResponse(
            data = role,
            message = "Role retrieved successfully",
            status = HttpStatus.OK
        )
    }

    @GetMapping
    @Operation(summary = "Get all roles")
    fun getAllRoles(): ResponseEntity<ApiResponse<List<RoleResponseDto>>> {
        val roles = roleService.getAllRoles()
        return ApiResponse.buildResponse(
            data = roles,
            message = "Roles retrieved successfully",
            status = HttpStatus.OK
        )
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get roles for a user")
    fun getRolesForUser(@PathVariable userId: Long): ResponseEntity<ApiResponse<List<RoleResponseDto>>> {
        val roles = roleService.getRolesForUser(userId)
        return ApiResponse.buildResponse(
            data = roles,
            message = "User roles retrieved successfully",
            status = HttpStatus.OK
        )
    }

    @PostMapping("/user/{userId}/role/{roleName}")
    @Operation(summary = "Assign a role to a user")
    fun assignRoleToUser(@PathVariable userId: Long, @PathVariable roleName: String): ResponseEntity<ApiResponse<Any>> {
        roleService.assignRoleToUser(userId, roleName)
        return ApiResponse.buildResponse(
            data = null,
            message = "Role assigned successfully",
            status = HttpStatus.OK
        )
    }

    @DeleteMapping("/user/{userId}/role/{roleName}")
    @Operation(summary = "Remove a role from a user")
    fun removeRoleFromUser(
        @PathVariable userId: Long,
        @PathVariable roleName: String
    ): ResponseEntity<ApiResponse<Any>> {
        roleService.removeRoleFromUser(userId, roleName)
        return ApiResponse.buildResponse(
            data = null,
            message = "Role removed successfully",
            status = HttpStatus.OK
        )
    }

    @GetMapping("/role/{roleId}/users")
    @Operation(summary = "Get users for a role")
    fun getUsersForRole(@PathVariable roleId: Long): ResponseEntity<ApiResponse<List<Long>>> {
        val users = roleService.getUsersForRole(roleId)
        return ApiResponse.buildResponse(
            data = users,
            message = "Role users retrieved successfully",
            status = HttpStatus.OK
        )
    }
}
