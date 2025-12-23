package com.vertyll.projecta.role.domain.model.enums

/**
 * Defines compensation actions for role service sagas
 */
enum class SagaCompensationActions(
    val value: String,
) {
    DELETE_ROLE("DELETE_ROLE"),
    REVOKE_ROLE("REVOKE_ROLE"),
    ASSIGN_ROLE("ASSIGN_ROLE"),
    REVERT_ROLE_UPDATE("REVERT_ROLE_UPDATE"),
    RECREATE_ROLE("RECREATE_ROLE"),
    ;

    companion object {
        fun fromString(value: String): SagaCompensationActions? = SagaCompensationActions.entries.find { it.value == value }
    }
}
