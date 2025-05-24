package com.vertyll.projecta.user.domain.model

enum class SagaTypes(val value: String) {
    USER_REGISTRATION("UserRegistration"),
    USER_DELETION("UserDeletion"),
    USER_UPDATE("UserUpdate"),
    USER_PASSWORD_CHANGE("UserPasswordChange"),
    USER_EMAIL_CHANGE("UserEmailChange");

    companion object {
        fun fromString(value: String): SagaTypes? {
            return SagaTypes.entries.find { it.value == value }
        }
    }
}
