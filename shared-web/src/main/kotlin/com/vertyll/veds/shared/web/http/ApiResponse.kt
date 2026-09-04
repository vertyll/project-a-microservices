package com.vertyll.veds.shared.web.http

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

/**
 * The envelope every service answers in: the payload, a translation key naming what
 * happened, and when. The key is resolved by the caller against translation-service,
 * so an API answers the same way whichever language the reader wants.
 */
open class BaseResponse<T>(
    open val data: T?,
    open val message: String,
    open val timestamp: LocalDateTime = LocalDateTime.now(),
)

class ApiResponse<T> private constructor(
    data: T?,
    message: String,
    timestamp: LocalDateTime,
) : BaseResponse<T>(data, message, timestamp) {
    companion object {
        fun <T> buildResponse(
            data: T?,
            message: String?,
            status: HttpStatus,
        ): ResponseEntity<ApiResponse<T>> =
            ResponseEntity(
                ApiResponse(data = data, message = message ?: "", timestamp = LocalDateTime.now()),
                status,
            )
    }
}
