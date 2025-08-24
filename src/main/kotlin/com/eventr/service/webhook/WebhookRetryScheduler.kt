package com.eventr.service.webhook

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled task for retrying failed webhook deliveries
 * Can be disabled via configuration property
 */
@Component
@ConditionalOnProperty(
    value = ["webhook.retry.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class WebhookRetryScheduler(
    private val webhookDeliveryService: WebhookDeliveryService
) {
    
    private val logger = LoggerFactory.getLogger(WebhookRetryScheduler::class.java)
    
    /**
     * Retry failed deliveries every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    fun retryFailedDeliveries() {
        try {
            logger.debug("Starting scheduled retry of failed webhook deliveries")
            webhookDeliveryService.retryFailedDeliveries()
            logger.debug("Completed scheduled retry of failed webhook deliveries")
        } catch (e: Exception) {
            logger.error("Error during scheduled webhook retry", e)
        }
    }
}