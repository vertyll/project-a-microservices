package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import java.time.Instant
import java.util.UUID

data class ProjectResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val isActive: Boolean,
    val typeId: UUID?,
    val iconFileId: UUID?,
    val ownerId: UUID,
    val hiddenWorkLogEnabled: Boolean,
    val hiddenWorkLogRoles: Set<ProjectRoleCode>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long?,
    val permissions: Set<ProjectPermission> = emptySet(),
) {
    companion object {
        fun from(
            project: Project,
            permissions: Set<ProjectPermission> = emptySet(),
        ): ProjectResponse =
            ProjectResponse(
                id = project.id,
                name = project.name,
                description = project.description,
                isPublic = project.isPublic,
                isActive = project.isActive,
                typeId = project.typeId,
                iconFileId = project.iconFileId,
                ownerId = project.ownerId,
                hiddenWorkLogEnabled = project.hiddenWorkLogEnabled,
                hiddenWorkLogRoles = project.hiddenWorkLogRoles,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt,
                version = project.version,
                permissions = permissions,
            )
    }
}
