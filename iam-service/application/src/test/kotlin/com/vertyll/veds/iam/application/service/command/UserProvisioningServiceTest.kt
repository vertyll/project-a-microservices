package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.InMemoryRoleRepository
import com.vertyll.veds.iam.application.InMemoryUserRepository
import com.vertyll.veds.iam.application.RecordingAuthEventPublisher
import com.vertyll.veds.iam.application.dto.AuthenticatedIdentity
import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleType
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

internal class UserProvisioningServiceTest {
    private val users = InMemoryUserRepository()
    private val roles = InMemoryRoleRepository()
    private val events = RecordingAuthEventPublisher()

    private val service = UserProvisioningService(users, roles, events)

    private val identity =
        AuthenticatedIdentity(
            keycloakId = UUID.randomUUID(),
            email = "keycloak-native@example.com",
            firstName = "Keycloak",
            lastName = "Native",
        )

    private fun givenDefaultRole() {
        roles.save(Role(id = 1L, name = RoleType.USER.value))
    }

    /** Registration happens in Keycloak now, so first contact is what makes the user local. */
    @Test
    fun `an identity unknown to iam is recorded with the default role`() {
        givenDefaultRole()

        service.provision(identity)

        val stored = users.findByKeycloakId(identity.keycloakId)
        assertNotNull(stored)
        assertEquals(identity.email, stored.email)
        assertEquals(listOf(RoleType.USER.value), stored.roles.map { it.name })
    }

    /** Other services build their user projections from this event, exactly as for a registration. */
    @Test
    fun `recording a new identity announces it`() {
        givenDefaultRole()

        service.provision(identity)

        assertEquals(listOf("UserRegistered(${identity.email})"), events.published)
    }

    @Test
    fun `provisioning the same identity twice changes nothing`() {
        givenDefaultRole()

        service.provision(identity)
        service.provision(identity)

        assertEquals(1, users.findAll(PageRequest(0, 10)).totalElements.toInt())
        assertEquals(1, events.published.size)
    }

    /** Falling back to a role-less user would hand out an account that silently cannot do anything. */
    @Test
    fun `a missing default role is refused rather than provisioning a role-less user`() {
        val error = assertFailsWith<ApiException> { service.provision(identity) }

        assertEquals(IamError.DEFAULT_ROLE_NOT_CONFIGURED, error.error)
        assertEquals(0, events.published.size)
    }
}
