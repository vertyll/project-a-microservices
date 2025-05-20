package com.vertyll.projecta.auth.application.controller

import com.vertyll.projecta.auth.domain.dto.AuthRequestDto
import com.vertyll.projecta.auth.domain.dto.AuthResponseDto
import com.vertyll.projecta.auth.domain.dto.ChangeEmailRequestDto
import com.vertyll.projecta.auth.domain.dto.ChangePasswordRequestDto
import com.vertyll.projecta.auth.domain.dto.RegisterRequestDto
import com.vertyll.projecta.auth.domain.dto.ResetPasswordRequestDto
import com.vertyll.projecta.auth.domain.dto.SessionDto
import com.vertyll.projecta.auth.domain.service.AuthService
import com.vertyll.projecta.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
class AuthController(private val authService: AuthService) {
    @PostMapping("/register")
    @Operation(summary = "Register new user")
    fun register(
            @RequestBody @Valid request: RegisterRequestDto
    ): ResponseEntity<ApiResponse<Any>> {
        authService.register(request)
        return ApiResponse.buildResponse(null, "User registered successfully", HttpStatus.OK)
    }

    @PostMapping("/activate")
    @Operation(summary = "Activate user account with activation code")
    fun activateAccount(@RequestParam token: String): ResponseEntity<ApiResponse<Any>> {
        authService.activateAccount(token)
        return ApiResponse.buildResponse(null, "Account activated successfully", HttpStatus.OK)
    }

    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate user and get token")
    fun authenticate(
            @RequestBody @Valid request: AuthRequestDto,
            response: HttpServletResponse,
            httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<AuthResponseDto>> {
        // Extract User-Agent header and add it to the request
        val userAgent = httpRequest.getHeader("User-Agent")
        val requestWithUserAgent = request.copy(userAgent = userAgent)
        
        val authResponse = authService.authenticate(requestWithUserAgent, response)
        return ApiResponse.buildResponse(authResponse, "Authentication successful", HttpStatus.OK)
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token using refresh token cookie")
    fun refreshToken(
            request: HttpServletRequest,
            response: HttpServletResponse
    ): ResponseEntity<ApiResponse<AuthResponseDto>> {
        val authResponse = authService.refreshToken(request, response)
        return ApiResponse.buildResponse(
                authResponse,
                "Token refreshed successfully",
                HttpStatus.OK
        )
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout from current session")
    fun logout(
            request: HttpServletRequest,
            response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Any>> {
        authService.logout(request, response)
        return ApiResponse.buildResponse(null, "Logged out successfully", HttpStatus.OK)
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user details")
    fun getCurrentUser(
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        if (userDetails == null) {
            return ApiResponse.buildResponse(emptyMap(), "Not authenticated", HttpStatus.UNAUTHORIZED)
        }
        
        val userInfo = mapOf(
            "username" to userDetails.username,
            "authorities" to userDetails.authorities.map { it.authority },
            "accountNonExpired" to userDetails.isAccountNonExpired,
            "accountNonLocked" to userDetails.isAccountNonLocked,
            "credentialsNonExpired" to userDetails.isCredentialsNonExpired,
            "enabled" to userDetails.isEnabled
        )
        
        return ApiResponse.buildResponse(userInfo, "User details retrieved successfully", HttpStatus.OK)
    }

    @PostMapping("/reset-password-request")
    @Operation(summary = "Request password reset for a forgotten password")
    fun requestPasswordReset(@RequestParam email: String): ResponseEntity<ApiResponse<Any>> {
        authService.sendPasswordResetRequest(email)
        return ApiResponse.buildResponse(
                null,
                "Password reset instructions sent to email",
                HttpStatus.OK
        )
    }

    @PostMapping("/confirm-reset-password")
    @Operation(summary = "Reset password using reset token")
    fun resetPassword(
            @RequestParam token: String,
            @RequestBody @Valid request: ResetPasswordRequestDto
    ): ResponseEntity<ApiResponse<Any>> {
        authService.resetPassword(token, request)
        return ApiResponse.buildResponse(null, "Password reset successfully", HttpStatus.OK)
    }

    @PostMapping("/change-email-request")
    @Operation(summary = "Request email change")
    fun requestEmailChange(
            @RequestParam email: String,
            @RequestBody @Valid request: ChangeEmailRequestDto
    ): ResponseEntity<ApiResponse<Any>> {
        authService.requestEmailChange(email, request)
        return ApiResponse.buildResponse(
                null,
                "Email change instructions sent to email",
                HttpStatus.OK
        )
    }

    @PostMapping("/confirm-email-change")
    @Operation(summary = "Confirm email change using token")
    fun confirmEmailChange(@RequestParam token: String): ResponseEntity<ApiResponse<Any>> {
        authService.confirmEmailChange(token)
        return ApiResponse.buildResponse(null, "Email changed successfully", HttpStatus.OK)
    }

    @PostMapping("/change-password-request")
    @Operation(summary = "Request to change password")
    fun changePassword(
            @RequestParam email: String,
            @RequestBody @Valid request: ChangePasswordRequestDto
    ): ResponseEntity<ApiResponse<Any>> {
        authService.changePassword(email, request)
        return ApiResponse.buildResponse(
                null,
                "Password change confirmation sent to email",
                HttpStatus.OK
        )
    }

    @PostMapping("/confirm-password-change")
    @Operation(summary = "Confirm password change using token")
    fun confirmPasswordChange(
        @RequestParam token: String
    ): ResponseEntity<ApiResponse<Any>> {
        authService.confirmPasswordChange(token)
        return ApiResponse.buildResponse(null, "Password change verification successful. Check your email for further instructions.", HttpStatus.OK)
    }

    @PostMapping("/set-new-password")
    @Operation(summary = "Set new password after token verification (second step of password change)")
    fun setNewPassword(
        @RequestParam tokenId: Long,
        @RequestBody @Valid request: ResetPasswordRequestDto
    ): ResponseEntity<ApiResponse<Any>> {
        authService.setNewPassword(tokenId, request.newPassword)
        return ApiResponse.buildResponse(null, "Password changed successfully", HttpStatus.OK)
    }

    @PostMapping("/resend-activation")
    @Operation(summary = "Resend activation email")
    fun resendActivationEmail(@RequestParam email: String): ResponseEntity<ApiResponse<Any>> {
        authService.resendActivationEmail(email)
        return ApiResponse.buildResponse(
            null,
            "Activation email sent. Please check your inbox.",
            HttpStatus.OK
        )
    }
    
    @GetMapping("/sessions")
    @Operation(summary = "Get all active sessions for the current user")
    fun getSessions(
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<ApiResponse<List<SessionDto>>> {
        if (userDetails == null) {
            return ApiResponse.buildResponse(emptyList(), "Not authenticated", HttpStatus.UNAUTHORIZED)
        }
        
        val sessions = authService.getActiveSessions(userDetails.username)
        return ApiResponse.buildResponse(sessions, "Active sessions retrieved successfully", HttpStatus.OK)
    }
    
    @PostMapping("/sessions/{sessionId}/revoke")
    @Operation(summary = "Revoke a specific session")
    fun revokeSession(
        @PathVariable sessionId: Long,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<ApiResponse<Any>> {
        if (userDetails == null) {
            return ApiResponse.buildResponse(null, "Not authenticated", HttpStatus.UNAUTHORIZED)
        }
        
        val success = authService.revokeSession(sessionId, userDetails.username)
        return if (success) {
            ApiResponse.buildResponse(null, "Session revoked successfully", HttpStatus.OK)
        } else {
            ApiResponse.buildResponse(null, "Failed to revoke session", HttpStatus.BAD_REQUEST)
        }
    }
}
