package com.vertyll.veds.sharedtranslation

import com.ibm.icu.text.MessageFormat
import java.util.Locale

/**
 * Checks that a pattern is a compilable ICU message.
 *
 * Run twice for the same reason: an unbalanced brace in
 * `{count, plural, one{…}` does not fail quietly — it throws when the message is
 * rendered, in the middle of a page nobody was editing. So patterns are checked
 * when a service declares them and again when an administrator saves one.
 */
object IcuPatternValidator {
    fun requireValid(
        key: String,
        language: String,
        pattern: String,
    ) {
        val failure = validate(language, pattern)
        require(failure == null) { "invalid ICU pattern for '$key' [$language]: $failure" }
    }

    fun validate(
        language: String,
        pattern: String,
    ): String? =
        try {
            MessageFormat(pattern, Locale.forLanguageTag(language))
            null
        } catch (e: IllegalArgumentException) {
            e.message ?: "malformed ICU pattern"
        }

    fun argumentsOf(
        language: String,
        pattern: String,
    ): Set<String> =
        try {
            MessageFormat(pattern, Locale.forLanguageTag(language)).argumentNames.toSet()
        } catch (e: IllegalArgumentException) {
            emptySet()
        }
}
