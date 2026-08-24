package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRole
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import java.util.UUID

data class ProjectRoleResponse(
    val id: UUID,
    val code: ProjectRoleCode,
    val name: String,
    val description: String?,
    val permissions: Set<ProjectPermission>,
    val isActive: Boolean,
    val version: Long?,
) {
    companion object {
        fun from(
            role: ProjectRole,
            language: LanguageTag,
        ): ProjectRoleResponse =
            ProjectRoleResponse(
                id = role.id,
                code = role.code,
                name = role.translationFor(language).name,
                description = role.translationFor(language).description,
                permissions = role.permissions,
                isActive = role.isActive,
                version = role.version,
            )
    }
}
