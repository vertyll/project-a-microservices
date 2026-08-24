package com.vertyll.veds.sharedtranslation

import com.ibm.icu.text.MessageFormat
import java.util.Locale

/**
 * Renders a translation key into a message using ICU MessageFormat.
 *
 * A missing key renders as the key itself. That is not a fallback hiding a
 * problem: substituting another language looks like a finished translation,
 * whereas a bare key is greppable and names exactly what has to be fixed.
 * Throwing would take down a page over one absent string.
 *
 * Used by the back end for the two things it must render itself — e-mail bodies,
 * which are sent asynchronously and so have no request language, and spreadsheet
 * headers.
 */
class MessageResolver(
    private val source: TranslationSource,
) {
    fun resolve(
        key: String,
        language: String,
        arguments: Map<String, Any> = emptyMap(),
    ): String {
        val pattern = source.patternFor(key, language) ?: return key

        return try {
            MessageFormat(pattern, Locale.forLanguageTag(language)).format(arguments)
        } catch (e: IllegalArgumentException) {
            key
        }
    }

    fun hasTranslation(
        key: String,
        language: String,
    ): Boolean = source.patternFor(key, language) != null
}

/**
 * Where [MessageResolver] reads patterns from: a cache, a snapshot, a test
 * fixture or the build-time defaults.
 */
fun interface TranslationSource {
    fun patternFor(
        key: String,
        language: String,
    ): String?
}
