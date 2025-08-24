package com.eventr.service.webhook

import com.eventr.model.webhook.Webhook
import com.eventr.model.webhook.WebhookEventType
import com.eventr.model.webhook.WebhookStatus
import java.util.UUID

/**
 * Interface for webhook management operations
 * Follows Interface Segregation Principle by focusing only on webhook CRUD operations
 */
interface WebhookService {
    
    /**
     * Create a new webhook configuration
     */
    fun createWebhook(
        name: String,
        url: String,
        eventTypes: Set<WebhookEventType>,
        createdBy: String? = null,
        maxRetries: Int = 3,
        timeoutSeconds: Int = 30
    ): Webhook
    
    /**
     * Update an existing webhook
     */
    fun updateWebhook(
        webhookId: UUID,
        name: String? = null,
        url: String? = null,
        eventTypes: Set<WebhookEventType>? = null,
        status: WebhookStatus? = null,
        maxRetries: Int? = null,
        timeoutSeconds: Int? = null
    ): Webhook
    
    /**
     * Delete a webhook
     */
    fun deleteWebhook(webhookId: UUID)
    
    /**
     * Get webhook by ID
     */
    fun getWebhookById(webhookId: UUID): Webhook?
    
    /**
     * Get all webhooks
     */
    fun getAllWebhooks(): List<Webhook>
    
    /**
     * Get webhooks by status
     */
    fun getWebhooksByStatus(status: WebhookStatus): List<Webhook>
    
    /**
     * Get webhooks created by a specific user
     */
    fun getWebhooksByCreatedBy(createdBy: String): List<Webhook>
    
    /**
     * Get active webhooks that support a specific event type
     */
    fun getActiveWebhooksForEventType(eventType: WebhookEventType): List<Webhook>
    
    /**
     * Activate/deactivate a webhook
     */
    fun setWebhookStatus(webhookId: UUID, status: WebhookStatus): Webhook
    
    /**
     * Get webhook statistics
     */
    fun getWebhookStatistics(webhookId: UUID): WebhookStatistics
    
    /**
     * Regenerate webhook secret
     */
    fun regenerateSecret(webhookId: UUID): Webhook
}

/**
 * Data class for webhook statistics
 */
data class WebhookStatistics(
    val totalDeliveries: Long,
    val successfulDeliveries: Long,
    val failedDeliveries: Long,
    val successRate: Double,
    val lastDeliveryAt: String?,
    val lastSuccessAt: String?,
    val averageResponseTime: Long? = null
)