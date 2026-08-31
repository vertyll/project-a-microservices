package com.vertyll.veds.apigateway.controller

import com.vertyll.veds.apigateway.infrastructure.response.ApiResponse
import com.vertyll.veds.apigateway.security.AuthTransactionCookies
import com.vertyll.veds.apigateway.security.Pkce
import com.vertyll.veds.apigateway.session.KeycloakTokenClient
import com.vertyll.veds.apigateway.session.SessionCookies
import com.vertyll.veds.apigateway.session.SessionStore
import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI

/**
 * BFF (Backend-For-Frontend) controller that proxies authentication requests to Keycloak.
 *
 * Handles:
 * - POST /auth/token — login (username + password → access_token + refresh_token in cookie)
 * - POST /auth/refresh-token — refresh (reads refresh_token from cookie)
 * - POST /auth/logout — logout (invalidates refresh_token, clears cookie)
 */
@RestController
@RequestMapping("/auth")
internal class AuthProxyController(
    private val sharedConfig: SharedKeycloakProperties,
    private val authTransactionCookies: AuthTransactionCookies,
    private val sessionCookies: SessionCookies,
    private val sessionStore: SessionStore,
    private val keycloakTokenClient: KeycloakTokenClient,
) {
    private companion object {
        private val log = LoggerFactory.getLogger(AuthProxyController::class.java)

        private const val MSG_SESSION_ACTIVE = "auth.session_active"
        private const val MSG_NO_SESSION = "auth.no_session"
        private const val MSG_LOGOUT_SUCCESS = "auth.logout_successful"

        private const val ERROR_PARAM = "error"
        private const val ERR_STATE_MISMATCH = "state_mismatch"
        private const val ERR_MISSING_VERIFIER = "missing_code_verifier"
        private const val ERR_CODE_EXCHANGE_FAILED = "code_exchange_failed"

        private const val KC_ACTION_PARAM = "kc_action"

        private val ALLOWED_KC_ACTIONS = setOf("CONFIGURE_TOTP", "UPDATE_PASSWORD", "delete_credential")

        private const val SCOPE = "openid profile email"
    }

    data class SessionResponse(
        val userId: String,
        val email: String,
        val roles: List<String>,
    )

    @GetMapping("/authorize")
    fun authorize(
        exchange: ServerWebExchange,
        @RequestParam(name = "kc_action", required = false) kcAction: String?,
    ): ResponseEntity<Void> {
        val state = Pkce.newState()
        val codeVerifier = Pkce.newCodeVerifier()

        authTransactionCookies.issue(exchange, state = state, codeVerifier = codeVerifier)

        val authorizationUri =
            UriComponentsBuilder
                .fromUriString(keycloakAuthorizationUrl())
                .queryParam("client_id", sharedConfig.gatewayClientId)
                .queryParam("redirect_uri", sharedConfig.oauth.redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .queryParam("code_challenge", Pkce.challengeOf(codeVerifier))
                .queryParam("code_challenge_method", Pkce.CHALLENGE_METHOD)
                .apply { allowedAction(kcAction)?.let { queryParam(KC_ACTION_PARAM, it) } }
                .encode()
                .build()
                .toUriString()

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authorizationUri)).build()
    }

    // Application-Initiated Actions are relayed, not implemented. Configuring a
    // second factor happens on Keycloak's pages, so no TOTP secret ever reaches
    // this service, its logs or the browser's JavaScript.
    //
    // The value is checked against a fixed set rather than forwarded as given: an
    // open kc_action would let a caller push any user into any Keycloak flow,
    // including ones that change credentials.
    private fun allowedAction(kcAction: String?): String? = kcAction?.takeIf { it in ALLOWED_KC_ACTIONS }

    @GetMapping("/callback")
    fun callback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(name = ERROR_PARAM, required = false) error: String?,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<Void>> {
        val expectedState = authTransactionCookies.read(exchange, AuthTransactionCookies.STATE_COOKIE)
        val codeVerifier = authTransactionCookies.read(exchange, AuthTransactionCookies.VERIFIER_COOKIE)
        authTransactionCookies.clear(exchange)

        if (error != null) {
            log.debug("Keycloak returned an authorization error: {}", error)
            return Mono.just(redirectToApp(error))
        }

        val stateMatches = state != null && expectedState != null && state == expectedState
        if (code == null || !stateMatches) {
            log.warn("Rejecting callback: state does not match the value issued to this browser")
            return Mono.just(redirectToApp(ERR_STATE_MISMATCH))
        }

        if (codeVerifier == null) {
            log.warn("Rejecting callback: no code_verifier cookie present")
            return Mono.just(redirectToApp(ERR_MISSING_VERIFIER))
        }

        return keycloakTokenClient
            .exchangeAuthorizationCode(code, codeVerifier)
            .flatMap { session -> sessionStore.create(session) }
            .map { sessionId ->
                sessionCookies.issue(exchange, sessionId)
                redirectToApp(null)
            }.onErrorResume { ex ->
                log.warn("Authorization code exchange failed: {}", ex.message)
                Mono.just(redirectToApp(ERR_CODE_EXCHANGE_FAILED))
            }
    }

    @GetMapping("/session")
    fun session(exchange: ServerWebExchange): Mono<ResponseEntity<ApiResponse<SessionResponse>>> {
        val sessionId =
            sessionCookies.read(exchange)
                ?: return Mono.just(noSession())

        return sessionStore
            .find(sessionId)
            .map { session ->
                ApiResponse.buildResponse(
                    data =
                        SessionResponse(
                            userId = session.subject,
                            email = session.email,
                            roles = session.roles,
                        ),
                    message = MSG_SESSION_ACTIVE,
                    status = HttpStatus.OK,
                )
            }.switchIfEmpty(Mono.fromCallable { noSession() })
    }

    /**
     * Logout: invalidates the refresh_token in Keycloak and clears the cookie.
     */
    @Suppress("kotlin:S6508")
    @PostMapping("/logout")
    fun logout(exchange: ServerWebExchange): Mono<ResponseEntity<ApiResponse<Void>>> {
        val sessionId = sessionCookies.read(exchange)
        sessionCookies.clear(exchange)

        if (sessionId == null) return Mono.just(loggedOut())

        return sessionStore
            .find(sessionId)
            .flatMap { session -> keycloakTokenClient.revoke(session.refreshToken) }
            .then(sessionStore.delete(sessionId))
            .thenReturn(loggedOut())
            .onErrorResume { ex ->
                log.warn("Keycloak revocation failed, local session destroyed anyway: {}", ex.message)
                sessionStore.delete(sessionId).thenReturn(loggedOut())
            }
    }

    private fun noSession(): ResponseEntity<ApiResponse<SessionResponse>> =
        ApiResponse.buildResponse(
            data = null,
            message = MSG_NO_SESSION,
            status = HttpStatus.UNAUTHORIZED,
        )

    @Suppress("kotlin:S6508")
    private fun loggedOut(): ResponseEntity<ApiResponse<Void>> =
        ApiResponse.buildResponse(
            data = null,
            message = MSG_LOGOUT_SUCCESS,
            status = HttpStatus.NO_CONTENT,
        )

    private fun redirectToApp(error: String?): ResponseEntity<Void> {
        val target =
            UriComponentsBuilder
                .fromUriString(sharedConfig.oauth.postLoginRedirectUri)
                .apply { if (error != null) queryParam(ERROR_PARAM, error) }
                .encode()
                .build()
                .toUriString()
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build()
    }

    private fun keycloakAuthorizationUrl(): String = "${sharedConfig.serverUrl}/realms/${sharedConfig.realm}/protocol/openid-connect/auth"
}
