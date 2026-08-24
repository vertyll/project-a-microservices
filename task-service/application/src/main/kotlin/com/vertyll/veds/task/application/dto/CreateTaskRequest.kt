package com.vertyll.veds.task.application.dto

import jakarta.validation.constraints.NotBlank

data class CreateTaskRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val payload: String,
)
