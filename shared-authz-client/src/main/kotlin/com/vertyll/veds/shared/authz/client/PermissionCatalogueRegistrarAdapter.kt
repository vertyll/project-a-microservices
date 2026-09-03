package com.vertyll.veds.shared.authz.client

import com.vertyll.veds.sharedauthz.PermissionCatalogue
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * Publishes a module's permission catalogue to `iam-service` over HTTP.
 *
 * HTTP rather than an event, for the same reason the translation catalogue uses
 * it: a rejected catalogue is a deployment mistake, and it belongs in the log of
 * the service that made it rather than failing quietly in a consumer elsewhere.
 */
@Component
class PermissionCatalogueRegistrarAdapter(
    properties: AuthzClientProperties,
) {
    private val client: RestClient = RestClient.builder().baseUrl(properties.baseUrl).build()

    fun register(catalogue: PermissionCatalogue) {
        client
            .post()
            .uri("/internal/authz/catalogue")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                RegisterCataloguePayload(
                    module = catalogue.module,
                    permissions =
                        catalogue.definitions.map {
                            PermissionPayload(name = it.name, description = it.description, scope = it.scope.name)
                        },
                    stockRoles =
                        catalogue.stockRoles.map {
                            StockRolePayload(name = it.name, scope = it.scope.name, permissions = it.permissions)
                        },
                ),
            ).retrieve()
            .toBodilessEntity()
    }

    internal data class RegisterCataloguePayload(
        val module: String,
        val permissions: List<PermissionPayload>,
        val stockRoles: List<StockRolePayload>,
    )

    internal data class StockRolePayload(
        val name: String,
        val scope: String,
        val permissions: Set<String>,
    )

    internal data class PermissionPayload(
        val name: String,
        val description: String?,
        val scope: String,
    )
}

/**
 * @property baseUrl where `iam-service` lives. The call goes straight to the service
 *           inside the cluster: the gateway routes no `/internal` path, and registration
 *           is not a request any browser makes.
 * @property registrationRetryInterval how long to wait before trying again when
 *           iam-service is not answering yet. Registration is what fills this
 *           service's role projection, so giving up would leave every permission
 *           check failing closed until the next restart.
 *
 * Example:
 * ```yaml
 * veds:
 *   authz:
 *     client:
 *       base-url: http://iam-service:8082
 *       registration-retry-interval: 15s
 * ```
 */
@ConfigurationProperties(prefix = "veds.authz.client")
data class AuthzClientProperties(
    val baseUrl: String,
    val registrationRetryInterval: Duration = DEFAULT_REGISTRATION_RETRY_INTERVAL,
) {
    private companion object {
        private val DEFAULT_REGISTRATION_RETRY_INTERVAL: Duration = Duration.ofSeconds(15)
    }
}
