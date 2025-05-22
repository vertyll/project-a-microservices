package com.vertyll.projecta.common.saga

enum class SagaTypes(val value: String) {
    // User related sagas
    USER_REGISTRATION("UserRegistration"),
    USER_DELETION("UserDeletion"),
    USER_UPDATE("UserUpdate"),
    
    // Email related sagas
    EMAIL_CHANGE("EmailChange"),
    
    // Password related sagas
    PASSWORD_CHANGE("PasswordChange"),
    PASSWORD_RESET("PasswordReset"),
    
    // Role related sagas
    ROLE_ASSIGNMENT("RoleAssignment"),
    ROLE_REVOCATION("RoleRevocation");
    
    companion object {
        fun fromString(value: String): SagaTypes? {
            return SagaTypes.entries.find { it.value == value }
        }
    }
} 