package com.vertyll.veds.iam.infrastructure.web.controller

import com.vertyll.veds.iam.application.dto.SecuritySettingsResponse
import com.vertyll.veds.iam.application.port.inbound.command.SecurityCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.query.SecurityQueryUseCase
import com.vertyll.veds.iam.infrastructure.response.ApiResponse
import com.vertyll.veds.iam.infrastructure.web.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/me/security")
@Tag(name = "Account security", description = "Second-factor status")
internal class SecurityController(
    private val securityQueries: SecurityQueryUseCase,
    private val securityCommands: SecurityCommandUseCase,
) {
    private companion object {
        private const val SETTINGS_RETRIEVED = "iam.security.settings_retrieved"
        private const val TWO_FACTOR_DISABLED = "iam.security.two_factor_disabled"
    }

    @GetMapping
    @Operation(summary = "Get the caller's second-factor status")
    fun getSettings(
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<ApiResponse<SecuritySettingsResponse>> {
        val settings = securityQueries.getSecuritySettings(CurrentUser.keycloakIdOf(jwt))
        return ApiResponse.buildResponse(settings, SETTINGS_RETRIEVED, HttpStatus.OK)
    }

    @DeleteMapping("/two-factor")
    @Operation(summary = "Turn the caller's second factor off")
    fun disableTwoFactor(
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<ApiResponse<SecuritySettingsResponse>> {
        val settings = securityCommands.disableTwoFactor(CurrentUser.keycloakIdOf(jwt))
        return ApiResponse.buildResponse(settings, TWO_FACTOR_DISABLED, HttpStatus.OK)
    }
}
