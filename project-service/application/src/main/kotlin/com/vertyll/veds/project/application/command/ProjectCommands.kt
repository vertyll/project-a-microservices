package com.vertyll.veds.project.application.command

import com.vertyll.veds.project.domain.model.Translation
import java.util.UUID

data class CreateProjectCommand(
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val typeId: UUID?,
    val iconFileId: UUID?,
)

data class UpdateProjectCommand(
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val typeId: UUID?,
    val iconFileId: UUID?,
)

data class CreateCategoryCommand(
    val color: String,
    val translations: Set<Translation>,
)

data class UpdateCategoryCommand(
    val color: String,
    val translations: Set<Translation>,
    val isActive: Boolean,
)

data class CreateStatusCommand(
    val color: String,
    val translations: Set<Translation>,
)

data class UpdateStatusCommand(
    val color: String,
    val translations: Set<Translation>,
    val isActive: Boolean,
)

data class InviteMemberCommand(
    val email: String,
    val roleId: UUID?,
)

data class UpdateMemberRoleCommand(
    val roleId: UUID,
)
