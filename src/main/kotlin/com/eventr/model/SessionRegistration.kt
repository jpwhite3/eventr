package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class SessionRegistrationStatus {
    REGISTERED, WAITLIST, CANCELLED, ATTENDED, NO_SHOW
}

@Entity
data class SessionRegistration(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    var session: Session? = null,
    
    @ManyToOne
    @JoinColumn(name = "registration_id", nullable = false)
    var registration: Registration? = null,
    
    @Enumerated(EnumType.STRING)
    var status: SessionRegistrationStatus = SessionRegistrationStatus.REGISTERED,
    
    var registeredAt: LocalDateTime = LocalDateTime.now(),
    var checkedInAt: LocalDateTime? = null,
    var cancelledAt: LocalDateTime? = null,
    
    // Waitlist management
    var waitlistPosition: Int? = null,
    var waitlistRegisteredAt: LocalDateTime? = null,
    
    // Session-specific information
    var notes: String? = null,
    var rating: Int? = null, // 1-5 star rating post-session
    var feedback: String? = null,
    
    // Attendance tracking
    var attendanceVerified: Boolean = false,
    var verificationMethod: String? = null, // QR_CODE, MANUAL, RFID, etc.
    
    var updatedAt: LocalDateTime = LocalDateTime.now()
)