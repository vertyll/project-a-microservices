package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.command.CreateCategoryCommand
import com.vertyll.veds.project.application.command.UpdateCategoryCommand
import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.application.exception.ApiException
import com.vertyll.veds.project.application.port.inbound.ProjectCategoryUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageCode
import com.vertyll.veds.project.domain.model.ProjectCategory
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.VersionGuard
import com.vertyll.veds.project.domain.repository.ProjectCategoryRepository
import java.util.UUID

/**
 * Manages the categories owned by a project.
 *
 * Every mutation publishes `project-category-changed` so task-service can keep
 * its read model current — tasks reference categories but live in another
 * database, so there is no foreign key to fall back on.
 */
class ProjectCategoryService(
    private val categoryRepository: ProjectCategoryRepository,
    private val authorization: ProjectAuthorizationService,
    private val eventPublisher: ProjectEventPublisherPort,
) : ProjectCategoryUseCase {
    override fun getCategories(
        projectId: UUID,
        actorId: UUID,
        language: LanguageCode,
    ): List<ProjectCategoryResponse> {
        authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)
        return categoryRepository
            .findAllByProjectId(projectId)
            .map { ProjectCategoryResponse.from(it, language) }
    }

    override fun createCategory(
        projectId: UUID,
        request: CreateCategoryCommand,
        actorId: UUID,
        language: LanguageCode,
    ): ProjectCategoryResponse {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        val category =
            categoryRepository.save(
                ProjectCategory.create(
                    projectId = projectId,
                    color = request.color,
                    translations = request.translations,
                ),
            )

        eventPublisher.publishCategoryChanged(
            projectId = projectId,
            categoryId = category.id,
            names = category.translations.associate { it.language.name to it.name },
            color = category.color,
            removed = false,
        )

        return ProjectCategoryResponse.from(category, language)
    }

    override fun updateCategory(
        projectId: UUID,
        categoryId: UUID,
        request: UpdateCategoryCommand,
        actorId: UUID,
        language: LanguageCode,
        version: Long?,
    ): ProjectCategoryResponse {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        val category = loadOwnedCategory(projectId, categoryId)

        VersionGuard.requireMatch(category.version, version) {
            ApiException(ProjectError.VERSION_MISMATCH)
        }

        val updated =
            categoryRepository.save(
                category
                    .recolor(request.color)
                    .retranslate(request.translations)
                    .let { if (request.isActive) it.activate() else it.deactivate() },
            )

        eventPublisher.publishCategoryChanged(
            projectId = projectId,
            categoryId = updated.id,
            names = updated.translations.associate { it.language.name to it.name },
            color = updated.color,
            removed = !updated.isActive,
        )

        return ProjectCategoryResponse.from(updated, language)
    }

    override fun deleteCategory(
        projectId: UUID,
        categoryId: UUID,
        actorId: UUID,
    ) {
        authorization.requirePermission(projectId, actorId, ProjectPermission.EDIT_PROJECT)

        val category = loadOwnedCategory(projectId, categoryId)
        categoryRepository.delete(category.id)

        eventPublisher.publishCategoryChanged(
            projectId = projectId,
            categoryId = category.id,
            names = category.translations.associate { it.language.name to it.name },
            color = category.color,
            removed = true,
        )
    }

    /**
     * Guards against a caller reaching a category of project A through the URL
     * of project B, where they happen to have edit rights.
     */
    private fun loadOwnedCategory(
        projectId: UUID,
        categoryId: UUID,
    ): ProjectCategory {
        val category =
            categoryRepository.findById(categoryId)
                ?: throw ApiException(ProjectError.CATEGORY_NOT_FOUND)
        if (category.projectId != projectId) {
            throw ApiException(ProjectError.CATEGORY_NOT_FOUND)
        }
        return category
    }
}
