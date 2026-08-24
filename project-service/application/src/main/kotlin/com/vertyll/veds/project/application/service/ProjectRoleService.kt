package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.dto.ProjectRoleResponse
import com.vertyll.veds.project.application.exception.ApiException
import com.vertyll.veds.project.application.port.inbound.ProjectRoleUseCase
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageCode
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import java.util.UUID

/**
 * Read-only access to the project role catalogue and the permissions each role
 * grants.
 *
 * The front-end uses this to render role pickers and to explain to a user what
 * a role will allow before assigning it.
 */
class ProjectRoleService(
    private val roleRepository: ProjectRoleRepository,
) : ProjectRoleUseCase {
    override fun getAllRoles(language: LanguageCode): List<ProjectRoleResponse> =
        roleRepository
            .findAll()
            .filter { it.isActive }
            .map { ProjectRoleResponse.from(it, language) }

    override fun getRoleById(
        id: UUID,
        language: LanguageCode,
    ): ProjectRoleResponse {
        val role = roleRepository.findById(id) ?: throw ApiException(ProjectError.ROLE_NOT_FOUND)
        return ProjectRoleResponse.from(role, language)
    }
}
