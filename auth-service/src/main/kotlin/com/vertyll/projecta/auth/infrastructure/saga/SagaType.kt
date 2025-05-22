package com.vertyll.projecta.auth.infrastructure.saga

enum class SagaType(val value: String) {
    USER_REGISTRATION("UserRegistration"),
    EMAIL_CHANGE("EmailChange"),
    PASSWORD_CHANGE("PasswordChange");
    
    companion object {
        fun fromString(value: String): SagaType? = SagaType.entries.find { it.value == value }
    }
}