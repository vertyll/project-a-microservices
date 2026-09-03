package com.vertyll.veds.task.application.port.inbound.query

import com.vertyll.veds.task.application.dto.WorkLogPageResponse
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.WorkLogVisibility
import java.util.UUID

interface WorkLogQueryUseCase {
    fun getEntries(
        taskId: UUID,
        actorId: UUID,
        visibility: WorkLogVisibility,
        pageRequest: PageRequest,
    ): WorkLogPageResponse
}
