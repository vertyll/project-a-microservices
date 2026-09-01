package com.vertyll.veds.notification.infrastructure.config

import com.vertyll.veds.notification.application.exception.ApiException
import com.vertyll.veds.notification.domain.error.ErrorKind
import com.vertyll.veds.notification.infrastructure.response.ApiResponse
import com.vertyll.veds.notification.infrastructure.web.error.ErrorDetails
import com.vertyll.veds.notification.infrastructure.web.error.ErrorHttpStatusMapper
import com.vertyll.veds.notification.infrastructure.web.error.ValidationErrorDetails
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice(basePackages = ["com.vertyll.veds.notification"])
internal class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private companion object {
        private const val ACCESS_DENIED = "common.access_denied"
        private const val VALIDATION_FAILED = "common.validation_failed"
        private const val UNEXPECTED_ERROR = "common.unexpected_error"
        private const val INVALID_VALUE = "common.invalid_value"
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

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAccessDenied(ex: AuthorizationDeniedException): ResponseEntity<ApiResponse<ErrorDetails>> {
        logger.debug("Access denied: {}", ex.message)

        return ApiResponse.buildResponse(
            data = ErrorDetails(code = ACCESS_DENIED),
            message = ACCESS_DENIED,
            status = HttpStatus.FORBIDDEN,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<ValidationErrorDetails>> {
        val fields =
            ex.bindingResult.fieldErrors.associate { error ->
                error.field to (error.defaultMessage ?: INVALID_VALUE)
            }

        logger.debug("Validation rejected: {}", fields)

        return ApiResponse.buildResponse(
            data = ValidationErrorDetails(code = VALIDATION_FAILED, fields = fields),
            message = VALIDATION_FAILED,
            status = HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<ValidationErrorDetails>> {
        logger.debug("Unparseable request parameter '{}': {}", ex.name, ex.value)

        return ApiResponse.buildResponse(
            data = ValidationErrorDetails(code = VALIDATION_FAILED, fields = mapOf(ex.name to INVALID_VALUE)),
            message = VALIDATION_FAILED,
            status = HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<ErrorDetails>> {
        logger.error("Unhandled exception", ex)
        return ApiResponse.buildResponse(
            data = ErrorDetails(code = UNEXPECTED_ERROR),
            message = UNEXPECTED_ERROR,
            status = HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}
