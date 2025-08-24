package com.eventr.events

import com.eventr.model.webhook.WebhookEventType
import java.time.LocalDateTime
import java.util.UUID

/**
 * Base interface for all domain events in the system
 */
interface DomainEvent {
    val eventId: UUID
    val eventType: WebhookEventType
    val occurredAt: LocalDateTime
    val aggregateId: UUID
    val payload: Map<String, Any>
}

/**
 * Abstract base class for domain events with common functionality
 */
abstract class BaseDomainEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val eventType: WebhookEventType,
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val aggregateId: UUID,
    override val payload: Map<String, Any>
) : DomainEvent