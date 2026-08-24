package com.vertyll.veds.project.domain.model

import java.time.Instant
import java.util.UUID

data class ProjectRole(
    val id: UUID = UUID.randomUUID(),
    val code: ProjectRoleCode,
    val permissions: Set<ProjectPermission> = emptySet(),
    val isActive: Boolean = true,
    val translations: Set<Translation> = emptySet(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        requireAtLeastOneTranslation(translations)
    }

    fun grants(permission: ProjectPermission): Boolean = isActive && permission in permissions

    fun translationFor(language: LanguageTag): Translation = translations.resolveFor(language)

    fun withPermissions(newPermissions: Set<ProjectPermission>): ProjectRole = copy(permissions = newPermissions, updatedAt = Instant.now())

    companion object {
        fun create(
            code: ProjectRoleCode,
            permissions: Set<ProjectPermission>,
            translations: Set<Translation>,
        ): ProjectRole =
            ProjectRole(
                code = code,
                permissions = permissions,
                translations = translations,
            )
    }
}
