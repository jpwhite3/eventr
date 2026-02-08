package com.eventr.modules.event.api.dto

import com.eventr.model.EventCategory
import com.eventr.model.EventStatus
import com.eventr.model.EventType
import java.time.LocalDateTime
import java.util.UUID

/**
 * DTO for creating a new event
 */
data class CreateEventRequest(
    val name: String,
    val description: String? = null,
    val eventType: EventType = EventType.IN_PERSON,
    val category: EventCategory? = null,
    val bannerImageUrl: String? = null,
    val thumbnailImageUrl: String? = null,
    val tags: List<String>? = null,
    val capacity: Int? = null,
    val waitlistEnabled: Boolean = false,
    
    // Location
    val venueName: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val country: String? = null,
    
    // Virtual
    val virtualUrl: String? = null,
    val dialInNumber: String? = null,
    val accessCode: String? = null,
    
    // Registration
    val requiresApproval: Boolean = false,
    val maxRegistrations: Int? = null,
    
    // Organizer
    val organizerName: String? = null,
    val organizerEmail: String? = null,
    val organizerPhone: String? = null,
    val organizerWebsite: String? = null,
    
    // Timing
    val startDateTime: LocalDateTime? = null,
    val endDateTime: LocalDateTime? = null,
    val timezone: String = "UTC",
    
    // Content
    val agenda: String? = null
)

/**
 * DTO for updating an existing event
 */
data class UpdateEventRequest(
    val name: String? = null,
    val description: String? = null,
    val eventType: EventType? = null,
    val category: EventCategory? = null,
    val bannerImageUrl: String? = null,
    val thumbnailImageUrl: String? = null,
    val tags: List<String>? = null,
    val capacity: Int? = null,
    val waitlistEnabled: Boolean? = null,
    val venueName: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val country: String? = null,
    val virtualUrl: String? = null,
    val dialInNumber: String? = null,
    val accessCode: String? = null,
    val requiresApproval: Boolean? = null,
    val maxRegistrations: Int? = null,
    val organizerName: String? = null,
    val organizerEmail: String? = null,
    val organizerPhone: String? = null,
    val organizerWebsite: String? = null,
    val startDateTime: LocalDateTime? = null,
    val endDateTime: LocalDateTime? = null,
    val timezone: String? = null,
    val agenda: String? = null
)

/**
 * DTO for event response
 */
data class EventResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val status: EventStatus,
    val eventType: EventType,
    val category: EventCategory?,
    val bannerImageUrl: String?,
    val thumbnailImageUrl: String?,
    val tags: List<String>,
    val capacity: Int?,
    val waitlistEnabled: Boolean,
    val venueName: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val country: String?,
    val virtualUrl: String?,
    val dialInNumber: String?,
    val accessCode: String?,
    val requiresApproval: Boolean,
    val maxRegistrations: Int?,
    val organizerName: String?,
    val organizerEmail: String?,
    val organizerPhone: String?,
    val organizerWebsite: String?,
    val startDateTime: LocalDateTime?,
    val endDateTime: LocalDateTime?,
    val timezone: String?,
    val agenda: String?,
    val instances: List<EventInstanceResponse>
)

/**
 * DTO for event instance response
 */
data class EventInstanceResponse(
    val id: UUID,
    val startDateTime: LocalDateTime?,
    val endDateTime: LocalDateTime?,
    val location: String?,
    val capacity: Int?,
    val registrationCount: Int = 0
)

/**
 * Filter criteria for finding events
 */
data class EventFilterCriteria(
    val search: String? = null,
    val city: String? = null,
    val category: EventCategory? = null,
    val eventType: EventType? = null,
    val publishedOnly: Boolean = true,
    val sortBy: String = "startDateTime",
    val sortDirection: String = "asc"
)
