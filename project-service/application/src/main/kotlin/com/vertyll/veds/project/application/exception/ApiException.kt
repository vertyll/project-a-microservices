package com.vertyll.veds.project.application.exception

import com.vertyll.veds.project.domain.error.ProjectError

class ApiException(
    val error: ProjectError,
    val params: Map<String, Any> = emptyMap(),
) : RuntimeException(error.key)
