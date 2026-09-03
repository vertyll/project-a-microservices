package com.vertyll.veds.task.application.service.query

import com.vertyll.veds.task.application.dto.WorkLogEntryResponse
import com.vertyll.veds.task.application.port.inbound.query.WorkLogQueryUseCase
import com.vertyll.veds.task.application.port.outbound.TaskQueryPort
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import java.util.UUID

class WorkLogQueryService(
    private val queryPort: TaskQueryPort,
    private val projectDirectory: ProjectDirectoryRepository,
    private val authorization: TaskAuthorizationService,
) : WorkLogQueryUseCase {
    override fun getEntries(
        taskId: UUID,
        actorId: UUID,
    ): List<WorkLogEntryResponse> {
        val task = authorization.requireTaskPermission(taskId, actorId, TaskPermission.VIEW_TASKS)
        val project = projectDirectory.findProject(task.projectId)
        val roleCode = projectDirectory.findMembership(task.projectId, actorId)?.roleCode
        val readsHidden = project != null && project.allowsHiddenWorkLogFor(roleCode)

        return queryPort.findWorkLog(taskId).filter { !it.hidden || it.author.id == actorId || readsHidden }
    }
}
