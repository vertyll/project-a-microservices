package com.vertyll.veds.iam.infrastructure.web.controller

import com.vertyll.veds.iam.application.dto.PermissionModuleResponse
import com.vertyll.veds.iam.application.dto.PermissionResponse
import com.vertyll.veds.iam.application.port.inbound.query.PermissionQueryUseCase
import com.vertyll.veds.shared.web.http.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/permissions")
@Tag(name = "Permissions", description = "Platform permission catalogue")
@PreAuthorize("@authz.has('ROLES_VIEW')")
internal class PermissionController(
    private val permissionQueries: PermissionQueryUseCase,
) {
    private companion object {
        private const val PERMISSIONS_RETRIEVED = "iam.permissions_retrieved"
    }

    @GetMapping
    @Operation(summary = "List every permission with the roles that grant it")
    fun getPermissions(): ResponseEntity<ApiResponse<List<PermissionResponse>>> =
        ApiResponse.buildResponse(permissionQueries.getAllPermissions(), PERMISSIONS_RETRIEVED, HttpStatus.OK)

    @GetMapping("/modules")
    @Operation(summary = "List every permission grouped by the module that declares it")
    fun getPermissionsByModule(): ResponseEntity<ApiResponse<List<PermissionModuleResponse>>> =
        ApiResponse.buildResponse(permissionQueries.getPermissionsByModule(), PERMISSIONS_RETRIEVED, HttpStatus.OK)
}
