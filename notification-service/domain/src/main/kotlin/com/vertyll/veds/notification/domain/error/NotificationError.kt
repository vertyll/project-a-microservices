package com.vertyll.veds.notification.domain.error

enum class NotificationError(
    val key: String,
    val kind: ErrorKind,
) {
    NOTIFICATION_NOT_FOUND("notification.not_found", ErrorKind.NOT_FOUND),
    NOT_THE_RECIPIENT("notification.not_the_recipient", ErrorKind.ACCESS_DENIED),
    RECIPIENT_UNKNOWN("notification.recipient_unknown", ErrorKind.NOT_FOUND),

    VERSION_MISMATCH("common.version_mismatch", ErrorKind.PRECONDITION_FAILED),
    NOT_AUTHENTICATED("common.not_authenticated", ErrorKind.ACCESS_DENIED),
    TOKEN_CLAIM_MISSING("common.token_claim_missing", ErrorKind.ACCESS_DENIED),
}
