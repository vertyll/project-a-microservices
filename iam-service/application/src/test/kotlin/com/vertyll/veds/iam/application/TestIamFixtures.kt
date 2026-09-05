@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.iam.application

import com.vertyll.veds.iam.application.port.outbound.AuthEventPublisherPort
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.application.port.outbound.RolePermissionsEventPublisherPort
import com.vertyll.veds.iam.application.port.outbound.UseCaseLogger
import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.domain.model.Permission
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.repository.PermissionRepository
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal fun user(
    id: Long = 1L,
    email: String = "ada@example.com",
    keycloakId: UUID? = Uuid.generateV7().toJavaUuid(),
    roles: Set<Role> = emptySet(),
) = User(id = id, keycloakId = keycloakId, email = email, firstName = "Ada", lastName = "Lovelace", roles = roles)

internal fun role(
    id: Long = 1L,
    name: String = "USER",
    permissions: Set<Permission> = emptySet(),
    unrestricted: Boolean = false,
) = Role(id = id, name = name, permissions = permissions, unrestricted = unrestricted)

internal fun permission(
    id: Long = 1L,
    name: String = "TASKS_VIEW",
    module: String = "task",
    scope: RoleScope = RoleScope.PROJECT,
    description: String? = null,
) = Permission(id = id, name = name, module = module, scope = scope, description = description)

internal class InMemoryUserRepository : UserRepository {
    val stored = linkedMapOf<Long, User>()
    private var nextId = 1L

    fun given(vararg users: User) = users.forEach { stored[it.id!!] = it }

    override fun save(user: User): User {
        val withId = user.id?.let { user } ?: user.copy(id = nextId++)
        stored[withId.id!!] = withId
        return withId
    }

    override fun findById(id: Long) = stored[id]

    override fun findByEmail(email: String) = stored.values.firstOrNull { it.email == email }

    override fun findByKeycloakId(keycloakId: UUID) = stored.values.firstOrNull { it.keycloakId == keycloakId }

    override fun existsByEmail(email: String) = findByEmail(email) != null

    override fun findAll(pageRequest: PageRequest) =
        PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun search(
        term: String,
        pageRequest: PageRequest,
    ): PageResult<User> {
        val matching =
            stored.values.filter {
                it.email.contains(term, ignoreCase = true) ||
                    it.firstName.contains(term, ignoreCase = true) ||
                    it.lastName.contains(term, ignoreCase = true)
            }
        return PageResult(content = matching, page = 0, size = matching.size, totalElements = matching.size.toLong())
    }

    override fun countByRole(roleId: Long) = stored.values.count { user -> user.roles.any { it.id == roleId } }.toLong()

    override fun deleteById(id: Long) {
        stored.remove(id)
    }
}

internal class InMemoryRoleRepository : RoleRepository {
    val stored = mutableListOf<Role>()
    private var nextId = 100L

    fun given(vararg roles: Role) =
        roles.forEach {
            stored += it
            nextId = maxOf(nextId, (it.id ?: 0L) + 1)
        }

    override fun save(role: Role): Role {
        val withId = role.id?.let { role } ?: role.copy(id = nextId++)
        stored.removeAll { it.id == withId.id }
        stored += withId
        return withId
    }

    override fun findById(id: Long) = stored.firstOrNull { it.id == id }

    override fun findByName(name: String) = stored.firstOrNull { it.name == name }

    override fun existsByName(name: String) = findByName(name) != null

    override fun findAll() = stored.toList()

    override fun findAllByNames(names: Collection<String>) = stored.filter { it.name in names }

    override fun delete(role: Role) {
        stored.removeAll { it.id == role.id }
    }
}

internal class InMemoryPermissionRepository : PermissionRepository {
    val stored = mutableListOf<Permission>()
    private var nextId = 1L

    fun given(vararg permissions: Permission) =
        permissions.forEach {
            stored += it
            nextId = maxOf(nextId, (it.id ?: 0L) + 1)
        }

    override fun save(permission: Permission): Permission {
        val withId = permission.id?.let { permission } ?: permission.copy(id = nextId++)
        stored.removeAll { it.id == withId.id }
        stored += withId
        return withId
    }

    override fun findById(id: Long) = stored.firstOrNull { it.id == id }

    override fun findByName(name: String) = stored.firstOrNull { it.name == name }

    override fun existsByName(name: String) = findByName(name) != null

    override fun findAll() = stored.toList()

    override fun findByModule(module: String) = stored.filter { it.module == module }

    override fun findAllByNames(names: Collection<String>) = stored.filter { it.name in names }

    override fun delete(permission: Permission) {
        stored.removeAll { it.id == permission.id }
    }
}

internal class RecordingRolePermissionsPublisher : RolePermissionsEventPublisherPort {
    val changed = mutableListOf<Role>()
    val removed = mutableListOf<String>()

    override fun publishChanged(role: Role) {
        changed += role
    }

    override fun publishRemoved(
        roleName: String,
        scope: RoleScope,
    ) {
        removed += roleName
    }
}

internal class FakeIdentityProvider : IdentityProviderPort {
    val calls = mutableListOf<String>()
    var createUserFails: Exception? = null
    var validPasswords = mutableMapOf<String, String>()

    override fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        roleName: String,
    ): UUID {
        createUserFails?.let { throw it }
        calls += "createUser($email,$roleName)"
        return UUID.nameUUIDFromBytes(email.toByteArray())
    }

    override fun enableUser(keycloakId: UUID) {
        calls += "enableUser($keycloakId)"
    }

    override fun resetPassword(
        keycloakId: UUID,
        newPassword: String,
    ) {
        calls += "resetPassword($keycloakId)"
    }

    override fun updateEmail(
        keycloakId: UUID,
        newEmail: String,
    ) {
        calls += "updateEmail($keycloakId,$newEmail)"
    }

    override fun createRole(
        roleName: String,
        description: String?,
    ) {
        calls += "createRole($roleName)"
    }

    override fun deleteRole(roleName: String) {
        calls += "deleteRole($roleName)"
    }

    override fun assignRole(
        keycloakUserId: String,
        roleName: String,
    ) {
        calls += "assignRole($keycloakUserId,$roleName)"
    }

    override fun removeRole(
        keycloakUserId: String,
        roleName: String,
    ) {
        calls += "removeRole($keycloakUserId,$roleName)"
    }

    override fun credentialTypes(keycloakId: UUID) = setOf("password")

    override fun removeCredential(
        keycloakId: UUID,
        credentialType: String,
    ) {
        calls += "removeCredential($keycloakId,$credentialType)"
    }

    override fun validatePassword(
        email: String,
        password: String,
    ) = validPasswords[email] == password
}

internal class RecordingAuthEventPublisher : AuthEventPublisherPort {
    val published = mutableListOf<String>()

    override fun requestMail(
        to: String,
        templateName: String,
        variables: Map<String, String>,
        language: String,
        replyTo: String?,
        priority: Int,
        sagaId: String?,
    ) {
        published += "MailRequested($to,$templateName)"
    }

    override fun publishUserRegistered(
        userId: UUID,
        email: String,
        firstName: String?,
        lastName: String?,
    ) {
        published += "UserRegistered($email)"
    }

    override fun publishUserProfileUpdated(
        userId: UUID,
        email: String,
        firstName: String?,
        lastName: String?,
        avatarFileId: UUID?,
    ) {
        published += "UserProfileUpdated($email)"
    }
}

internal object SilentLogger : UseCaseLogger {
    override fun debug(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun info(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun warn(
        message: String,
        vararg args: Any?,
    ) = Unit

    override fun error(
        message: String,
        vararg args: Any?,
    ) = Unit
}
