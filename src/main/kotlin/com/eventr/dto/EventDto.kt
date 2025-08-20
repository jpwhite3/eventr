package com.eventr.dto

import com.eventr.model.EventCategory
import com.eventr.model.EventStatus
import com.eventr.model.EventType
import java.time.LocalDateTime
import java.util.UUID

data class EventDto(
    var id: UUID? = null,
    var name: String? = null,
    var description: String? = null,
    var status: EventStatus? = null,
    var bannerImageUrl: String? = null,
    var thumbnailImageUrl: String? = null,
    var tags: List<String>? = null,
    var capacity: Int? = null,
    var waitlistEnabled: Boolean? = null,
    var instances: List<EventInstanceDto>? = null,
    
    // Event type and category
    var eventType: EventType? = null,
    var category: EventCategory? = null,
    
    // Location fields for in-person events
    var venueName: String? = null,
    var address: String? = null,
    var city: String? = null,
    var state: String? = null,
    var zipCode: String? = null,
    var country: String? = null,
    
    // Virtual event fields
    var virtualUrl: String? = null,
    var dialInNumber: String? = null,
    var accessCode: String? = null,
    
    // Registration settings
    var requiresApproval: Boolean = false,
    var maxRegistrations: Int? = null,
    
    // Organizer information
    var organizerName: String? = null,
    var organizerEmail: String? = null,
    var organizerPhone: String? = null,
    var organizerWebsite: String? = null,
    
    // Event timing
    var startDateTime: LocalDateTime? = null,
    var endDateTime: LocalDateTime? = null,
    var timezone: String? = null,
    
    // Agenda/Schedule
    var agenda: String? = null
)
