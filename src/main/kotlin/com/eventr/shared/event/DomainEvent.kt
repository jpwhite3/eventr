package com.eventr.shared.event

import java.time.LocalDateTime
import java.util.UUID

/**
 * Base interface for all domain events in the system.
 * Domain events represent something that happened in the domain that other parts
 * of the system might be interested in.
 */
interface DomainEvent {
    /** Unique identifier for this event instance */
    val eventId: UUID
    
    /** Type/name of the event (e.g., "UserRegistered", "EventPublished") */
    val eventType: String
    
    /** When the event occurred */
    val occurredAt: LocalDateTime
    
    /** ID of the aggregate/entity this event relates to */
    val aggregateId: UUID
    
    /** Additional event data as key-value pairs */
    val payload: Map<String, Any>
}

/**
 * Abstract base class for domain events with common functionality.
 * Extend this class when creating new domain events.
 */
abstract class BaseDomainEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val eventType: String,
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val aggregateId: UUID,
    override val payload: Map<String, Any> = emptyMap()
) : DomainEvent
