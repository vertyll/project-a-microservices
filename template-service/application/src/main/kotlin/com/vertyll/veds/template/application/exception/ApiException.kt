package com.vertyll.veds.template.application.exception

import com.vertyll.veds.template.domain.error.TemplateError

class ApiException(
    val error: TemplateError,
    val params: Map<String, Any> = emptyMap(),
) : RuntimeException(error.key)
