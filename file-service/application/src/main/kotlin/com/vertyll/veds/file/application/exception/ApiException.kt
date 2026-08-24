package com.vertyll.veds.file.application.exception

import com.vertyll.veds.file.domain.error.FileError

class ApiException(
    val error: FileError,
    val params: Map<String, Any> = emptyMap(),
) : RuntimeException(error.key)
