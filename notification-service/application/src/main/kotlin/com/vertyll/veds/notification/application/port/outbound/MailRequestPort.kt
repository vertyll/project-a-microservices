package com.vertyll.veds.notification.application.port.outbound

import com.vertyll.veds.notification.domain.model.NotificationType

interface MailRequestPort {
    fun requestMail(
        to: String,
        type: NotificationType,
        params: Map<String, String>,
    )
}
