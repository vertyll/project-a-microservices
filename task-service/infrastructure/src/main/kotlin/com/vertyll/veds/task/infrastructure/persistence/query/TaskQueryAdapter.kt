package com.vertyll.veds.task.infrastructure.persistence.query

import com.vertyll.veds.task.application.dto.TaskCategoryView
import com.vertyll.veds.task.application.dto.TaskCommentResponse
import com.vertyll.veds.task.application.dto.TaskListItemResponse
import com.vertyll.veds.task.application.dto.TaskUserView
import com.vertyll.veds.task.application.port.outbound.TaskQueryPort
import com.vertyll.veds.task.domain.model.LanguageTag
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.PageResult
import com.vertyll.veds.task.domain.model.TaskPriority
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import com.vertyll.veds.task.domain.model.TaskSortField
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@Suppress("LongMethod")
internal class TaskQueryAdapter : TaskQueryPort {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun searchTasks(
        criteria: TaskSearchCriteria,
        pageRequest: PageRequest,
        language: LanguageTag,
    ): PageResult<TaskListItemResponse> {
        val where =
            """
            WHERE t.projectId = :projectId
            AND (:onlyActive = FALSE OR t.isActive = TRUE)
            AND (:statusId IS NULL OR t.statusId = :statusId)
            AND (:priority IS NULL OR t.priority = :priority)
            AND (:categoryId IS NULL OR :categoryId MEMBER OF t.categoryIds)
            AND (:assigneeId IS NULL OR :assigneeId MEMBER OF t.assigneeIds)
            AND (
                :searchTerm IS NULL
                OR LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
                OR LOWER(COALESCE(t.additionalDescription, '')) LIKE
                   LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
            )
            """

        val order =
            when (criteria.sortBy) {
                TaskSortField.CREATED_AT -> "t.createdAt"
                TaskSortField.UPDATED_AT -> "t.updatedAt"
                TaskSortField.PRIORITY -> "t.priority"
                TaskSortField.DESCRIPTION -> "t.description"
            } + if (criteria.sortDescending) " DESC" else " ASC"

        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT t.id, t.projectId, t.description, t.priority, t.statusId,
                           t.workedTime, t.createdAt, t.version,
                           (SELECT COUNT(c) FROM TaskCommentJpaEntity c WHERE c.taskId = t.id)
                    FROM TaskJpaEntity t
                    $where
                    ORDER BY $order
                    """,
                    Array<Any>::class.java,
                ).applyCriteria(criteria)
                .setFirstResult(pageRequest.offset.toInt())
                .setMaxResults(pageRequest.size)
                .resultList

        val total =
            entityManager
                .createQuery("SELECT COUNT(t) FROM TaskJpaEntity t $where", java.lang.Long::class.java)
                .applyCriteria(criteria)
                .singleResult
                .toLong()

        val taskIds = rows.map { it[0] as UUID }
        val categoriesByTask = categoriesFor(taskIds, criteria.projectId, language)
        val assigneesByTask = assigneesFor(taskIds)
        val statuses = statusesFor(criteria.projectId, language)

        return PageResult(
            content =
                rows.map { r ->
                    val id = r[0] as UUID
                    val statusId = r[4] as UUID?
                    val status = statusId?.let { statuses[it] }
                    TaskListItemResponse(
                        id = id,
                        projectId = r[1] as UUID,
                        description = r[2] as String,
                        priority = r[3] as TaskPriority,
                        statusId = statusId,
                        statusName = status?.name,
                        statusColor = status?.color,
                        categories = categoriesByTask[id].orEmpty(),
                        assignees = assigneesByTask[id].orEmpty(),
                        commentCount = (r[8] as Long).toInt(),
                        workedTime = r[5] as Int,
                        createdAt = r[6] as Instant,
                        version = r[7] as Long?,
                    )
                },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = total,
        )
    }

    override fun findComments(taskId: UUID): List<TaskCommentResponse> {
        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT c.id, c.taskId, c.authorId, c.content, c.createdAt, c.updatedAt, c.version,
                           u.email, u.firstName, u.lastName, u.avatarFileId
                    FROM TaskCommentJpaEntity c
                    LEFT JOIN UserRefJpaEntity u ON u.userId = c.authorId
                    WHERE c.taskId = :taskId
                    ORDER BY c.createdAt
                    """,
                    Array<Any>::class.java,
                ).setParameter("taskId", taskId)
                .resultList

        val attachments = attachmentsFor(rows.map { it[0] as UUID })

        return rows.map { r ->
            val id = r[0] as UUID
            val email = r[7] as String?
            TaskCommentResponse(
                id = id,
                taskId = r[1] as UUID,
                author =
                    TaskUserView(
                        id = r[2] as UUID,
                        displayName =
                            listOfNotNull(r[8] as String?, r[9] as String?)
                                .joinToString(" ")
                                .ifBlank { email ?: (r[2] as UUID).toString() },
                        avatarFileId = r[10] as UUID?,
                    ),
                content = r[3] as String,
                attachmentIds = attachments[id].orEmpty(),
                createdAt = r[4] as Instant,
                updatedAt = r[5] as Instant,
                version = r[6] as Long?,
            )
        }
    }

    private fun categoriesFor(
        taskIds: List<UUID>,
        projectId: UUID,
        language: LanguageTag,
    ): Map<UUID, List<TaskCategoryView>> {
        if (taskIds.isEmpty()) return emptyMap()

        @Suppress("UNCHECKED_CAST")
        val links =
            entityManager
                .createNativeQuery("SELECT task_id, category_id FROM task_category WHERE task_id IN (:ids)")
                .setParameter("ids", taskIds)
                .resultList as List<Array<Any?>>

        val labels = categoryLabels(projectId, language)

        return links
            .groupBy { it[0] as UUID }
            .mapValues { (_, rows) ->
                rows.mapNotNull { row ->
                    val categoryId = row[1] as UUID
                    labels[categoryId]?.let { label ->
                        TaskCategoryView(
                            id = categoryId,
                            name = label.name,
                            nameLanguage = label.language,
                            color = label.color,
                        )
                    }
                }
            }
    }

    private fun assigneesFor(taskIds: List<UUID>): Map<UUID, List<TaskUserView>> {
        if (taskIds.isEmpty()) return emptyMap()

        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(
                    """
                    SELECT a.task_id, a.user_id, u.email, u.first_name, u.last_name, u.avatar_url
                    FROM task_assignee a
                    LEFT JOIN user_ref u ON u.user_id = a.user_id
                    WHERE a.task_id IN (:ids)
                    """,
                ).setParameter("ids", taskIds)
                .resultList as List<Array<Any?>>

        return rows
            .groupBy { it[0] as UUID }
            .mapValues { (_, group) ->
                group.map { r ->
                    val userId = r[1] as UUID
                    val email = r[2] as String?
                    TaskUserView(
                        id = userId,
                        displayName =
                            listOfNotNull(r[3] as String?, r[4] as String?)
                                .joinToString(" ")
                                .ifBlank { email ?: userId.toString() },
                        avatarFileId = r[5] as UUID?,
                    )
                }
            }
    }

    private fun attachmentsFor(commentIds: List<UUID>): Map<UUID, Set<UUID>> {
        if (commentIds.isEmpty()) return emptyMap()

        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(
                    "SELECT comment_id, attachment_id FROM task_comment_attachment WHERE comment_id IN (:ids)",
                ).setParameter("ids", commentIds)
                .resultList as List<Array<Any?>>

        return rows.groupBy { it[0] as UUID }.mapValues { (_, g) -> g.map { it[1] as UUID }.toSet() }
    }

    private fun categoryLabels(
        projectId: UUID,
        language: LanguageTag,
    ): Map<UUID, ResolvedLabelRow> = labelsOf("project_category_ref", "category_id", projectId, language)

    private fun statusesFor(
        projectId: UUID,
        language: LanguageTag,
    ): Map<UUID, ResolvedLabelRow> = labelsOf("project_status_ref", "status_id", projectId, language)

    private fun labelsOf(
        table: String,
        idColumn: String,
        projectId: UUID,
        language: LanguageTag,
    ): Map<UUID, ResolvedLabelRow> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(
                    """
                    -- LEFT JOIN with a fallback row: a project-owned label may
                    -- not exist in the requested language, and a board that drops
                    -- the chip entirely would look like the category was removed.
                    SELECT r.$idColumn,
                           COALESCE(requested.name, any_name.name),
                           COALESCE(requested.language, any_name.language),
                           r.color
                    FROM $table r
                    LEFT JOIN ${table}_name requested
                        ON requested.$idColumn = r.$idColumn AND requested.language = :language
                    LEFT JOIN LATERAL (
                        SELECT n.name, n.language FROM ${table}_name n
                        WHERE n.$idColumn = r.$idColumn
                        ORDER BY n.language
                        LIMIT 1
                    ) any_name ON TRUE
                    WHERE r.project_id = :projectId
                    """,
                ).setParameter("projectId", projectId)
                .setParameter("language", language.value)
                .resultList as List<Array<Any?>>

        return rows.associate {
            (it[0] as UUID) to
                ResolvedLabelRow(
                    name = it[1] as String,
                    language = it[2] as String,
                    color = it[3] as String,
                )
        }
    }

    private fun <T> TypedQuery<T>.applyCriteria(criteria: TaskSearchCriteria) =
        setParameter("projectId", criteria.projectId)
            .setParameter("onlyActive", criteria.onlyActive)
            .setParameter("statusId", criteria.statusId)
            .setParameter("priority", criteria.priority)
            .setParameter("categoryId", criteria.categoryId)
            .setParameter("assigneeId", criteria.assigneeId)
            .setParameter("searchTerm", criteria.searchTerm)
}

internal data class ResolvedLabelRow(
    val name: String,
    val language: String,
    val color: String,
)
