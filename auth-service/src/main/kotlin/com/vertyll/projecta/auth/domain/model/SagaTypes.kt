package com.vertyll.projecta.auth.domain.model

enum class SagaTypes(val value: String) {
    // User related sagas
    USER_REGISTRATION("UserRegistration"),

    // Email related sagas
    EMAIL_CHANGE("EmailChange"),
    EMAIL_VERIFICATION("EmailVerification"),

    // Password related sagas
    PASSWORD_CHANGE("PasswordChange"),
    PASSWORD_RESET("PasswordReset");

    companion object {
        fun fromString(value: String): SagaTypes? {
            return SagaTypes.entries.find { it.value == value }
        }
    }
}
