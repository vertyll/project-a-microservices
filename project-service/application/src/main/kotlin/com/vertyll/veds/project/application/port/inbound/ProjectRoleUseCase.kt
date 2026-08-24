package com.vertyll.veds.project.application.port.inbound

import com.vertyll.veds.project.application.dto.ProjectRoleResponse
import com.vertyll.veds.project.domain.model.LanguageCode
import java.util.UUID

interface ProjectRoleUseCase {
    fun getAllRoles(language: LanguageCode): List<ProjectRoleResponse>

    fun getRoleById(
        id: UUID,
        language: LanguageCode,
    ): ProjectRoleResponse
}
