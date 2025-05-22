package com.vertyll.projecta.common.event

enum class EventSource(val value: String) {
    AUTH_SERVICE("AUTH_SERVICE"),
    USER_SERVICE("USER_SERVICE"),
    ROLE_SERVICE("ROLE_SERVICE"),
    MAIL_SERVICE("MAIL_SERVICE");

    companion object {
        fun fromString(value: String): EventSource? {
            return EventSource.entries.find { it.value == value }
        }
    }
}
