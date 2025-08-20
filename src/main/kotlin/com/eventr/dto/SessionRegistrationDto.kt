package com.eventr.dto

import com.eventr.model.SessionRegistrationStatus
import java.time.LocalDateTime
import java.util.*

data class SessionRegistrationDto(
    var id: UUID? = null,
    var sessionId: UUID? = null,
    var registrationId: UUID? = null,
    var status: SessionRegistrationStatus = SessionRegistrationStatus.REGISTERED,
    var registeredAt: LocalDateTime? = null,
    var checkedInAt: LocalDateTime? = null,
    var cancelledAt: LocalDateTime? = null,
    var waitlistPosition: Int? = null,
    var waitlistRegisteredAt: LocalDateTime? = null,
    var notes: String? = null,
    var rating: Int? = null,
    var feedback: String? = null,
    var attendanceVerified: Boolean = false,
    var verificationMethod: String? = null,
    
    // Session details for easier frontend handling
    var sessionTitle: String? = null,
    var sessionStartTime: LocalDateTime? = null,
    var sessionEndTime: LocalDateTime? = null,
    var sessionLocation: String? = null,
    var sessionRoom: String? = null,
    
    // User details for admin views
    var userName: String? = null,
    var userEmail: String? = null
)

data class SessionRegistrationCreateDto(
    var sessionId: UUID,
    var registrationId: UUID,
    var notes: String? = null
)

data class SessionRegistrationUpdateDto(
    var status: SessionRegistrationStatus? = null,
    var notes: String? = null,
    var rating: Int? = null,
    var feedback: String? = null
)

data class BulkSessionRegistrationDto(
    var sessionIds: List<UUID>,
    var registrationId: UUID,
    var notes: String? = null
)

data class SessionAttendanceDto(
    var sessionRegistrationId: UUID,
    var verificationMethod: String = "MANUAL",
    var notes: String? = null
)