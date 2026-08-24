package com.vertyll.veds.notification.application.exception

import com.vertyll.veds.notification.domain.error.NotificationError

class ApiException(
    val error: NotificationError,
    val params: Map<String, Any> = emptyMap(),
) : RuntimeException(error.key)
