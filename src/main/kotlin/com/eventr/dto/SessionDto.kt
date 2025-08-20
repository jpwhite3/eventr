package com.eventr.dto

import com.eventr.model.SessionType
import java.time.LocalDateTime
import java.util.*

data class SessionDto(
    var id: UUID? = null,
    var eventId: UUID? = null,
    var title: String = "",
    var description: String? = null,
    var type: SessionType = SessionType.PRESENTATION,
    var startTime: LocalDateTime? = null,
    var endTime: LocalDateTime? = null,
    var location: String? = null,
    var room: String? = null,
    var building: String? = null,
    var capacity: Int? = null,
    var isRegistrationRequired: Boolean = true,
    var isWaitlistEnabled: Boolean = true,
    
    // Presenter information
    var presenter: String? = null,
    var presenterTitle: String? = null,
    var presenterBio: String? = null,
    var presenterEmail: String? = null,
    
    // Session materials
    var materialUrl: String? = null,
    var recordingUrl: String? = null,
    var slidesUrl: String? = null,
    
    // Session details
    var prerequisites: String? = null,
    var targetAudience: String? = null,
    var difficultyLevel: String? = null,
    var tags: List<String>? = null,
    
    // Registration statistics
    var registeredCount: Int = 0,
    var waitlistCount: Int = 0,
    var attendedCount: Int = 0,
    var availableSpots: Int = 0,
    
    var isActive: Boolean = true,
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
)

data class SessionCreateDto(
    var eventId: UUID,
    var title: String,
    var description: String? = null,
    var type: SessionType = SessionType.PRESENTATION,
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,
    var location: String? = null,
    var room: String? = null,
    var building: String? = null,
    var capacity: Int? = null,
    var isRegistrationRequired: Boolean = true,
    var isWaitlistEnabled: Boolean = true,
    
    var presenter: String? = null,
    var presenterTitle: String? = null,
    var presenterBio: String? = null,
    var presenterEmail: String? = null,
    
    var materialUrl: String? = null,
    var prerequisites: String? = null,
    var targetAudience: String? = null,
    var difficultyLevel: String? = null,
    var tags: List<String>? = null
)

data class SessionUpdateDto(
    var title: String? = null,
    var description: String? = null,
    var type: SessionType? = null,
    var startTime: LocalDateTime? = null,
    var endTime: LocalDateTime? = null,
    var location: String? = null,
    var room: String? = null,
    var building: String? = null,
    var capacity: Int? = null,
    var isRegistrationRequired: Boolean? = null,
    var isWaitlistEnabled: Boolean? = null,
    
    var presenter: String? = null,
    var presenterTitle: String? = null,
    var presenterBio: String? = null,
    var presenterEmail: String? = null,
    
    var materialUrl: String? = null,
    var recordingUrl: String? = null,
    var slidesUrl: String? = null,
    var prerequisites: String? = null,
    var targetAudience: String? = null,
    var difficultyLevel: String? = null,
    var tags: List<String>? = null
)