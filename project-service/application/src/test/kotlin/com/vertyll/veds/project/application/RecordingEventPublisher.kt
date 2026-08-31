package com.vertyll.veds.project.application

import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import java.util.UUID

/**
 * Records what the application layer asked to be announced. Other services learn about a project
 * only through these events, so "did the use case publish it" is part of the behaviour under test,
 * not an implementation detail.
 */
internal open class RecordingEventPublisher : ProjectEventPublisherPort {
    val published = mutableListOf<String>()

    override fun publishProjectCreated(
        projectId: UUID,
        name: String,
        ownerId: UUID,
    ) {
        published += "ProjectCreated($projectId)"
    }

    override fun publishProjectUpdated(
        projectId: UUID,
        name: String,
    ) {
        published += "ProjectUpdated($projectId)"
    }

    override fun publishProjectArchived(
        projectId: UUID,
        sagaId: String?,
    ) {
        published += "ProjectArchived($projectId)"
    }

    override fun publishMemberInvited(
        projectId: UUID,
        projectName: String,
        invitationId: UUID,
        inviteeEmail: String,
        inviterId: UUID,
        sagaId: String?,
    ) {
        published += "MemberInvited($projectId,$inviteeEmail)"
    }

    override fun publishMemberJoined(
        projectId: UUID,
        memberId: UUID,
        userId: UUID,
        roleCode: String,
    ) {
        published += "MemberJoined($projectId,$userId,$roleCode)"
    }

    override fun publishMemberRemoved(
        projectId: UUID,
        userId: UUID,
    ) {
        published += "MemberRemoved($projectId,$userId)"
    }

    override fun publishCategoryChanged(
        projectId: UUID,
        categoryId: UUID,
        names: Map<String, String>,
        color: String,
        removed: Boolean,
    ) {
        published += "CategoryChanged($projectId,$categoryId,removed=$removed)"
    }

    override fun publishStatusChanged(
        projectId: UUID,
        statusId: UUID,
        names: Map<String, String>,
        color: String,
        removed: Boolean,
    ) {
        published += "StatusChanged($projectId,$statusId,removed=$removed)"
    }
}
