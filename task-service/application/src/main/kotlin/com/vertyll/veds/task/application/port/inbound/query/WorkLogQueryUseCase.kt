package com.vertyll.veds.task.application.port.inbound.query

import com.vertyll.veds.task.application.dto.WorkLogEntryResponse
import java.util.UUID

interface WorkLogQueryUseCase {
    fun getEntries(
        taskId: UUID,
        actorId: UUID,
    ): List<WorkLogEntryResponse>
}
