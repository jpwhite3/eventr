package com.eventr.modules.event.events

import com.eventr.shared.event.BaseDomainEvent
import java.time.LocalDateTime
import java.util.UUID

/**
 * Published when a new event is created.
 * Other modules might use this to initialize related data.
 */
data class EventCreated(
    override val aggregateId: UUID,  // Event ID
    val eventName: String,
    val organizerEmail: String?,
    val startDateTime: LocalDateTime?,
    val defaultInstanceId: UUID?  // The auto-created instance ID
) : BaseDomainEvent(
    eventType = "EventCreated",
    aggregateId = aggregateId,
    payload = mapOf(
        "eventName" to eventName,
        "organizerEmail" to (organizerEmail ?: ""),
        "startDateTime" to (startDateTime?.toString() ?: ""),
        "defaultInstanceId" to (defaultInstanceId?.toString() ?: "")
    )
)

/**
 * Published when an event is published (made visible to attendees).
 * Notification module might send announcements to subscribers.
 */
data class EventPublished(
    override val aggregateId: UUID,  // Event ID
    val eventName: String,
    val organizerEmail: String?,
    val startDateTime: LocalDateTime?
) : BaseDomainEvent(
    eventType = "EventPublished",
    aggregateId = aggregateId,
    payload = mapOf(
        "eventName" to eventName,
        "organizerEmail" to (organizerEmail ?: ""),
        "startDateTime" to (startDateTime?.toString() ?: "")
    )
)

/**
 * Published when event details are updated.
 * Notification module might notify registered attendees of changes.
 */
data class EventUpdated(
    override val aggregateId: UUID,  // Event ID
    val eventName: String,
    val changedFields: List<String>  // Which fields changed
) : BaseDomainEvent(
    eventType = "EventUpdated",
    aggregateId = aggregateId,
    payload = mapOf(
        "eventName" to eventName,
        "changedFields" to changedFields.joinToString(",")
    )
)

/**
 * Published when an event is cancelled.
 * Registration module should cancel all registrations.
 * Notification module should notify all registrants.
 */
data class EventCancelled(
    override val aggregateId: UUID,  // Event ID
    val eventName: String,
    val reason: String?,
    val organizerEmail: String?
) : BaseDomainEvent(
    eventType = "EventCancelled",
    aggregateId = aggregateId,
    payload = mapOf(
        "eventName" to eventName,
        "reason" to (reason ?: ""),
        "organizerEmail" to (organizerEmail ?: "")
    )
)

/**
 * Published when an event is deleted.
 */
data class EventDeleted(
    override val aggregateId: UUID,  // Event ID
    val eventName: String
) : BaseDomainEvent(
    eventType = "EventDeleted",
    aggregateId = aggregateId,
    payload = mapOf(
        "eventName" to eventName
    )
)
