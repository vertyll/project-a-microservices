@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.domain.model

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class WorkLogEntryTest {
    private val author = Uuid.generateV7().toJavaUuid()
    private val someoneElse = Uuid.generateV7().toJavaUuid()
    private val projectId = Uuid.generateV7().toJavaUuid()

    private fun entry(hidden: Boolean) =
        WorkLogEntry.create(
            taskId = Uuid.generateV7().toJavaUuid(),
            authorId = author,
            minutes = 60,
            workedOn = LocalDate.of(2026, 9, 2),
            hidden = hidden,
        )

    private fun project(
        enabled: Boolean = false,
        roles: Set<String> = emptySet(),
    ) = ProjectRef(
        projectId = projectId,
        name = "Apollo",
        hiddenWorkLogEnabled = enabled,
        hiddenWorkLogRoles = roles,
    )

    @Test
    fun `a normal entry is visible to everyone who may see the task`() {
        assertTrue(entry(hidden = false).isVisibleTo(someoneElse, project(), "MEMBER"))
    }

    @Test
    fun `a hidden entry stays visible to its own author`() {
        assertTrue(entry(hidden = true).isVisibleTo(author, project(), "MEMBER"))
    }

    @Test
    fun `a hidden entry is invisible to anyone else by default`() {
        assertFalse(entry(hidden = true).isVisibleTo(someoneElse, project(), "MEMBER"))
    }

    @Test
    fun `a hidden entry opens up to a role the project allows`() {
        val configured = project(enabled = true, roles = setOf("MANAGER"))

        assertTrue(entry(hidden = true).isVisibleTo(someoneElse, configured, "MANAGER"))
        assertFalse(entry(hidden = true).isVisibleTo(someoneElse, configured, "MEMBER"))
    }

    @Test
    fun `listed roles grant nothing while the feature is off`() {
        val stale = project(enabled = false, roles = setOf("MANAGER"))

        assertFalse(entry(hidden = true).isVisibleTo(someoneElse, stale, "MANAGER"))
    }

    @Test
    fun `a member with no role in the project never reads hidden entries`() {
        val configured = project(enabled = true, roles = setOf("MANAGER"))

        assertFalse(entry(hidden = true).isVisibleTo(someoneElse, configured, null))
    }
}
