package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.FakeIdentityProvider
import com.vertyll.veds.iam.application.InMemoryRoleRepository
import com.vertyll.veds.iam.application.InMemoryUserRepository
import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.role
import com.vertyll.veds.iam.application.user
import com.vertyll.veds.iam.domain.error.IamError
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Roles live in two places: this service's tables and Keycloak, which is what actually stamps them
 * into a token. A role granted here but not there is invisible at the gateway, so both have to move
 * together or not at all.
 */
internal class RoleCommandServiceTest {
    private val roles = InMemoryRoleRepository()
    private val users = InMemoryUserRepository()
    private val identity = FakeIdentityProvider()

    private val service = RoleCommandService(roles, users, identity)

    private val adminRole = role(id = 2L, name = "ADMIN").also { roles.given(it) }

    private fun givenUser(vararg granted: com.vertyll.veds.iam.domain.model.Role) =
        user(roles = granted.toSet()).copy(version = 0L).also { users.given(it) }

    @Test
    fun `granting a role records it locally and in the identity provider`() {
        val existing = givenUser()

        service.assignRoleToUser(existing.id!!, "ADMIN", version = 0L)

        assertTrue(users.findById(existing.id!!)!!.roles.contains(adminRole))
        assertTrue(identity.calls.contains("assignRole(${existing.keycloakId},ADMIN)"))
    }

    @Test
    fun `revoking a role removes it from both`() {
        val existing = givenUser(adminRole)

        service.removeRoleFromUser(existing.id!!, "ADMIN", version = 0L)

        assertTrue(users.findById(existing.id!!)!!.roles.isEmpty())
        assertTrue(identity.calls.contains("removeRole(${existing.keycloakId},ADMIN)"))
    }

    @Test
    fun `a role that does not exist cannot be granted`() {
        val existing = givenUser()

        val error = assertFailsWith<ApiException> { service.assignRoleToUser(existing.id!!, "NOT_A_ROLE", version = 0L) }

        assertEquals(IamError.ROLE_NOT_FOUND, error.error)
        assertTrue(identity.calls.isEmpty())
    }

    @Test
    fun `an unknown user cannot be granted a role`() {
        val error = assertFailsWith<ApiException> { service.assignRoleToUser(404L, "ADMIN", version = 0L) }

        assertEquals(IamError.USER_NOT_FOUND, error.error)
    }

    /** Two administrators editing the same account must not silently undo each other's grants. */
    @Test
    fun `a grant against a stale version is refused`() {
        val existing = givenUser()

        val error = assertFailsWith<ApiException> { service.assignRoleToUser(existing.id!!, "ADMIN", version = 9L) }

        assertEquals(IamError.VERSION_MISMATCH, error.error)
        assertTrue(users.findById(existing.id!!)!!.roles.isEmpty())
        assertTrue(identity.calls.isEmpty())
    }

    @Test
    fun `a revocation against a stale version is refused`() {
        val existing = givenUser(adminRole)

        assertFailsWith<ApiException> { service.removeRoleFromUser(existing.id!!, "ADMIN", version = 9L) }

        assertTrue(users.findById(existing.id!!)!!.roles.contains(adminRole))
        assertTrue(identity.calls.isEmpty())
    }

    /** Granting the same role twice must leave one grant, not two. */
    @Test
    fun `granting a role the user already holds changes nothing`() {
        val existing = givenUser(adminRole)

        service.assignRoleToUser(existing.id!!, "ADMIN", version = 0L)

        assertEquals(setOf(adminRole), users.findById(existing.id!!)!!.roles)
    }
}
