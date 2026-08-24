package com.vertyll.veds.sharedtranslation

/**
 * An immutable set of translations for one language.
 *
 * @property version served as an ETag, so a client already holding the current
 *           set confirms it with a 304 instead of transferring everything again.
 */
data class TranslationSnapshot(
    val language: String,
    val version: String,
    val entries: Map<String, String>,
) : TranslationSource {
    override fun patternFor(
        key: String,
        language: String,
    ): String? = if (language.equals(this.language, ignoreCase = true)) entries[key] else null

    companion object {
        fun empty(language: String) = TranslationSnapshot(language, EMPTY_VERSION, emptyMap())

        const val EMPTY_VERSION = "0"
    }
}

/**
 * A [TranslationSource] over several languages.
 *
 * @property fallbackDefaults values the owning services declared, packaged at
 *           build time. Not a per-key fallback — a key missing from both still
 *           renders as the key — but enough to keep e-mails going out with
 *           sensible text when the catalogue service is unreachable at start-up.
 */
class CompositeTranslationSource(
    private val snapshots: () -> Map<String, TranslationSnapshot>,
    private val fallbackDefaults: Map<String, Map<String, String>> = emptyMap(),
) : TranslationSource {
    override fun patternFor(
        key: String,
        language: String,
    ): String? {
        val normalized = language.lowercase()
        snapshots()[normalized]?.entries?.get(key)?.let { return it }
        return fallbackDefaults[normalized]?.get(key)
    }
}
