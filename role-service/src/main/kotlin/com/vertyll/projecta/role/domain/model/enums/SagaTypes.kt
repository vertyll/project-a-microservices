package com.vertyll.projecta.role.domain.model.enums

enum class SagaTypes(val value: String) {
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
