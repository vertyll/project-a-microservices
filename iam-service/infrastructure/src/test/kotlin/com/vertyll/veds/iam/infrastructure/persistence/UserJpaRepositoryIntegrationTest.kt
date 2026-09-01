package com.vertyll.veds.iam.infrastructure.persistence

import com.vertyll.veds.iam.infrastructure.IntegrationTestBase
import com.vertyll.veds.iam.infrastructure.persistence.entity.UserJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.RoleJpaRepository
import com.vertyll.veds.iam.infrastructure.persistence.repository.UserJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The entity graph names `roles.permissions`; `permissions` alone is not an attribute of
 * [UserJpaEntity], and Hibernate rejects such a query outright rather than merely fetching
 * lazily — so every lookup below fails at runtime if the path regresses.
 */
internal class UserJpaRepositoryIntegrationTest
    @Autowired
    constructor(
        private val users: UserJpaRepository,
        private val roles: RoleJpaRepository,
    ) : IntegrationTestBase() {
        private val keycloakId: UUID = UUID.randomUUID()

        private companion object {
            private const val EMAIL = "entity-graph-probe@example.com"
        }

        @BeforeEach
        fun resetSharedState() {
            users.findByEmail(EMAIL).ifPresent { users.delete(it) }
        }

        private fun persistUser(): UserJpaEntity =
            users.save(
                UserJpaEntity(
                    keycloakId = keycloakId,
                    email = EMAIL,
                    firstName = "Entity",
                    lastName = "Graph",
                    roles = mutableSetOf(roles.findAll().first()),
                ),
            )

        @Test
        @Transactional
        fun `a user is loaded by email with roles and their permissions`() {
            persistUser()

            val found = users.findByEmail(EMAIL)

            assertTrue(found.isPresent)
            assertEquals(1, found.get().roles.size)
            assertNotNull(
                found
                    .get()
                    .roles
                    .first()
                    .permissions,
            )
        }

        @Test
        @Transactional
        fun `a user is loaded by keycloak id`() {
            persistUser()

            assertTrue(users.findByKeycloakId(keycloakId).isPresent)
        }

        @Test
        @Transactional
        fun `a user is loaded by id`() {
            val saved = persistUser()

            assertTrue(users.findById(saved.id!!).isPresent)
        }

        @Test
        @Transactional
        fun `users are listed page by page`() {
            persistUser()

            assertTrue(users.findAll(PageRequest.of(0, 10)).totalElements >= 1)
        }
    }
