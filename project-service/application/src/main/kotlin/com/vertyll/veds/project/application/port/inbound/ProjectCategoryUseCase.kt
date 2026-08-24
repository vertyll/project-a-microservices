package com.vertyll.veds.project.application.port.inbound

import com.vertyll.veds.project.application.command.CreateCategoryCommand
import com.vertyll.veds.project.application.command.UpdateCategoryCommand
import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.domain.model.LanguageCode
import java.util.UUID

interface ProjectCategoryUseCase {
    fun getCategories(
        projectId: UUID,
        actorId: UUID,
        language: LanguageCode,
    ): List<ProjectCategoryResponse>

    fun createCategory(
        projectId: UUID,
        request: CreateCategoryCommand,
        actorId: UUID,
        language: LanguageCode,
    ): ProjectCategoryResponse

    fun updateCategory(
        projectId: UUID,
        categoryId: UUID,
        request: UpdateCategoryCommand,
        actorId: UUID,
        language: LanguageCode,
        version: Long? = null,
    ): ProjectCategoryResponse

    fun deleteCategory(
        projectId: UUID,
        categoryId: UUID,
        actorId: UUID,
    )
}
