package com.vertyll.veds.task.infrastructure.persistence.repository

import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectCategoryRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectMembershipRefId
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectMembershipRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectStatusRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.UserRefJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface ProjectRefJpaRepository : JpaRepository<ProjectRefJpaEntity, UUID>

@Repository
internal interface ProjectCategoryRefJpaRepository : JpaRepository<ProjectCategoryRefJpaEntity, UUID> {
    fun findAllByProjectId(projectId: UUID): List<ProjectCategoryRefJpaEntity>
}

@Repository
internal interface ProjectStatusRefJpaRepository : JpaRepository<ProjectStatusRefJpaEntity, UUID> {
    fun findAllByProjectId(projectId: UUID): List<ProjectStatusRefJpaEntity>
}

@Repository
internal interface ProjectMembershipRefJpaRepository : JpaRepository<ProjectMembershipRefJpaEntity, ProjectMembershipRefId> {
    fun findAllByProjectId(projectId: UUID): List<ProjectMembershipRefJpaEntity>

    fun deleteByProjectIdAndUserId(
        projectId: UUID,
        userId: UUID,
    )
}

@Repository
internal interface UserRefJpaRepository : JpaRepository<UserRefJpaEntity, UUID>
