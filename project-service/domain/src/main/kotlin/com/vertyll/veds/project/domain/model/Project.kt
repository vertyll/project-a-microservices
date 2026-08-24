package com.vertyll.veds.project.domain.model

import java.time.Instant
import java.util.UUID

data class Project(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = false,
    val iconFileId: UUID? = null,
    val typeId: UUID? = null,
    val ownerId: UUID,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        require(name.isNotBlank()) { NAME_BLANK }
        require(name.length <= MAX_NAME_LENGTH) { NAME_TOO_LONG }
    }

    fun rename(newName: String): Project = copy(name = newName, updatedAt = Instant.now())

    fun describe(newDescription: String?): Project = copy(description = newDescription, updatedAt = Instant.now())

    fun changeType(newTypeId: UUID?): Project = copy(typeId = newTypeId, updatedAt = Instant.now())

    fun changeIcon(newIconFileId: UUID?): Project = copy(iconFileId = newIconFileId, updatedAt = Instant.now())

    fun changeVisibility(makePublic: Boolean): Project = copy(isPublic = makePublic, updatedAt = Instant.now())

    fun archive(): Project = copy(isActive = false, updatedAt = Instant.now())

    fun restore(): Project = copy(isActive = true, updatedAt = Instant.now())

    fun isOwnedBy(userId: UUID): Boolean = ownerId == userId

    private companion object {
        private const val MAX_NAME_LENGTH = 255
        private const val NAME_BLANK = "project name must not be blank"
        private const val NAME_TOO_LONG = "project name must not exceed 255 characters"
    }

    companion object {
        fun create(
            name: String,
            description: String?,
            isPublic: Boolean,
            typeId: UUID?,
            ownerId: UUID,
            iconFileId: UUID? = null,
        ): Project =
            Project(
                name = name,
                description = description,
                isPublic = isPublic,
                typeId = typeId,
                ownerId = ownerId,
                iconFileId = iconFileId,
            )
    }
}
