package com.vertyll.projecta.auth.domain.enums

enum class TokenType(val value: String) {
    ACCOUNT_ACTIVATION("ACCOUNT_ACTIVATION"),
    EMAIL_CHANGE("EMAIL_CHANGE"),
    PASSWORD_CHANGE_REQUEST("PASSWORD_CHANGE_REQUEST"),
    PASSWORD_RESET("PASSWORD_RESET");

    companion object {
        fun fromString(value: String): TokenType? {
            return TokenType.entries.find { it.value == value }
        }
    }
}
