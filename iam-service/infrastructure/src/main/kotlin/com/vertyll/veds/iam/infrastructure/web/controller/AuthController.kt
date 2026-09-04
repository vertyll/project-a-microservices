package com.vertyll.veds.iam.infrastructure.web.controller

import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.application.port.inbound.command.AuthCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.command.ProvisionCurrentUserUseCase
import com.vertyll.veds.iam.application.port.inbound.query.AuthQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.UserQueryUseCase
import com.vertyll.veds.iam.infrastructure.web.dto.ChangeEmailRequest
import com.vertyll.veds.iam.infrastructure.web.dto.ChangePasswordRequest
import com.vertyll.veds.iam.infrastructure.web.dto.ConfirmPasswordChangeRequest
import com.vertyll.veds.iam.infrastructure.web.dto.RegisterRequest
import com.vertyll.veds.iam.infrastructure.web.dto.ResetPasswordRequest
import com.vertyll.veds.iam.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.http.ETagUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
internal class AuthController(
    private val authServiceCommands: AuthCommandUseCase,
    private val authServiceQueries: AuthQueryUseCase,
    private val provisionCurrentUser: ProvisionCurrentUserUseCase,
    private val userServiceQueries: UserQueryUseCase,
) {
    @PostMapping("/register")
    @Operation(summary = "Register new user")
    fun register(
        @RequestBody @Valid
        request: RegisterRequest,
    ): ResponseEntity<Any> {
        authServiceCommands.register(request.toCommand())
        return ResponseEntity.ok().build()
    }

    @PostMapping("/activate")
    @Operation(summary = "Activate user account with activation code")
    fun activateAccount(
        @RequestParam token: String,
    ): ResponseEntity<Any> {
        authServiceCommands.activateAccount(token)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/resend-activation")
    @Operation(summary = "Resend activation email")
    fun resendActivationEmail(
        @RequestParam email: String,
    ): ResponseEntity<Any> {
        authServiceCommands.resendActivationEmail(email)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/reset-password-request")
    @Operation(summary = "Request password reset for a forgotten password")
    fun requestPasswordReset(
        @RequestParam email: String,
    ): ResponseEntity<Any> {
        authServiceCommands.sendPasswordResetRequest(email)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/confirm-reset-password")
    @Operation(summary = "Reset password using reset token")
    fun resetPassword(
        @RequestParam token: String,
        @RequestBody @Valid
        request: ResetPasswordRequest,
    ): ResponseEntity<Any> {
        authServiceCommands.resetPassword(token, request.toCommand())
        return ResponseEntity.ok().build()
    }

    @PostMapping("/change-email-request")
    @Operation(summary = "Request email change")
    fun requestEmailChange(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody @Valid
        request: ChangeEmailRequest,
    ): ResponseEntity<Any> {
        val email = CurrentUser.emailOf(jwt)
        authServiceCommands.requestEmailChange(email, request.toCommand())
        return ResponseEntity.ok().build()
    }

    @PostMapping("/confirm-email-change")
    @Operation(summary = "Confirm email change using token")
    fun confirmEmailChange(
        @RequestParam token: String,
    ): ResponseEntity<Any> {
        authServiceCommands.confirmEmailChange(token)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/change-password-request")
    @Operation(summary = "Request to change password")
    fun changePassword(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody @Valid
        request: ChangePasswordRequest,
    ): ResponseEntity<Any> {
        val email = CurrentUser.emailOf(jwt)
        authServiceCommands.changePassword(email, request.toCommand())
        return ResponseEntity.ok().build()
    }

    @PostMapping("/confirm-password-change")
    @Operation(summary = "Confirm password change using token and new password")
    fun confirmPasswordChange(
        @RequestParam token: String,
        @RequestBody @Valid
        request: ConfirmPasswordChangeRequest,
    ): ResponseEntity<Any> {
        authServiceCommands.confirmPasswordChange(token, request.newPassword)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/set-new-password")
    @Operation(summary = "Set new password after token verification (second step of password change)")
    fun setNewPassword(
        @RequestParam tokenId: Long,
        @RequestBody @Valid
        request: ResetPasswordRequest,
    ): ResponseEntity<Any> {
        authServiceCommands.setNewPassword(tokenId, request.toCommand())
        return ResponseEntity.ok().build()
    }

    @GetMapping("/me")
    @Operation(summary = "Get the signed-in person's profile")
    fun getCurrentUser(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<UserResponse> {
        val keycloakId = CurrentUser.keycloakIdOf(jwt)
        provisionCurrentUser.provision(CurrentUser.identityOf(jwt))

        val user = userServiceQueries.getUserByKeycloakId(keycloakId)
        val etag = ETagUtils.buildWeakETag(user.version) ?: return ResponseEntity.ok(user)
        return ResponseEntity.ok().eTag(etag).body(user)
    }

    @GetMapping("/me/permissions")
    @Operation(summary = "Get current user permissions from local database")
    fun getCurrentUserPermissions(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<String>> {
        val keycloakId = CurrentUser.keycloakIdOf(jwt)
        val permissions = authServiceQueries.getUserPermissions(keycloakId)

        return ResponseEntity.ok(permissions)
    }
}
