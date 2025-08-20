package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class CheckInType {
    EVENT, SESSION
}

enum class CheckInMethod {
    QR_CODE, MANUAL, BULK, SELF_SERVICE, STAFF_SCAN, RFID, NFC
}

@Entity
data class CheckIn(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne
    @JoinColumn(name = "registration_id", nullable = false)
    var registration: Registration? = null,
    
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = true)
    var session: Session? = null,
    
    @Enumerated(EnumType.STRING)
    var type: CheckInType = CheckInType.EVENT,
    
    @Enumerated(EnumType.STRING)
    var method: CheckInMethod = CheckInMethod.MANUAL,
    
    var checkedInAt: LocalDateTime = LocalDateTime.now(),
    var checkedInBy: String? = null, // Staff member or system user
    
    // Location and device info
    var deviceId: String? = null,
    var deviceName: String? = null,
    var ipAddress: String? = null,
    var userAgent: String? = null,
    var location: String? = null,
    
    // Verification details
    var verificationCode: String? = null,
    var qrCodeUsed: String? = null,
    var isVerified: Boolean = true,
    
    // Additional metadata
    var notes: String? = null,
    var metadata: String? = null, // JSON for additional data
    
    // Sync status for offline check-ins
    var isSynced: Boolean = true,
    var syncedAt: LocalDateTime? = null,
    
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)