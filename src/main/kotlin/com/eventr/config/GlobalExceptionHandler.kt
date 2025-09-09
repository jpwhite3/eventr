package com.eventr.config

import com.eventr.dto.ErrorResponse
import com.eventr.dto.FieldError
import com.eventr.exception.*
import com.eventr.util.SecureLogger
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.*

/**
 * Global exception handler for all REST API endpoints.
 * Provides consistent error response format and proper HTTP status codes.
 * 
 * Features:
 * - Standardized error response format
 * - Proper HTTP status code mapping
 * - Detailed validation error messages
 * - Security-aware error messaging (no information disclosure)
 * - Comprehensive exception coverage
 * - Centralized error logging
 */
@ControllerAdvice
class GlobalExceptionHandler {

    private val secureLogger = SecureLogger(GlobalExceptionHandler::class.java)

    /**
     * Handle Bean Validation (@Valid) failures.
     * Returns detailed field-level validation errors for client feedback.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map { error ->
            FieldError(
                field = error.field,
                rejectedValue = error.rejectedValue,
                message = error.defaultMessage ?: "Validation failed"
            )
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = "Request validation failed. Please check your input and try again.",
            path = request.requestURI,
            requestId = generateRequestId(),
            errors = fieldErrors
        )

        secureLogger.logErrorEvent("VALIDATION_FAILED", null, null, 
            "Bean validation failed for ${fieldErrors.size} fields")

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Handle method argument binding failures.
     * Returns validation errors for request binding issues.
     */
    @ExceptionHandler(BindException::class)
    fun handleBindExceptions(
        ex: BindException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.fieldErrors.map { error ->
            FieldError(
                field = error.field,
                rejectedValue = error.rejectedValue,
                message = error.defaultMessage ?: "Binding failed"
            )
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Request Binding Failed",
            message = "Request parameter binding failed. Please check your input format.",
            path = request.requestURI,
            requestId = generateRequestId(),
            errors = fieldErrors
        )

        secureLogger.logErrorEvent("BINDING_FAILED", null, ex, 
            "Request binding failed for ${fieldErrors.size} fields")

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Handle constraint violation exceptions (direct validation).
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolations(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.constraintViolations.map { violation ->
            FieldError(
                field = violation.propertyPath.toString(),
                rejectedValue = violation.invalidValue,
                message = violation.message
            )
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Constraint Violation",
            message = "Input constraints violated. Please check your data.",
            path = request.requestURI,
            requestId = generateRequestId(),
            errors = fieldErrors
        )

        secureLogger.logErrorEvent("CONSTRAINT_VIOLATION", null, ex,
            "Constraint violations for ${fieldErrors.size} fields")

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Handle custom validation exceptions from business logic.
     */
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(
        ex: ValidationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = if (ex.field != null) {
            listOf(FieldError(
                field = ex.field,
                rejectedValue = null,
                message = ex.message ?: "Validation failed"
            ))
        } else emptyList()

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Business Validation Failed",
            message = ex.message ?: "Validation failed",
            path = request.requestURI,
            requestId = generateRequestId(),
            errors = fieldErrors
        )

        secureLogger.logErrorEvent("BUSINESS_VALIDATION_FAILED", null, ex,
            "Business validation failed: ${ex.field ?: "general"}")

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Handle entity not found exceptions.
     */
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(
        ex: EntityNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Resource Not Found",
            message = "The requested resource could not be found.",
            path = request.requestURI,
            requestId = generateRequestId()
        )

        secureLogger.logErrorEvent("ENTITY_NOT_FOUND", null, null,
            "Entity not found: ${ex.entity}")

        return ResponseEntity.notFound().build<ErrorResponse>().let {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
        }
    }

    /**
     * Handle business rule violations.
     */
    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRule(
        ex: BusinessRuleException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = "Business Rule Violation",
            message = ex.message ?: "Business rule violation",
            path = request.requestURI,
            requestId = generateRequestId()
        )

        secureLogger.logErrorEvent("BUSINESS_RULE_VIOLATION", null, ex,
            "Business rule violation: ${ex.ruleCode ?: "unknown"}")

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    /**
     * Handle authentication failures with security-aware messaging.
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Authentication Required",
            message = "Authentication credentials are required or invalid.",
            path = request.requestURI,
            requestId = generateRequestId()
        )

        secureLogger.logSecurityEvent("AUTHENTICATION_FAILED", null, false,
            "Authentication failed for request")

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    /**
     * Handle authorization failures.
     */
    @ExceptionHandler(AuthorizationException::class)
    fun handleAuthorization(
        ex: AuthorizationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Access Denied",
            message = "You do not have permission to access this resource.",
            path = request.requestURI,
            requestId = generateRequestId()
        )

        secureLogger.logSecurityEvent("AUTHORIZATION_FAILED", null, false,
            "Authorization failed: ${ex.resource ?: "unknown resource"}")

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    /**
     * Handle external service failures.
     */
    @ExceptionHandler(ExternalServiceException::class)
    fun handleExternalService(
        ex: ExternalServiceException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            error = "Service Unavailable",
            message = "An external service is temporarily unavailable. Please try again later.",
            path = request.requestURI,
            requestId = generateRequestId()
        )

        secureLogger.logErrorEvent("EXTERNAL_SERVICE_FAILED", null, ex,
            "External service failure: ${ex.service}")

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }

    /**
     * Handle data conflicts (optimistic locking, etc.).
     */
    @ExceptionHandler(DataConflictException::class)
    fun handleDataConflict(
        ex: DataConflictException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = "Data Conflict",
            message = "The resource has been modified by another request. Please refresh and try again.",
            path = request.requestURI,
            requestId = generateRequestId()
        )

        secureLogger.logErrorEvent("DATA_CONFLICT", null, ex,
            "Data conflict: ${ex.conflictType ?: "unknown"}")

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    /**
     * Handle HTTP method not supported.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.METHOD_NOT_ALLOWED.value(),
            error = "Method Not Allowed",
            message = "HTTP method '${ex.method}' is not supported for this endpoint.",
            path = request.requestURI,
            requestId = generateRequestId()
        )

        secureLogger.logErrorEvent("METHOD_NOT_SUPPORTED", null, null,
            "Method not supported: ${ex.method}")

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse)
    }

    /**
     * Handle malformed JSON or request body issues.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Malformed Request",
            message = "Request body is malformed or unreadable. Please check your JSON format.",
            path = request.requestURI,
            requestId = generateRequestId()
        )

        secureLogger.logErrorEvent("MALFORMED_REQUEST", null, ex,
            "Malformed request body")

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Handle missing required parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldError = FieldError(
            field = ex.parameterName,
            rejectedValue = null,
            message = "Required parameter '${ex.parameterName}' is missing"
        )

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Missing Parameter",
            message = "Required parameter '${ex.parameterName}' is missing.",
            path = request.requestURI,
            requestId = generateRequestId(),
            errors = listOf(fieldError)
        )

        secureLogger.logErrorEvent("MISSING_PARAMETER", null, null,
            "Missing required parameter: ${ex.parameterName}")

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Handle parameter type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldError = FieldError(
            field = ex.name,
            rejectedValue = ex.value,
            message = "Parameter '${ex.name}' has invalid format"
        )

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Invalid Parameter Format",
            message = "Parameter '${ex.name}' has an invalid format.",
            path = request.requestURI,
            requestId = generateRequestId(),
            errors = listOf(fieldError)
        )

        secureLogger.logErrorEvent("TYPE_MISMATCH", null, null,
            "Type mismatch for parameter: ${ex.name}")

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Handle generic IllegalArgumentException (legacy support).
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Invalid Request",
            message = ex.message ?: "Request contains invalid data",
            path = request.requestURI,
            requestId = generateRequestId()
        )

        secureLogger.logErrorEvent("ILLEGAL_ARGUMENT", null, ex,
            "Illegal argument exception")

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Handle all other unexpected exceptions.
     * Returns generic error message to prevent information disclosure.
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val requestId = generateRequestId()
        
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred. Please try again later.",
            path = request.requestURI,
            requestId = requestId
        )

        // Log full exception details for debugging (not exposed to client)
        secureLogger.logErrorEvent("UNEXPECTED_ERROR", null, ex,
            "Unexpected error - Request ID: $requestId")

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    /**
     * Generate unique request ID for error tracking and support.
     */
    private fun generateRequestId(): String {
        return UUID.randomUUID().toString().substring(0, 8).uppercase()
    }
}