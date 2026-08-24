package com.vertyll.veds.sharedinfrastructure.translation

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.TranslationCatalogueRegistrar
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Publishes a catalogue to `translation-service` over HTTP.
 *
 * HTTP rather than an event, because the caller wants an answer: a key rejected
 * for belonging to another service is a deployment mistake worth seeing in the
 * logs of the service that made it, not a message failing silently in a consumer
 * somewhere else.
 */
@Component
class TranslationCatalogueRegistrarAdapter(
    private val properties: TranslationClientProperties,
    restClientBuilder: RestClient.Builder,
) : TranslationCatalogueRegistrar {
    private val client: RestClient = restClientBuilder.baseUrl(properties.baseUrl).build()

    override fun register(catalogue: TranslationCatalogue) {
        client
            .post()
            .uri("/internal/translations/catalogue")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                RegisterCataloguePayload(
                    sourceService = catalogue.sourceService,
                    entries =
                        catalogue.definitions.map {
                            CatalogueEntryPayload(
                                key = it.key,
                                description = it.description,
                                defaultValues = it.defaultValues,
                            )
                        },
                ),
            ).retrieve()
            .toBodilessEntity()
    }

    internal data class RegisterCataloguePayload(
        val sourceService: String,
        val entries: List<CatalogueEntryPayload>,
    )

    internal data class CatalogueEntryPayload(
        val key: String,
        val description: String?,
        val defaultValues: Map<String, String>,
    )
}

/**
 * @property baseUrl where `translation-service` lives. Requests go through the
 *           gateway in production, so services never need each other's addresses.
 */
@ConfigurationProperties(prefix = "veds.translation.client")
data class TranslationClientProperties(
    val baseUrl: String,
)
