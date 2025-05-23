package com.vertyll.projecta.role.domain.model

enum class SagaTypes(val value: String) {
    // Role related sagas
    ROLE_CREATION("RoleCreation"),
    ROLE_ASSIGNMENT("RoleAssignment"),
    ROLE_REVOCATION("RoleRevocation"),
    ROLE_UPDATE("RoleUpdate"),
    ROLE_DELETION("RoleDeletion");

    companion object {
        fun fromString(value: String): SagaTypes? {
            return SagaTypes.entries.find { it.value == value }
        }
    }
} 