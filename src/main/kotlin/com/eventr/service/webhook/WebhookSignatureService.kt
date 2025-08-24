package com.eventr.service.webhook

/**
 * Interface for webhook signature operations
 * Follows Single Responsibility Principle by focusing only on security
 */
interface WebhookSignatureService {
    
    /**
     * Generate HMAC signature for webhook payload
     */
    fun generateSignature(payload: String, secret: String): String
    
    /**
     * Validate webhook signature
     */
    fun validateSignature(payload: String, signature: String, secret: String): Boolean
    
    /**
     * Generate a new webhook secret
     */
    fun generateSecret(): String
}