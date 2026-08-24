package com.vertyll.veds.project.domain.model

import java.time.Instant
import java.util.UUID

data class ProjectMember(
    val id: UUID = UUID.randomUUID(),
    val projectId: UUID,
    val userId: UUID,
    val roleId: UUID,
    val assignedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    fun reassignTo(newRoleId: UUID): ProjectMember = copy(roleId = newRoleId, assignedAt = Instant.now())

    companion object {
        fun create(
            projectId: UUID,
            userId: UUID,
            roleId: UUID,
        ): ProjectMember =
            ProjectMember(
                projectId = projectId,
                userId = userId,
                roleId = roleId,
            )
    }
}
