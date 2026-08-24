package com.vertyll.veds.mail.infrastructure.web.error

import com.vertyll.veds.mail.domain.error.ErrorKind
import org.springframework.http.HttpStatus

internal object ErrorHttpStatusMapper {
    fun toStatus(kind: ErrorKind): HttpStatus =
        when (kind) {
            ErrorKind.NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorKind.ACCESS_DENIED -> HttpStatus.FORBIDDEN
            ErrorKind.CONFLICT -> HttpStatus.CONFLICT
            ErrorKind.INVALID -> HttpStatus.BAD_REQUEST
            ErrorKind.PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED
            ErrorKind.GONE -> HttpStatus.GONE
            ErrorKind.MISCONFIGURED -> HttpStatus.INTERNAL_SERVER_ERROR
        }
}
