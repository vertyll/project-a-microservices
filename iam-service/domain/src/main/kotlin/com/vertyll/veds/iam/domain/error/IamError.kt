package com.vertyll.veds.iam.domain.error

enum class IamError(
    val key: String,
    val kind: ErrorKind,
) {
    USER_NOT_FOUND("iam.user.not_found", ErrorKind.NOT_FOUND),
    USER_VERSION_MISMATCH("iam.user.version_mismatch", ErrorKind.PRECONDITION_FAILED),
    EMAIL_NOT_CHANGEABLE("iam.user.email_not_changeable", ErrorKind.CONFLICT),
    MISSING_NEW_EMAIL_DATA("iam.user.missing_new_email_data", ErrorKind.INVALID),

    // Deliberately one code for both "no such user" and "wrong password".
    // Separating them is a user-enumeration oracle.
    INVALID_CREDENTIALS("iam.auth.invalid_credentials", ErrorKind.ACCESS_DENIED),
    INVALID_CURRENT_PASSWORD("iam.auth.invalid_current_password", ErrorKind.ACCESS_DENIED),
    INVALID_CONFIRMATION_CODE("iam.auth.invalid_confirmation_code", ErrorKind.INVALID),
    INVALID_TOKEN("iam.auth.invalid_token", ErrorKind.INVALID),
    INVALID_TOKEN_ID("iam.auth.invalid_token_id", ErrorKind.INVALID),
    TOKEN_EXPIRED_OR_USED("iam.auth.token_expired_or_used", ErrorKind.GONE),
    REGISTRATION_FAILED("iam.auth.registration_failed", ErrorKind.INVALID),

    USER_ALREADY_EXISTS("iam.user.already_exists", ErrorKind.CONFLICT),
    IDENTITY_PROVIDER_FAILED("iam.identity_provider.failed", ErrorKind.MISCONFIGURED),

    ROLE_NOT_FOUND("iam.role.not_found", ErrorKind.NOT_FOUND),
    DEFAULT_ROLE_NOT_CONFIGURED("iam.role.default_not_configured", ErrorKind.MISCONFIGURED),

    VERSION_MISMATCH("common.version_mismatch", ErrorKind.PRECONDITION_FAILED),
    NOT_AUTHENTICATED("common.not_authenticated", ErrorKind.ACCESS_DENIED),
    TOKEN_CLAIM_MISSING("common.token_claim_missing", ErrorKind.ACCESS_DENIED),
}
