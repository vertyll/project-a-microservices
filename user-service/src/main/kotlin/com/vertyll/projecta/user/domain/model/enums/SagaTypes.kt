package com.vertyll.projecta.user.domain.model.enums

enum class SagaTypes(
    val value: String,
) {
    USER_REGISTRATION("UserRegistration"),
    USER_DELETION("UserDeletion"),
    USER_UPDATE("UserUpdate"),
    USER_PASSWORD_CHANGE("UserPasswordChange"),
    USER_EMAIL_CHANGE("UserEmailChange"),
    USER_PROFILE_UPDATE("UserProfileUpdate"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = SagaTypes.entries.find { it.value == value }
    }
}
