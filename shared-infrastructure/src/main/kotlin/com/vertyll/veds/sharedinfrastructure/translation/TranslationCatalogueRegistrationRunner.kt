package com.vertyll.veds.sharedinfrastructure.translation

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
/**
 * Registers the service's declared keys with `translation-service` at start-up.
 *
 * A service opts in by exposing a
 * [TranslationCatalogue] bean.
 *
 * Failure does not stop the service. An unpublished catalogue is a degraded
 * state — clients use whatever the catalogue already holds, and a genuinely
 * missing key renders as the key — whereas refusing to start would let a brief
 * outage of one service take down every other.
 */
@Component
class TranslationCatalogueRegistrationRunner(
    private val catalogues: List<TranslationCatalogue>,
    private val registrar: TranslationCatalogueRegistrarAdapter,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (catalogues.isEmpty()) {
            logger.debug("No translation catalogue declared, nothing to register")
            return
        }

        catalogues.forEach { catalogue ->
            try {
                registrar.register(catalogue)
                logger.info(
                    "Registered {} translation keys from {}",
                    catalogue.definitions.size,
                    catalogue.sourceService,
                )
            } catch (e: Exception) {
                logger.error(
                    "Could not register the translation catalogue for {}: {}. " +
                        "The service continues; keys will be republished on the next start.",
                    catalogue.sourceService,
                    e.message,
                )
            }
        }
    }
}
