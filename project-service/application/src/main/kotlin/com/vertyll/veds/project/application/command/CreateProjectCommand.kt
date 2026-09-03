package com.vertyll.veds.project.application.command

import com.vertyll.veds.project.domain.model.ProjectRoleCode
import java.util.UUID

data class CreateProjectCommand(
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val typeId: UUID?,
    val iconFileId: UUID?,
    val hiddenWorkLogEnabled: Boolean = false,
    val hiddenWorkLogRoles: Set<ProjectRoleCode> = emptySet(),
)
