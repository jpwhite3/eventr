package com.eventr.events

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Spring implementation of EventPublisher using ApplicationEventPublisher
 * This follows the Adapter pattern to integrate with Spring's event system
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
            // Re-throw to let the caller handle the failure
            throw e
        }
    }
}