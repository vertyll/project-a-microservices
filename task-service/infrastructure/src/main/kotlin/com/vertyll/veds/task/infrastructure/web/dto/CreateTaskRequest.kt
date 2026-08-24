package com.vertyll.veds.task.infrastructure.web.dto

import com.vertyll.veds.task.application.command.CreateTaskCommand
import jakarta.validation.constraints.NotBlank

data class CreateTaskRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val payload: String,
) {
    fun toCommand(): CreateTaskCommand =
        CreateTaskCommand(
            name = name,
            payload = payload,
        )
}
