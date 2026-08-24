package com.vertyll.veds.project.application.port.inbound

import com.vertyll.veds.project.application.dto.ProjectTypeResponse
import com.vertyll.veds.project.domain.model.LanguageCode
import java.util.UUID

interface ProjectTypeUseCase {
    fun getAllTypes(language: LanguageCode): List<ProjectTypeResponse>

    fun getTypeById(
        id: UUID,
        language: LanguageCode,
    ): ProjectTypeResponse
}
