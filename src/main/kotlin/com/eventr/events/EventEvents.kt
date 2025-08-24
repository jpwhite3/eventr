package com.eventr.events

import com.eventr.model.webhook.WebhookEventType
import java.time.LocalDateTime
import java.util.UUID

/**
 * Event fired when an event is created
 */
data class EventCreatedEvent(
    override val aggregateId: UUID, // Event ID
    val eventName: String,
    val eventDescription: String?,
    val startsAt: LocalDateTime?,
    val endsAt: LocalDateTime?,
    val venue: String?,
    val maxCapacity: Int?,
    val createdBy: String?,
    override val payload: Map<String, Any> = mapOf(
        "eventName" to eventName,
        "eventDescription" to (eventDescription ?: ""),
        "startsAt" to (startsAt?.toString() ?: ""),
        "endsAt" to (endsAt?.toString() ?: ""),
        "venue" to (venue ?: ""),
        "maxCapacity" to (maxCapacity ?: 0),
        "createdBy" to (createdBy ?: "")
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.EVENT_CREATED,
    aggregateId = aggregateId,
    payload = payload
)

/**
 * Event fired when an event is updated
 */
data class EventUpdatedEvent(
    override val aggregateId: UUID, // Event ID
    val eventName: String,
    val changes: Map<String, Any>,
    val updatedBy: String?,
    override val payload: Map<String, Any> = mapOf(
        "eventName" to eventName,
        "changes" to changes,
        "updatedBy" to (updatedBy ?: "")
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.EVENT_UPDATED,
    aggregateId = aggregateId,
    payload = payload
)

/**
 * Event fired when an event is cancelled
 */
data class EventCancelledEvent(
    override val aggregateId: UUID, // Event ID
    val eventName: String,
    val cancellationReason: String?,
    val affectedRegistrations: Int,
    val cancelledBy: String?,
    override val payload: Map<String, Any> = mapOf(
        "eventName" to eventName,
        "cancellationReason" to (cancellationReason ?: ""),
        "affectedRegistrations" to affectedRegistrations,
        "cancelledBy" to (cancelledBy ?: "")
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.EVENT_CANCELLED,
    aggregateId = aggregateId,
    payload = payload
)