package com.vertyll.veds.iam.application

import com.vertyll.veds.iam.application.port.outbound.AuthEventPublisherPort
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.application.port.outbound.SagaProcessPort
import com.vertyll.veds.iam.application.port.outbound.UseCaseLogger
import com.vertyll.veds.iam.application.saga.model.Saga
import com.vertyll.veds.iam.application.saga.model.SagaStepNames
import com.vertyll.veds.iam.application.saga.model.SagaTypes
import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.model.VerificationToken
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import com.vertyll.veds.shared.saga.SagaStepStatus
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

internal fun user(
    id: Long = 1L,
    email: String = "ada@example.com",
    keycloakId: UUID? = UUID.randomUUID(),
    roles: Set<Role> = emptySet(),
) = User(id = id, keycloakId = keycloakId, email = email, firstName = "Ada", lastName = "Lovelace", roles = roles)

internal fun role(
    id: Long = 1L,
    name: String = "USER",
) = Role(id = id, name = name)

internal fun verificationToken(
    id: Long = 1L,
    token: String = "token-1",
    username: String = "ada@example.com",
    tokenType: String,
    used: Boolean = false,
    expiryDate: Instant = Instant.now().plus(24, ChronoUnit.HOURS),
    additionalData: String? = null,
    sagaId: String? = null,
) = VerificationToken(
    id = id,
    token = token,
    username = username,
    expiryDate = expiryDate,
    used = used,
    tokenType = tokenType,
    additionalData = additionalData,
    sagaId = sagaId,
)

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

    override fun deleteById(id: Long) {
        stored.remove(id)
    }
}

internal class InMemoryRoleRepository : RoleRepository {
    val stored = mutableListOf<Role>()

    fun given(vararg roles: Role) = roles.forEach { stored += it }

    override fun save(role: Role) = role.also { stored += it }

    override fun findById(id: Long) = stored.firstOrNull { it.id == id }

    override fun findByName(name: String) = stored.firstOrNull { it.name == name }

    override fun existsByName(name: String) = findByName(name) != null

    override fun findAll() = stored.toList()
}

internal class InMemoryVerificationTokenRepository : VerificationTokenRepository {
    val stored = linkedMapOf<Long, VerificationToken>()
    private var nextId = 1L

    fun given(vararg tokens: VerificationToken) = tokens.forEach { stored[it.id!!] = it }

    override fun save(verificationToken: VerificationToken): VerificationToken {
        val withId = verificationToken.id?.let { verificationToken } ?: verificationToken.copy(id = nextId++)
        stored[withId.id!!] = withId
        return withId
    }

    override fun findById(id: Long) = stored[id]

    override fun findByToken(token: String) = stored.values.firstOrNull { it.token == token }

    override fun findByUsernameAndTokenType(
        username: String,
        tokenType: String,
    ) = stored.values.firstOrNull { it.username == username && it.tokenType == tokenType }

    override fun findAllByUsernameAndTokenType(
        username: String,
        tokenType: String,
    ) = stored.values.filter { it.username == username && it.tokenType == tokenType }

    override fun findByAdditionalData(additionalData: String) = stored.values.firstOrNull { it.additionalData == additionalData }

    override fun deleteById(id: Long) {
        stored.remove(id)
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

    override fun sendMailRequestedEvent(
        to: String,
        subject: String,
        templateName: String,
        variables: Map<String, String>,
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

internal class RecordingSagaProcess : SagaProcessPort {
    val trail = mutableListOf<String>()
    private val sagas = linkedMapOf<String, Saga>()
    private var counter = 0

    override fun startSaga(
        sagaType: SagaTypes,
        payload: Map<String, Any?>,
    ): Saga =
        Saga(id = "saga-${++counter}", type = sagaType.value, payload = payload.toString())
            .also {
                sagas[it.id] = it
                trail += "start(${sagaType.value})"
            }

    override fun recordSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Map<String, Any?>,
    ) {
        trail += "step(${stepName.value},$status)"
    }

    override fun markSagaCompleted(sagaId: String) {
        trail += "completed"
    }

    override fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    ) {
        trail += "failed($errorMessage)"
    }

    override fun markAwaitingResponse(sagaId: String) {
        trail += "awaiting"
    }

    override fun findSagaDomainById(sagaId: String) = sagas[sagaId]
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
