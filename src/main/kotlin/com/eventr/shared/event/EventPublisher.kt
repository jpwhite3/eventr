package com.eventr.shared.event

/**
 * Interface for publishing domain events.
 * Modules use this to announce state changes that other modules may be interested in.
 * 
 * Following the Single Responsibility Principle and Dependency Inversion Principle,
 * this interface decouples event producers from the underlying event infrastructure.
 */
interface EventPublisher {
    /**
     * Publishes a domain event to all registered listeners.
     * 
     * @param event The domain event to publish
     */
    fun publish(event: DomainEvent)
    
    /**
     * Publishes multiple domain events in order.
     * 
     * @param events Collection of events to publish
     */
    fun publishAll(events: Collection<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
