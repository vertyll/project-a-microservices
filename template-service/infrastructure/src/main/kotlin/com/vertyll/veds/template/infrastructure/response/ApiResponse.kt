package com.vertyll.veds.template.infrastructure.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

internal class ApiResponse<T> private constructor(
    data: T?,
    message: String,
    timestamp: LocalDateTime,
) : BaseResponse<T>(data, message, timestamp) {
    companion object {
        fun <T> buildResponse(
            data: T?,
            message: String?,
            status: HttpStatus,
        ): ResponseEntity<ApiResponse<T>> {
            val response =
                ApiResponse(
                    data = data,
                    message = message ?: "",
                    timestamp = LocalDateTime.now(),
                )

            return ResponseEntity(response, status)
        }
    }
}
