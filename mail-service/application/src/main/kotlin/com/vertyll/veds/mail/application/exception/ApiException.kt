package com.vertyll.veds.mail.application.exception

import com.vertyll.veds.mail.domain.error.MailError

class ApiException(
    val error: MailError,
    val params: Map<String, Any> = emptyMap(),
) : RuntimeException(error.key)
