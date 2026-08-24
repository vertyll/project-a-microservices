package com.vertyll.veds.task.domain.model

import java.time.Instant
import java.util.UUID

data class TaskComment(
    val id: UUID = UUID.randomUUID(),
    val taskId: UUID,
    val authorId: UUID,
    val content: String,
    val attachmentIds: Set<UUID> = emptySet(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        require(content.isNotBlank()) { "comment content must not be blank" }
    }

    fun editedBy(
        editorId: UUID,
        newContent: String,
    ): TaskComment {
        check(editorId == authorId) { "only the author may edit a comment" }
        return copy(content = newContent, updatedAt = Instant.now())
    }

    fun withoutAttachment(attachmentId: UUID): TaskComment =
        if (attachmentId in attachmentIds) {
            copy(attachmentIds = attachmentIds - attachmentId, updatedAt = Instant.now())
        } else {
            this
        }

    fun isAuthoredBy(userId: UUID): Boolean = authorId == userId

    companion object {
        fun create(
            taskId: UUID,
            authorId: UUID,
            content: String,
            attachmentIds: Set<UUID> = emptySet(),
        ): TaskComment =
            TaskComment(
                taskId = taskId,
                authorId = authorId,
                content = content,
                attachmentIds = attachmentIds,
            )
    }
}
