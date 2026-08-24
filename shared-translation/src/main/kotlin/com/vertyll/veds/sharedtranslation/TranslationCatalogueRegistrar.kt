package com.vertyll.veds.sharedtranslation

/**
 * Publishes a service's declared keys to the catalogue.
 *
 * Implementations must be additive: insert what is new, refresh
 * `default_value`, and never touch `override_value`. Anything else would make
 * every deploy silently revert the administrators' work.
 */
fun interface TranslationCatalogueRegistrar {
    fun register(catalogue: TranslationCatalogue)
}
