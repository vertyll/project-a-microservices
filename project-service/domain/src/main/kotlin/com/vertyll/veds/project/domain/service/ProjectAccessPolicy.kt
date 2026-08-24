package com.vertyll.veds.project.domain.service

import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRole
import java.util.UUID

object ProjectAccessPolicy {
    // Order is the rule: the first decision wins, so deny overrides. Moving
    // OWNER_GRANT above RESOURCE_STATE would let an owner edit an archived project.
    private val RULES: List<AccessRule> =
        listOf(
            RESOURCE_STATE,
            OWNER_GRANT,
            PUBLIC_VISIBILITY,
            ROLE_GRANT,
        )

    fun evaluate(request: AccessRequest): AccessDecision {
        for (rule in RULES) {
            when (val decision = rule.evaluate(request)) {
                is AccessDecision.Deny -> return decision
                is AccessDecision.Permit -> return decision
                null -> continue
            }
        }
        return AccessDecision.Deny(ProjectError.PROJECT_ACCESS_DENIED)
    }

    fun permits(
        project: Project,
        userId: UUID,
        member: ProjectMember?,
        role: ProjectRole?,
        permission: ProjectPermission,
    ): AccessDecision =
        evaluate(
            AccessRequest(
                subjectId = userId,
                action = permission,
                project = project,
                membership = member,
                role = role,
            ),
        )

    fun canView(
        project: Project,
        userId: UUID,
        member: ProjectMember?,
        role: ProjectRole?,
    ): Boolean = permits(project, userId, member, role, ProjectPermission.VIEW_PROJECT).isPermitted

    fun permissionsOf(
        project: Project,
        userId: UUID,
        member: ProjectMember?,
        role: ProjectRole?,
    ): Set<ProjectPermission> =
        ProjectPermission.entries
            .filter { permits(project, userId, member, role, it).isPermitted }
            .toSet()
}

internal fun interface AccessRule {
    fun evaluate(request: AccessRequest): AccessDecision?
}

private val RESOURCE_STATE =
    AccessRule { request ->
        if (!request.project.isActive && request.isMutating) {
            AccessDecision.Deny(ProjectError.PROJECT_ARCHIVED)
        } else {
            null
        }
    }

private val OWNER_GRANT =
    AccessRule { request -> if (request.isOwner) AccessDecision.Permit else null }

private val PUBLIC_VISIBILITY =
    AccessRule { request ->
        if (request.project.isPublic && request.action == ProjectPermission.VIEW_PROJECT) {
            AccessDecision.Permit
        } else {
            null
        }
    }

private val ROLE_GRANT =
    AccessRule { request ->
        when {
            !request.hasCoherentMembership -> AccessDecision.Deny(ProjectError.PROJECT_ACCESS_DENIED)
            request.role?.grants(request.action) == true -> AccessDecision.Permit
            else -> AccessDecision.Deny(ProjectError.PROJECT_ACCESS_DENIED)
        }
    }
