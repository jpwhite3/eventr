package com.eventr.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Secure logging utility that prevents accidental exposure of sensitive data in application logs.
 * 
 * This class provides structured logging methods that ensure:
 * - No sensitive data (tokens, passwords, emails) is logged in plaintext
 * - Consistent logging format across the application
 * - Proper user identification using UUIDs instead of PII
 * - Compliance with data protection regulations (GDPR, SOC 2)
 * 
 * Usage:
 * ```kotlin
 * private val secureLogger = SecureLogger(AuthService::class.java)
 * 
 * // Log authentication events
 * secureLogger.logSecurityEvent("EMAIL_VERIFICATION_SENT", user.id, true)
 * 
 * // Log user actions
 * secureLogger.logUserAction(user.id, "PROFILE_UPDATED", "Basic information")
 * 
 * // Log email events
 * secureLogger.logEmailEvent(user.id, "REGISTRATION_CONFIRMATION", true)
 * ```
 * 
 * @param logger The SLF4J logger instance to use for actual logging
 */
class SecureLogger(private val logger: Logger) {
    
    constructor(clazz: Class<*>) : this(LoggerFactory.getLogger(clazz))
    
    /**
     * Log user actions without exposing sensitive data.
     * 
     * @param userId User UUID for identification (not PII)
     * @param action The action performed (e.g., "LOGIN", "PROFILE_UPDATED")
     * @param details Optional non-sensitive details about the action
     */
    fun logUserAction(userId: UUID, action: String, details: String? = null) {
        logger.info("User action - ID: {}, Action: {}${details?.let { ", Details: {}" } ?: ""}", 
                   userId, action, details)
    }
    
    /**
     * Log security-related events for auditing and monitoring.
     * 
     * @param eventType Type of security event (e.g., "EMAIL_VERIFICATION_SENT", "PASSWORD_RESET_REQUESTED")
     * @param userId Optional user UUID associated with the event
     * @param success Whether the security event was successful
     * @param details Optional non-sensitive details
     */
    fun logSecurityEvent(eventType: String, userId: UUID? = null, success: Boolean, details: String? = null) {
        logger.info("Security event - Type: {}, Success: {}${userId?.let { ", User: {}" } ?: ""}${details?.let { ", Details: {}" } ?: ""}", 
                   eventType, success, userId, details)
    }
    
    /**
     * Log email-related events without exposing email addresses.
     * 
     * @param userId User UUID for identification
     * @param emailType Type of email (e.g., "REGISTRATION_CONFIRMATION", "PASSWORD_RESET")
     * @param success Whether the email operation was successful
     * @param details Optional non-sensitive details
     */
    fun logEmailEvent(userId: UUID, emailType: String, success: Boolean, details: String? = null) {
        logger.info("Email event - User: {}, Type: {}, Success: {}${details?.let { ", Details: {}" } ?: ""}", 
                   userId, emailType, success, details)
    }
    
    /**
     * Log system events that don't involve user data.
     * 
     * @param eventType Type of system event
     * @param success Whether the system event was successful  
     * @param details Optional non-sensitive details
     */
    fun logSystemEvent(eventType: String, success: Boolean, details: String? = null) {
        logger.info("System event - Type: {}, Success: {}${details?.let { ", Details: {}" } ?: ""}", 
                   eventType, success, details)
    }
    
    /**
     * Log error events without exposing sensitive data in stack traces.
     * 
     * @param eventType Type of error event
     * @param userId Optional user UUID if error is user-specific
     * @param error The exception (will be logged with stack trace in debug mode)
     * @param safeMessage A safe message that doesn't contain sensitive data
     */
    fun logErrorEvent(eventType: String, userId: UUID? = null, error: Throwable? = null, safeMessage: String? = null) {
        val message = "Error event - Type: {}${userId?.let { ", User: {}" } ?: ""}${safeMessage?.let { ", Message: {}" } ?: ""}"
        
        if (error != null) {
            if (logger.isDebugEnabled) {
                // Include full stack trace in debug mode for development
                logger.error(message, eventType, userId, safeMessage, error)
            } else {
                // Only log error message in production to avoid noise
                logger.error("$message, Error: {}", eventType, userId, safeMessage, error.message)
            }
        } else {
            logger.error(message, eventType, userId, safeMessage)
        }
    }
    
    /**
     * Log debug information safely - only in development environments.
     * This method should be used sparingly and never contain sensitive data.
     * 
     * @param message Debug message (must not contain sensitive data)
     * @param args Optional message arguments
     */
    fun logDebugSafe(message: String, vararg args: Any?) {
        if (logger.isDebugEnabled) {
            logger.debug(message, *args)
        }
    }
    
    /**
     * Check if debug logging is enabled.
     * Use this to avoid expensive operations when debug logging is disabled.
     */
    fun isDebugEnabled(): Boolean = logger.isDebugEnabled
    
    /**
     * Get the underlying SLF4J logger for cases where direct access is needed.
     * Use with caution to ensure no sensitive data is logged.
     */
    fun getLogger(): Logger = logger
}