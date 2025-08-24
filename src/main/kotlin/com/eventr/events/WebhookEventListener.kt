package com.eventr.events

import com.eventr.service.webhook.WebhookDeliveryService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Event listener that processes domain events and triggers webhook deliveries
 * Uses Spring's event system and follows the Observer pattern
 */
@Component
class WebhookEventListener(
    private val webhookDeliveryService: WebhookDeliveryService
) {
    
    private val logger = LoggerFactory.getLogger(WebhookEventListener::class.java)
    
    /**
     * Listen for all domain events and trigger webhook deliveries
     * Async to avoid blocking the main thread
     */
    @Async
    @EventListener
    fun handleDomainEvent(event: DomainEvent) {
        logger.debug("Received domain event: {} with ID: {}", event.eventType, event.eventId)
        
        try {
            webhookDeliveryService.deliverEvent(event)
        } catch (e: Exception) {
            logger.error("Failed to process webhook deliveries for event: {} with ID: {}", 
                event.eventType, event.eventId, e)
            // Don't re-throw to avoid affecting the main business logic
        }
    }
}