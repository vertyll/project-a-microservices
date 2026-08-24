package com.vertyll.veds.iam.infrastructure.kafka

internal object IamKafkaTopics {
    const val USER_REGISTERED = "user-registered"
    const val USER_PROFILE_UPDATED = "user-profile-updated"

    const val MAIL_REQUESTED = "mail-requested"
    const val MAIL_SENT = "mail-sent"
    const val MAIL_FAILED = "mail-failed"
}
