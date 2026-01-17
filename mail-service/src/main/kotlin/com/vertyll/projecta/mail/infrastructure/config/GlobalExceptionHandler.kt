package com.vertyll.projecta.mail.infrastructure.config

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.vertyll.projecta.mail.infrastructure.exception.ApiException
import com.vertyll.projecta.mail.infrastructure.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Global exception handler for mail service.
 * Provides consistent error responses across the mail service.
 */
@RestControllerAdvice(basePackages = ["com.vertyll.projecta.mail"])
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private companion object {
        private const val INVALID_VALUE = "Invalid value"
        private const val VALIDATION_FAILED = "Validation failed"
        private const val AN_UNEXPECTED_ERROR_OCCURRED = "An unexpected error occurred"
        private const val INVALID_REQUEST_BODY = "Invalid request body"
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ApiResponse<Any>> {
        logger.error("API Exception: {}", ex.message)
        return ApiResponse.buildResponse(
            data = null,
            message = ex.message,
            status = ex.status,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Map<String, String>>> {
        logger.error("Message not readable: {}", ex.message)

        val cause = ex.cause
        if (cause is InvalidFormatException) {
            val fieldName = cause.path.joinToString(".") { it.fieldName }
            val invalidValue = cause.value?.toString() ?: "null"
            val targetType = cause.targetType.simpleName

            val errorMessage =
                if (cause.targetType.isEnum) {
                    val enumValues = cause.targetType.enumConstants.joinToString(", ") { it.toString() }
                    "Invalid value '$invalidValue' for field '$fieldName'. Accepted values are: $enumValues"
                } else {
                    "Invalid value '$invalidValue' for field '$fieldName'. Expected type: $targetType"
                }

            return ApiResponse.buildResponse(
                data = mapOf(fieldName to errorMessage),
                message = INVALID_REQUEST_BODY,
                status = HttpStatus.BAD_REQUEST,
            )
        }

        return ApiResponse.buildResponse(
            data = mapOf("error" to (ex.message ?: INVALID_REQUEST_BODY)),
            message = INVALID_REQUEST_BODY,
            status = HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Map<String, String>>> {
        logger.error("Validation Exception: {}", ex.message)

        val errors =
            ex.bindingResult.fieldErrors.associate { error ->
                error.field to (error.defaultMessage ?: INVALID_VALUE)
            }

        return ApiResponse.buildResponse(
            data = errors,
            message = VALIDATION_FAILED,
            status = HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<Any>> {
        logger.error("Unhandled exception", ex)
        return ApiResponse.buildResponse(
            data = null,
            message = AN_UNEXPECTED_ERROR_OCCURRED,
            status = HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}
