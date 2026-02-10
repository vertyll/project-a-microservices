package com.vertyll.projecta.sharedinfrastructure.http

object ETagUtil {
    private const val WEAK_PREFIX = "W/\""
    private const val STRONG_QUOTE = '"'

    // Build a weak ETag from a nullable version
    fun buildWeakETag(version: Long?): String? = version?.let { "$WEAK_PREFIX$it\"" }

    // Parse If-Match header to extract version (supports W/"<n>" and "<n>")
    fun parseIfMatchToVersion(ifMatch: String?): Long? {
        if (ifMatch.isNullOrBlank()) return null
        val trimmed = ifMatch.trim()
        val raw = when {
            trimmed.startsWith("W/\"") && trimmed.endsWith('"') ->
                trimmed.removePrefix("W/").trim()
            trimmed.startsWith('"') && trimmed.endsWith('"') ->
                trimmed
            else -> trimmed
        }
        val unquoted = raw.trim().trimStart(STRONG_QUOTE).trimEnd(STRONG_QUOTE)
        return unquoted.toLongOrNull()
    }
}