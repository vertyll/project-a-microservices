package com.vertyll.veds.project.domain.model

/**
 * Languages supported by translatable data.
 *
 * Kept as a domain enum rather than a `language` table: the set of supported
 * languages is a business decision of this bounded context, not runtime data.
 *
 * There is no default. A caller that does not state a language gets an error,
 * because silently picking one would render a Polish label to an English user
 * and look like a translation bug rather than a missing header.
 */
enum class LanguageCode {
    PL,
    EN,
    ;

    companion object {
        /**
         * Parses an `Accept-Language` tag. Returns `null` for an unsupported or
         * unparseable tag — the caller decides how to report it.
         */
        fun parse(tag: String?): LanguageCode? {
            val primary = tag?.substringBefore(',')?.substringBefore('-')?.trim() ?: return null
            return entries.firstOrNull { it.name.equals(primary, ignoreCase = true) }
        }
    }
}
