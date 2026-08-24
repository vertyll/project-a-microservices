package com.vertyll.veds.task.infrastructure.config

import com.vertyll.veds.task.application.exception.ApiException
import com.vertyll.veds.task.domain.error.ErrorKind
import com.vertyll.veds.task.infrastructure.response.ApiResponse
import com.vertyll.veds.task.infrastructure.web.error.ErrorDetails
import com.vertyll.veds.task.infrastructure.web.error.ErrorHttpStatusMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["com.vertyll.veds.task"])
internal class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private companion object {
        private const val INVALID_VALUE = "Invalid value"
        private const val VALIDATION_FAILED = "Validation failed"
        private const val AN_UNEXPECTED_ERROR_OCCURRED = "An unexpected error occurred"
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ApiResponse<ErrorDetails>> {
        val status = ErrorHttpStatusMapper.toStatus(ex.error.kind)

        if (ex.error.kind == ErrorKind.MISCONFIGURED) {
            logger.error("Misconfiguration: {} params={}", ex.error.key, ex.params, ex)
        } else {
            logger.debug("Rejected request: {} params={}", ex.error.key, ex.params)
        }

        return ApiResponse.buildResponse(
            data = ErrorDetails(code = ex.error.key, params = ex.params),
            message = ex.error.key,
            status = status,
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
