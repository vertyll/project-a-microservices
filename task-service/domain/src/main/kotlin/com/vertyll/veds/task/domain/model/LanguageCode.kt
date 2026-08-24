package com.vertyll.veds.task.domain.model

/**
 * Languages supported by translatable data mirrored from project-service.
 *
 * There is no default: a caller that does not state a language gets an error,
 * because silently picking one renders Polish labels to an English client that
 * merely forgot the header.
 */
enum class LanguageCode {
    PL,
    EN,
    ;

    companion object {
        fun parse(tag: String?): LanguageCode? {
            val primary = tag?.substringBefore(',')?.substringBefore('-')?.trim() ?: return null
            return entries.firstOrNull { it.name.equals(primary, ignoreCase = true) }
        }
    }
}
