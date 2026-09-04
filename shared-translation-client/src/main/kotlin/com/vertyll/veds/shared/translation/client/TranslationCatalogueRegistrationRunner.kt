package com.vertyll.veds.shared.translation.client

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Registers the service's declared keys with `translation-service` at start-up.
 *
 * A service opts in by exposing a [TranslationCatalogue] bean.
 *
 * Registration happens off the start-up thread and keeps retrying, so a service
 * that boots before translation-service does not depend on the order the two came
 * up in. A key that never registered renders as the key itself to every reader,
 * which is why one failed attempt is not the end of it.
 *
 * Failure never stops the service. Refusing to boot would let a brief outage of
 * translation-service take down every other service with it.
 */
@Component
class TranslationCatalogueRegistrationRunner(
    private val catalogues: List<TranslationCatalogue>,
    private val registrar: TranslationCatalogueRegistrarAdapter,
    private val properties: TranslationClientProperties,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (catalogues.isEmpty()) {
            logger.debug("No translation catalogue declared, nothing to register")
            return
        }

        val worker =
            Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "translation-registration")
            }
        worker.execute { registerUntilAccepted(worker) }
    }

    private fun registerUntilAccepted(worker: ScheduledExecutorService) {
        val pending = catalogues.filterNot(::register)

        if (pending.isEmpty()) {
            worker.shutdown()
            return
        }

        logger.warn(
            "{} catalogue(s) not registered yet; retrying in {}s",
            pending.size,
            properties.registrationRetryInterval.seconds,
        )
        worker.schedule(
            { registerUntilAccepted(worker) },
            properties.registrationRetryInterval.seconds,
            TimeUnit.SECONDS,
        )
    }

    private fun register(catalogue: TranslationCatalogue): Boolean =
        try {
            registrar.register(catalogue)
            logger.info(
                "Registered {} translation keys from {}",
                catalogue.definitions.size,
                catalogue.sourceService,
            )
            true
        } catch (e: Exception) {
            logger.error(
                "Could not register the translation catalogue for {}: {}",
                catalogue.sourceService,
                e.message,
            )
            false
        }
}
