package com.eventr.shared.event

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Spring implementation of EventPublisher using ApplicationEventPublisher.
 * This follows the Adapter pattern to integrate with Spring's event system.
 * 
 * Events published through this class will be delivered to any @EventListener
 * methods that accept the event type.
 */
@Component
class SpringEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : EventPublisher {
    
    private val logger = LoggerFactory.getLogger(SpringEventPublisher::class.java)
    
    override fun publish(event: DomainEvent) {
        try {
            logger.debug("Publishing domain event: {} with ID: {}", event.eventType, event.eventId)
            applicationEventPublisher.publishEvent(event)
            logger.debug("Successfully published domain event: {}", event.eventType)
        } catch (e: Exception) {
            logger.error("Failed to publish domain event: {} with ID: {}", 
                event.eventType, event.eventId, e)
            throw e
        }
    }
}
