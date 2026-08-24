package com.vertyll.veds.iam.application.port.outbound

import com.vertyll.veds.iam.domain.model.RoleType
import java.util.UUID

interface IdentityProviderPort {
    fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        roleName: String = RoleType.USER.name,
    ): UUID

    fun enableUser(keycloakId: UUID)

    fun resetPassword(
        keycloakId: UUID,
        newPassword: String,
    )

    fun updateEmail(
        keycloakId: UUID,
        newEmail: String,
    )

    fun assignRole(
        keycloakUserId: String,
        roleName: String,
    )

    fun removeRole(
        keycloakUserId: String,
        roleName: String,
    )

    /**
     * Credential types the identity provider holds for this user, lower-cased —
     * `password`, `otp`, and so on.
     *
     * Read from Keycloak rather than stored here, because Keycloak owns
     * authentication. A copy in this database would be a second answer to the
     * same question, wrong the moment somebody configures a factor on Keycloak's
     * own pages.
     */
    fun credentialTypes(keycloakId: UUID): Set<String>

    /**
     * Removes one credential, used to turn a second factor off.
     *
     * A password cannot be removed this way: it is the only factor left, and
     * deleting it would lock the account out with no way back.
     */
    fun removeCredential(
        keycloakId: UUID,
        credentialType: String,
    )

    fun validatePassword(
        email: String,
        password: String,
    ): Boolean
}
