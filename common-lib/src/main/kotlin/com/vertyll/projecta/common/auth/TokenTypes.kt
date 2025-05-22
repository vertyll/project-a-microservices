package com.vertyll.projecta.common.auth

enum class TokenTypes(val value: String) {
    ACCOUNT_ACTIVATION("ACCOUNT_ACTIVATION"),
    EMAIL_CHANGE("EMAIL_CHANGE"),
    PASSWORD_CHANGE_REQUEST("PASSWORD_CHANGE_REQUEST"),
    PASSWORD_RESET("PASSWORD_RESET");

    companion object {
        fun fromString(value: String): TokenTypes? {
            return TokenTypes.entries.find { it.value == value }
        }
    }
} 