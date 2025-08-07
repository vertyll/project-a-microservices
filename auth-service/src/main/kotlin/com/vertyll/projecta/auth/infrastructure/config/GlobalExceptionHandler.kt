package com.vertyll.projecta.auth.infrastructure.config

import com.vertyll.projecta.auth.infrastructure.exception.ApiException
import com.vertyll.projecta.auth.infrastructure.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Global exception handler for auth service.
 * Provides consistent error responses across the auth service.
 */
@RestControllerAdvice(basePackages = ["com.vertyll.projecta.auth"])
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ApiResponse<Any>> {
        logger.error("API Exception: {}", ex.message)
        return ApiResponse.buildResponse(
            data = null,
            message = ex.message,
            status = ex.status
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<ApiResponse<Map<String, String>>> {
        logger.error("Validation Exception: {}", ex.message)

        val errors = ex.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }

        return ApiResponse.buildResponse(
            data = errors,
            message = "Validation failed",
            status = HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<Any>> {
        logger.error("Unhandled exception", ex)
        return ApiResponse.buildResponse(
            data = null,
            message = "An unexpected error occurred",
            status = HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}
