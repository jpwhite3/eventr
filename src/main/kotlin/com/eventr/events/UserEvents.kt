package com.eventr.events

import com.eventr.model.webhook.WebhookEventType
import java.time.LocalDateTime
import java.util.UUID

/**
 * Event fired when a user registers for an event
 */
data class UserRegisteredEvent(
    override val aggregateId: UUID, // Registration ID
    val relatedEventId: UUID,
    val userEmail: String,
    val userName: String,
    val registrationStatus: String,
    override val payload: Map<String, Any> = mapOf(
        "eventId" to relatedEventId,
        "userEmail" to userEmail,
        "userName" to userName,
        "registrationStatus" to registrationStatus
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.USER_REGISTERED,
    aggregateId = aggregateId,
    payload = payload
)

/**
 * Event fired when a user cancels their registration
 */
data class UserCancelledEvent(
    override val aggregateId: UUID, // Registration ID
    val relatedEventId: UUID,
    val userEmail: String,
    val userName: String,
    val cancellationReason: String? = null,
    override val payload: Map<String, Any> = mapOf(
        "eventId" to relatedEventId,
        "userEmail" to userEmail,
        "userName" to userName,
        "cancellationReason" to (cancellationReason ?: "")
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.USER_CANCELLED,
    aggregateId = aggregateId,
    payload = payload
)

/**
 * Event fired when a user checks in to an event or session
 */
data class UserCheckedInEvent(
    override val aggregateId: UUID, // CheckIn ID
    val registrationId: UUID,
    val relatedEventId: UUID,
    val sessionId: UUID? = null,
    val userEmail: String,
    val checkInMethod: String,
    val location: String? = null,
    override val payload: Map<String, Any> = mapOf(
        "registrationId" to registrationId,
        "eventId" to relatedEventId,
        "sessionId" to (sessionId?.toString() ?: ""),
        "userEmail" to userEmail,
        "checkInMethod" to checkInMethod,
        "location" to (location ?: "")
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.USER_CHECKED_IN,
    aggregateId = aggregateId,
    payload = payload
)

/**
 * Event fired when a user checks out from an event or session
 */
data class UserCheckedOutEvent(
    override val aggregateId: UUID, // CheckIn ID
    val registrationId: UUID,
    val relatedEventId: UUID,
    val sessionId: UUID? = null,
    val userEmail: String,
    val checkInDurationMinutes: Long,
    override val payload: Map<String, Any> = mapOf(
        "registrationId" to registrationId,
        "eventId" to relatedEventId,
        "sessionId" to (sessionId?.toString() ?: ""),
        "userEmail" to userEmail,
        "checkInDurationMinutes" to checkInDurationMinutes
    )
) : BaseDomainEvent(
    eventType = WebhookEventType.USER_CHECKED_OUT,
    aggregateId = aggregateId,
    payload = payload
)