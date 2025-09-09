package com.eventr.dto

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Standardized error response format for all API endpoints.
 * Provides consistent error information to clients for better integration.
 * 
 * Features:
 * - Timestamp for request tracking
 * - HTTP status code and error type
 * - User-friendly message (safe for display)
 * - Request path for debugging
 * - Field-specific validation errors
 * - Request ID for support tracking
 */
data class ErrorResponse(
    val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val requestId: String? = null,
    val errors: List<FieldError> = emptyList()
)

/**
 * Field-specific validation error for detailed client feedback.
 * Used for Bean Validation (@Valid) errors to provide specific field information.
 */
data class FieldError(
    val field: String,
    val rejectedValue: Any?,
    val message: String
)

/**
 * Business validation error for domain-specific validation failures.
 * Used for custom business rule violations that don't map to specific fields.
 */
data class BusinessError(
    val code: String,
    val message: String,
    val details: Map<String, Any> = emptyMap()
)