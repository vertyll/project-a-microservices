package com.vertyll.veds.project.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class UpdateProjectRequest(
    @field:NotBlank(message = "Project name is required")
    @field:Size(max = 255, message = "Project name must not exceed 255 characters")
    val name: String = "",
    @field:Size(max = 5000, message = "Description must not exceed 5000 characters")
    val description: String? = null,
    val isPublic: Boolean = false,
    val typeId: UUID? = null,
    val iconFileId: UUID? = null,
)
