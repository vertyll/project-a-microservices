package com.vertyll.projecta.user.domain.model.enums

/**
 * Defines compensation actions for user service sagas
 */
enum class SagaCompensationActions(val value: String) {
    DELETE_USER_PROFILE("DELETE_USER_PROFILE"),
    REVERT_USER_PROFILE_UPDATE("REVERT_USER_PROFILE_UPDATE"),
    REVERT_USER_CREDENTIALS_UPDATE("REVERT_USER_CREDENTIALS_UPDATE"),
    REVERT_USER_EMAIL_UPDATE("REVERT_USER_EMAIL_UPDATE"),
    RECREATE_USER_PROFILE("RECREATE_USER_PROFILE");

    companion object {
        fun fromString(value: String): SagaCompensationActions? {
            return SagaCompensationActions.entries.find { it.value == value }
        }
    }
}
