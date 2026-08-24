package com.vertyll.veds.translation.application.exception

import com.vertyll.veds.translation.domain.error.TranslationError

class ApiException(
    val error: TranslationError,
    val params: Map<String, Any> = emptyMap(),
) : RuntimeException(error.key)
