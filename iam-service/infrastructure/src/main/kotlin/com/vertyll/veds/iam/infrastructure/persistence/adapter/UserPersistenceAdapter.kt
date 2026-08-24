package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.RoleJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.UserJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.RoleJpaRepository
import com.vertyll.veds.iam.infrastructure.persistence.repository.UserJpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class UserPersistenceAdapter(
    private val repository: UserJpaRepository,
    private val roleJpaRepository: RoleJpaRepository,
) : UserRepository {
    override fun save(user: User): User {
        // Re-read as managed entities so the join table is the only thing this write
        // touches. A role that cannot be found is an error, not something to drop:
        // silently saving a user with fewer roles than asked for is a privilege
        // change nobody requested and nobody can see.
        val managedRoles: MutableSet<RoleJpaEntity> =
            user.roles
                .map { role ->
                    val id = role.id ?: error("cannot assign an unsaved role '${'$'}{role.name}' to a user")
                    roleJpaRepository
                        .findById(id)
                        .orElseThrow { IllegalStateException("role ${'$'}id no longer exists") }
                }.toMutableSet()
        val entity =
            UserJpaEntity(
                id = user.id,
                keycloakId = user.keycloakId,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                roles = managedRoles,
                avatarFileId = user.avatarFileId,
                phoneNumber = user.phoneNumber,
                address = user.address,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                version = user.version,
            )
        return repository.save(entity).toDomain()
    }

    override fun findById(id: Long): User? = repository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? = repository.findByEmail(email).orElse(null)?.toDomain()

    override fun findByKeycloakId(keycloakId: UUID): User? = repository.findByKeycloakId(keycloakId).orElse(null)?.toDomain()

    override fun existsByEmail(email: String): Boolean = repository.existsByEmail(email)

    override fun findAll(pageRequest: DomainPageRequest): PageResult<User> {
        val springPage =
            repository.findAll(SpringPageRequest.of(pageRequest.page, pageRequest.size))
        return PageResult(
            content = springPage.content.map { it.toDomain() },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = springPage.totalElements,
        )
    }

    override fun deleteById(id: Long) {
        repository.deleteById(id)
    }
}

private fun UserJpaEntity.toDomain(): User =
    User(
        id = this.id,
        keycloakId = this.keycloakId,
        email = this.email,
        firstName = this.firstName,
        lastName = this.lastName,
        roles = this.roles.map { it.toDomain() }.toSet(),
        avatarFileId = this.avatarFileId,
        phoneNumber = this.phoneNumber,
        address = this.address,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
