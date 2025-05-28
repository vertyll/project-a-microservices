package com.vertyll.projecta.user.application.controller

import com.vertyll.projecta.common.response.ApiResponse
import com.vertyll.projecta.user.domain.dto.EmailUpdateDto
import com.vertyll.projecta.user.domain.dto.UserCreateDto
import com.vertyll.projecta.user.domain.dto.UserResponseDto
import com.vertyll.projecta.user.domain.dto.UserUpdateDto
import com.vertyll.projecta.user.domain.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
@Tag(name = "Users", description = "User management API")
class UserController(
    private val userService: UserService
) {
    @PostMapping
    @Operation(summary = "Create a new user")
    fun createUser(
        @RequestBody @Valid
        request: UserCreateDto
    ): ResponseEntity<ApiResponse<UserResponseDto>> {
        val user = userService.createUser(request)
        return ApiResponse.buildResponse(
            data = user,
            message = "User created successfully",
            status = HttpStatus.CREATED
        )
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody @Valid
        request: UserUpdateDto
    ): ResponseEntity<ApiResponse<UserResponseDto>> {
        val user = userService.updateUser(id, request)
        return ApiResponse.buildResponse(
            data = user,
            message = "User updated successfully",
            status = HttpStatus.OK
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    fun getUserById(@PathVariable id: Long): ResponseEntity<ApiResponse<UserResponseDto>> {
        val user = userService.getUserById(id)
        return ApiResponse.buildResponse(
            data = user,
            message = "User retrieved successfully",
            status = HttpStatus.OK
        )
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<ApiResponse<UserResponseDto>> {
        val user = userService.getUserByEmail(email)
        return ApiResponse.buildResponse(
            data = user,
            message = "User retrieved successfully",
            status = HttpStatus.OK
        )
    }

    @PostMapping("/email")
    @Operation(summary = "Update user email")
    fun updateEmail(
        @RequestParam @Valid
        request: EmailUpdateDto
    ): ResponseEntity<ApiResponse<UserResponseDto>> {
        val user = userService.updateEmail(request)
        return ApiResponse.buildResponse(
            data = user,
            message = "Email updated successfully",
            status = HttpStatus.OK
        )
    }
}
