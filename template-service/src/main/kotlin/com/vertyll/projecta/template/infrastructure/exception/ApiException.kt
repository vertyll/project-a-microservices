package com.vertyll.projecta.template.infrastructure.exception

import org.springframework.http.HttpStatus

class ApiException(
    message: String,
    val status: HttpStatus
) : RuntimeException(message)
