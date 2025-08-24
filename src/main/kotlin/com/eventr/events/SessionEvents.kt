package com.eventr.events

import com.eventr.model.webhook.WebhookEventType
import java.time.LocalDateTime
import java.util.UUID

/**
 * Event fired when a session is created
 */
data class SessionCreatedEvent(
    override val aggregateId: UUID, // Session ID
    val relatedEventId: UUID,
    val sessionTitle: String,
    val sessionDescription: String?,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val maxAttendees: Int?,
    val resourceId: UUID? = null,
    val createdBy: String?,
    override val payload: Map<String, Any> = mapOf(
        "eventId" to relatedEventId,
        "sessionTitle" to sessionTitle,
        "sessionDescription" to (sessionDescription ?: ""),
        "startTime" to (startTime?.toString() ?: ""),
        "endTime" to (endTime?.toString() ?: ""),
        "maxAttendees" to (maxAttendees ?: 0),
        "resourceId" to (resourceId?.toString() ?: ""),
        "createdBy" to (createdBy ?: "")
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.SESSION_CREATED,
    aggregateId = aggregateId,
    payload = payload
)

/**
 * Event fired when a session is updated
 */
data class SessionUpdatedEvent(
    override val aggregateId: UUID, // Session ID
    val relatedEventId: UUID,
    val sessionTitle: String,
    val changes: Map<String, Any>,
    val updatedBy: String?,
    override val payload: Map<String, Any> = mapOf(
        "eventId" to relatedEventId,
        "sessionTitle" to sessionTitle,
        "changes" to changes,
        "updatedBy" to (updatedBy ?: "")
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.SESSION_UPDATED,
    aggregateId = aggregateId,
    payload = payload
)

/**
 * Event fired when a session is cancelled
 */
data class SessionCancelledEvent(
    override val aggregateId: UUID, // Session ID
    val relatedEventId: UUID,
    val sessionTitle: String,
    val cancellationReason: String?,
    val affectedAttendees: Int,
    val cancelledBy: String?,
    override val payload: Map<String, Any> = mapOf(
        "eventId" to relatedEventId,
        "sessionTitle" to sessionTitle,
        "cancellationReason" to (cancellationReason ?: ""),
        "affectedAttendees" to affectedAttendees,
        "cancelledBy" to (cancelledBy ?: "")
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.SESSION_CANCELLED,
    aggregateId = aggregateId,
    payload = payload
)