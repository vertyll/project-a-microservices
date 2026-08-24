package com.vertyll.veds.project.application.port.inbound.query

import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.domain.model.LanguageTag
import java.util.UUID

interface ProjectCategoryQueryUseCase {
    fun getCategories(
        projectId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): List<ProjectCategoryResponse>
}
