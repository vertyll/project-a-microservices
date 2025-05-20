package com.vertyll.projecta.common.kafka

object KafkaTopics {
    // Auth events
    const val USER_REGISTERED = "user-registered"
    const val USER_ACTIVATED = "user-activated"
    const val USER_PASSWORD_RESET = "user-password-reset"
    
    // Role events
    const val ROLE_CREATED = "role-created"
    const val ROLE_UPDATED = "role-updated"
    const val ROLE_DELETED = "role-deleted"
    const val ROLE_ASSIGNED = "role-assigned"
    const val ROLE_REVOKED = "role-revoked"
    
    // User events
    const val USER_UPDATED = "user-updated"
    const val USER_EMAIL_UPDATED = "user-email-updated"
    const val USER_PASSWORD_UPDATED = "user-password-updated"
    const val USER_DELETED = "user-deleted"
    const val CREDENTIALS_VERIFICATION = "credentials-verification"
    const val CREDENTIALS_VERIFICATION_RESULT = "credentials-verification-result"
    
    // Mail events
    const val MAIL_REQUESTED = "mail-requested"
    const val MAIL_SENT = "mail-sent"
    const val MAIL_FAILED = "mail-failed"
}
