package com.vertyll.veds.apigateway.session

import com.fasterxml.jackson.annotation.JsonProperty
import com.vertyll.veds.shared.web.config.SharedConfigProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.Base64

@Component
internal class KeycloakTokenClient(
    private val sharedConfig: SharedConfigProperties,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
) {
    private companion object {
        private const val SUBJECT_CLAIM = "sub"
        private const val EMAIL_CLAIM = "email"
        private const val REALM_ACCESS_CLAIM = "realm_access"
        private const val ROLES_CLAIM = "roles"
        private const val JWT_PAYLOAD_INDEX = 1
        private const val JWT_PART_COUNT = 3
    }

    private val client: WebClient by lazy { WebClient.builder().build() }

    fun exchangeAuthorizationCode(
        code: String,
        codeVerifier: String,
    ): Mono<AuthSession> =
        post(
            tokenUrl(),
            BodyInserters
                .fromFormData("grant_type", "authorization_code")
                .with("client_id", sharedConfig.keycloak.gatewayClientId)
                .with("client_secret", sharedConfig.keycloak.gatewayClientSecret)
                .with("code", code)
                .with("redirect_uri", sharedConfig.keycloak.oauth.redirectUri)
                .with("code_verifier", codeVerifier),
        )

    fun refresh(refreshToken: String): Mono<AuthSession> =
        post(
            tokenUrl(),
            BodyInserters
                .fromFormData("grant_type", "refresh_token")
                .with("client_id", sharedConfig.keycloak.gatewayClientId)
                .with("client_secret", sharedConfig.keycloak.gatewayClientSecret)
                .with("refresh_token", refreshToken),
        )

    fun revoke(refreshToken: String): Mono<Void> =
        client
            .post()
            .uri(logoutUrl())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("client_id", sharedConfig.keycloak.gatewayClientId)
                    .with("client_secret", sharedConfig.keycloak.gatewayClientSecret)
                    .with("refresh_token", refreshToken),
            ).retrieve()
            .toBodilessEntity()
            .then()

    private fun post(
        url: String,
        body: BodyInserters.FormInserter<String>,
    ): Mono<AuthSession> =
        client
            .post()
            .uri(url)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .bodyToMono<KeycloakTokenResponse>()
            .map { it.toSession() }

    private fun KeycloakTokenResponse.toSession(): AuthSession {
        val claims = decodeClaims(accessToken)
        val realmAccess = claims[REALM_ACCESS_CLAIM] as? Map<*, *>

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresAt = Instant.now().plusSeconds(expiresIn),
            subject = claims[SUBJECT_CLAIM] as? String ?: error("access token has no 'sub' claim"),
            email = claims[EMAIL_CLAIM] as? String ?: error("access token has no 'email' claim"),
            roles = (realmAccess?.get(ROLES_CLAIM) as? List<*>).orEmpty().filterIsInstance<String>(),
        )
    }

    private fun decodeClaims(jwt: String): Map<String, Any> {
        val parts = jwt.split('.')
        require(parts.size == JWT_PART_COUNT) { "malformed access token received from Keycloak" }
        val payload = Base64.getUrlDecoder().decode(parts[JWT_PAYLOAD_INDEX])
        @Suppress("UNCHECKED_CAST")
        return objectMapper.readValue(payload, Map::class.java) as Map<String, Any>
    }

    private fun tokenUrl() = "${sharedConfig.keycloak.serverUrl}/realms/${sharedConfig.keycloak.realm}/protocol/openid-connect/token"

    private fun logoutUrl() = "${sharedConfig.keycloak.serverUrl}/realms/${sharedConfig.keycloak.realm}/protocol/openid-connect/logout"

    private data class KeycloakTokenResponse(
        @JsonProperty("access_token") val accessToken: String = "",
        @JsonProperty("expires_in") val expiresIn: Long = 0,
        @JsonProperty("refresh_token") val refreshToken: String = "",
    )
}
