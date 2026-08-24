package com.vertyll.veds.project.application.port.inbound.query

import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.domain.model.LanguageTag
import java.util.UUID

interface ProjectStatusQueryUseCase {
    fun getStatuses(
        projectId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): List<ProjectStatusResponse>
}
