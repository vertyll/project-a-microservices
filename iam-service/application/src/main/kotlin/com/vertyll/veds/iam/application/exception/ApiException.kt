package com.vertyll.veds.iam.application.exception

import com.vertyll.veds.iam.domain.error.IamError

class ApiException(
    val error: IamError,
    val params: Map<String, Any> = emptyMap(),
) : RuntimeException(error.key)
