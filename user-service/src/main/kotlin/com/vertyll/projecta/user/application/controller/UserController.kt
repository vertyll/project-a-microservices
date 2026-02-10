package com.vertyll.projecta.user.application.controller

import com.vertyll.projecta.sharedinfrastructure.http.ETagUtil
import com.vertyll.projecta.user.domain.dto.UserResponseDto
import com.vertyll.projecta.user.domain.service.UserService
import com.vertyll.projecta.user.infrastructure.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management API")
class UserController(
    private val userService: UserService,
) {
    private companion object {
        private const val USER_RETRIEVED_SUCCESSFULLY = "User retrieved successfully"
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    fun getUserById(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<UserResponseDto>> {
        val user = userService.getUserById(id)
        val etag = ETagUtil.buildWeakETag(user.version)
        val response =
            ApiResponse.buildResponse(
                data = user,
                message = USER_RETRIEVED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email")
    fun getUserByEmail(
        @PathVariable email: String,
    ): ResponseEntity<ApiResponse<UserResponseDto>> {
        val user = userService.getUserByEmail(email)
        val etag = ETagUtil.buildWeakETag(user.version)
        val response =
            ApiResponse.buildResponse(
                data = user,
                message = USER_RETRIEVED_SUCCESSFULLY,
                status = HttpStatus.OK,
            )
        return if (etag != null) ResponseEntity.status(HttpStatus.OK).eTag(etag).body(response.body) else response
    }
}
