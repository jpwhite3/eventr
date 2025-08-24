package com.eventr.events

/**
 * Interface for publishing domain events
 * Following the Single Responsibility Principle and Dependency Inversion Principle
 */
interface EventPublisher {
    /**
     * Publishes a domain event to all registered listeners
     */
    fun publish(event: DomainEvent)
    
    /**
     * Publishes multiple domain events
     */
    fun publishAll(events: Collection<DomainEvent>) {
        events.forEach { publish(it) }
    }
}