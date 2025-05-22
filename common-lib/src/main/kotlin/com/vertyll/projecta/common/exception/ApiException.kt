package com.vertyll.projecta.common.exception

import org.springframework.http.HttpStatus

class ApiException(
    message: String,
    val status: HttpStatus
) : RuntimeException(message)
