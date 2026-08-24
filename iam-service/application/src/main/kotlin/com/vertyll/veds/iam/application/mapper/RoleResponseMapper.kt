package com.vertyll.veds.iam.application.mapper

import com.vertyll.veds.iam.application.dto.RoleResponse
import com.vertyll.veds.iam.domain.model.Role

internal object RoleResponseMapper {
    fun toResponse(role: Role): RoleResponse =
        RoleResponse(
            id = role.id!!,
            name = role.name,
            description = role.description,
            permissions = role.permissions.map { it.name }.sorted(),
            version = role.version,
        )
}
