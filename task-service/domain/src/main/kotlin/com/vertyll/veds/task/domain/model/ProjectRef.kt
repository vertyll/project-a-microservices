package com.vertyll.veds.task.domain.model

import java.time.Instant
import java.util.UUID

data class ProjectRef(
    val projectId: UUID,
    val name: String,
    val isActive: Boolean = true,
    val updatedAt: Instant = Instant.now(),
)

data class ResolvedLabel(
    val name: String,
    val language: String,
)

internal fun Map<String, String>.resolveLabel(language: LanguageTag): ResolvedLabel {
    this[language.value]?.let { return ResolvedLabel(it, language.value) }
    // Unreachable: both refs reject an empty name map at construction. Rendering an
    // empty string here instead would put a blank chip on the board with nothing to
    // trace it back to.
    val entry = entries.minByOrNull { it.key } ?: error("label projection has no names at all")
    return ResolvedLabel(entry.value, entry.key)
}

data class ProjectCategoryRef(
    val categoryId: UUID,
    val projectId: UUID,
    val names: Map<String, String>,
    val color: String,
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(names.isNotEmpty()) { "a label projection must carry at least one name" }
    }

    fun resolve(language: LanguageTag): ResolvedLabel = names.resolveLabel(language)
}

data class ProjectStatusRef(
    val statusId: UUID,
    val projectId: UUID,
    val names: Map<String, String>,
    val color: String,
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(names.isNotEmpty()) { "a label projection must carry at least one name" }
    }

    fun resolve(language: LanguageTag): ResolvedLabel = names.resolveLabel(language)
}

data class ProjectMembershipRef(
    val projectId: UUID,
    val userId: UUID,
    val roleCode: String,
    val updatedAt: Instant = Instant.now(),
)

data class UserRef(
    val userId: UUID,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarFileId: UUID? = null,
    val updatedAt: Instant = Instant.now(),
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { email }
}
