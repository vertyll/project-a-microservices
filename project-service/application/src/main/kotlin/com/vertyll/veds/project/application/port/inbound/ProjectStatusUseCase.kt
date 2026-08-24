package com.vertyll.veds.project.application.port.inbound

import com.vertyll.veds.project.application.command.CreateStatusCommand
import com.vertyll.veds.project.application.command.UpdateStatusCommand
import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.domain.model.LanguageCode
import java.util.UUID

interface ProjectStatusUseCase {
    fun getStatuses(
        projectId: UUID,
        actorId: UUID,
        language: LanguageCode,
    ): List<ProjectStatusResponse>

    fun createStatus(
        projectId: UUID,
        request: CreateStatusCommand,
        actorId: UUID,
        language: LanguageCode,
    ): ProjectStatusResponse

    fun updateStatus(
        projectId: UUID,
        statusId: UUID,
        request: UpdateStatusCommand,
        actorId: UUID,
        language: LanguageCode,
        version: Long? = null,
    ): ProjectStatusResponse

    fun deleteStatus(
        projectId: UUID,
        statusId: UUID,
        actorId: UUID,
    )
}
