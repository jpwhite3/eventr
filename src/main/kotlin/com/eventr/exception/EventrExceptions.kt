package com.eventr.exception

/**
 * Base exception for all Eventr application-specific exceptions.
 * Provides a consistent foundation for domain-specific error handling.
 */
sealed class EventrException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Validation-related exceptions for business rule violations.
 * Used when input data fails domain-specific validation beyond Bean Validation.
 */
class ValidationException(
    message: String,
    val field: String? = null,
    val code: String? = null
) : EventrException(message)

/**
 * Entity not found exceptions for resource lookup failures.
 * Provides specific entity type and identifier information for debugging.
 */
class EntityNotFoundException(
    val entity: String,
    val identifier: Any,
    message: String = "$entity not found with identifier: $identifier"
) : EventrException(message)

/**
 * Business rule violation exceptions for domain logic enforcement.
 * Used for operations that violate business constraints or workflows.
 */
class BusinessRuleException(
    message: String,
    val ruleCode: String? = null
) : EventrException(message)

/**
 * Authentication-related exceptions for security failures.
 * Used for login, token validation, and authorization failures.
 */
class AuthenticationException(
    message: String = "Authentication failed"
) : EventrException(message)

/**
 * Authorization exceptions for access control violations.
 * Used when authenticated users lack permissions for requested operations.
 */
class AuthorizationException(
    message: String = "Access denied",
    val resource: String? = null,
    val action: String? = null
) : EventrException(message)

/**
 * External service integration exceptions for third-party failures.
 * Used for AWS, email service, or other external system failures.
 */
class ExternalServiceException(
    val service: String,
    message: String = "External service unavailable: $service",
    cause: Throwable? = null
) : EventrException(message, cause)

/**
 * Data conflict exceptions for concurrent modification issues.
 * Used for optimistic locking failures and resource conflicts.
 */
class DataConflictException(
    message: String,
    val conflictType: String? = null,
    val resourceId: Any? = null
) : EventrException(message)

/**
 * Rate limiting exceptions for request throttling.
 * Used when clients exceed allowed request rates.
 */
class RateLimitException(
    message: String = "Request rate limit exceeded",
    val retryAfterSeconds: Long? = null
) : EventrException(message)

/**
 * Configuration exceptions for application setup issues.
 * Used for missing or invalid configuration during startup.
 */
class ConfigurationException(
    message: String,
    val configKey: String? = null
) : EventrException(message)