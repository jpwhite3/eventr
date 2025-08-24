package com.eventr.service.webhook

import com.eventr.events.DomainEvent
import com.eventr.model.webhook.WebhookDelivery

/**
 * Interface for webhook delivery operations
 * Follows Single Responsibility Principle by focusing only on delivery
 */
interface WebhookDeliveryService {
    
    /**
     * Deliver a domain event to all relevant webhooks
     */
    fun deliverEvent(event: DomainEvent)
    
    /**
     * Retry failed webhook deliveries
     */
    fun retryFailedDeliveries()
    
    /**
     * Get delivery history for a webhook
     */
    fun getDeliveryHistory(webhookId: java.util.UUID): List<WebhookDelivery>
    
    /**
     * Get delivery statistics for a webhook
     */
    fun getDeliveryStatistics(webhookId: java.util.UUID): DeliveryStatistics
    
    /**
     * Manually retry a specific delivery
     */
    fun retryDelivery(deliveryId: java.util.UUID): WebhookDelivery
}

/**
 * Data class for delivery statistics
 */
data class DeliveryStatistics(
    val totalDeliveries: Long,
    val successfulDeliveries: Long,
    val failedDeliveries: Long,
    val pendingDeliveries: Long,
    val successRate: Double,
    val averageResponseTime: Long? = null
)