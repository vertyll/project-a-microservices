package com.vertyll.veds.task.application.exception

import com.vertyll.veds.task.domain.error.TaskError

class ApiException(
    val error: TaskError,
    val params: Map<String, Any> = emptyMap(),
) : RuntimeException(error.key)
