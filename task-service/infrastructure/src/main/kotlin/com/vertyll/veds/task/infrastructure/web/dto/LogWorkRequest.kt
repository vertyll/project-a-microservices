package com.vertyll.veds.task.infrastructure.web.dto

import com.vertyll.veds.task.application.command.LogWorkCommand
import jakarta.validation.constraints.Min

data class LogWorkRequest(
    @field:Min(value = 0, message = "validation.task.worked_time_negative")
    val hundredthsOfHour: Int = 0,
) {
    fun toCommand(): LogWorkCommand = LogWorkCommand(hundredthsOfHour = hundredthsOfHour)
}
