package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.dto.ProjectTypeResponse
import com.vertyll.veds.project.application.exception.ApiException
import com.vertyll.veds.project.application.port.inbound.ProjectTypeUseCase
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageCode
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import java.util.UUID

/**
 * Read-only access to project type reference data.
 *
 * Types are seeded by `ReferenceDataInitializer`; there is deliberately no
 * create/delete use case, because adding a type means adding a
 * [com.vertyll.veds.project.domain.model.ProjectTypeCode] and therefore a code change.
 */
class ProjectTypeService(
    private val typeRepository: ProjectTypeRepository,
) : ProjectTypeUseCase {
    override fun getAllTypes(language: LanguageCode): List<ProjectTypeResponse> =
        typeRepository
            .findAll()
            .filter { it.isActive }
            .map { ProjectTypeResponse.from(it, language) }

    override fun getTypeById(
        id: UUID,
        language: LanguageCode,
    ): ProjectTypeResponse {
        val type = typeRepository.findById(id) ?: throw ApiException(ProjectError.TYPE_NOT_FOUND)
        return ProjectTypeResponse.from(type, language)
    }
}
