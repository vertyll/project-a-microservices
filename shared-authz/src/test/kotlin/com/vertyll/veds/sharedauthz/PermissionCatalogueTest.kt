package com.vertyll.veds.sharedauthz

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PermissionCatalogueTest {
    @Test
    fun `a catalogue keeps the order its permissions were declared in`() {
        val catalogue =
            permissions("task") {
                permission("VIEW_TASKS")
                permission("MANAGE_TASKS")
                permission("COMMENT")
            }

        assertEquals(listOf("VIEW_TASKS", "MANAGE_TASKS", "COMMENT"), catalogue.names.toList())
    }

    @Test
    fun `declaring the same permission twice is refused`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                permissions("task") {
                    permission("VIEW_TASKS")
                    permission("VIEW_TASKS")
                }
            }

        assertTrue(error.message!!.contains("duplicate"))
    }

    @Test
    fun `a permission name must be upper snake case`() {
        assertFailsWith<IllegalArgumentException> { permissions("task") { permission("viewTasks") } }
        assertFailsWith<IllegalArgumentException> { permissions("task") { permission("VIEW-TASKS") } }
    }

    @Test
    fun `a module must declare at least one permission`() {
        assertFailsWith<IllegalArgumentException> { permissions("task") {} }
    }
}

internal class RolePermissionsTest {
    @Test
    fun `a role grants what it was given`() {
        val role = RolePermissions(role = "MEMBER", module = "task", permissions = setOf("VIEW_TASKS"))

        assertTrue(role.grants("VIEW_TASKS"))
        assertFalse(role.grants("MANAGE_TASKS"))
    }

    @Test
    fun `an unrestricted role grants a permission nobody listed`() {
        val role = RolePermissions(role = "ADMINISTRATOR", module = "task", permissions = emptySet(), unrestricted = true)

        assertTrue(role.grants("A_PERMISSION_ADDED_NEXT_YEAR"))
    }
}
