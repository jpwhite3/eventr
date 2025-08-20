package com.eventr.dto

import com.eventr.model.CheckInMethod
import com.eventr.model.CheckInType
import java.time.LocalDateTime
import java.util.*

data class CheckInDto(
    var id: UUID? = null,
    var registrationId: UUID? = null,
    var sessionId: UUID? = null,
    var type: CheckInType = CheckInType.EVENT,
    var method: CheckInMethod = CheckInMethod.MANUAL,
    var checkedInAt: LocalDateTime? = null,
    var checkedInBy: String? = null,
    var deviceId: String? = null,
    var deviceName: String? = null,
    var ipAddress: String? = null,
    var userAgent: String? = null,
    var location: String? = null,
    var verificationCode: String? = null,
    var qrCodeUsed: String? = null,
    var isVerified: Boolean = true,
    var notes: String? = null,
    var metadata: String? = null,
    var isSynced: Boolean = true,
    var syncedAt: LocalDateTime? = null,
    
    // Additional info for display
    var userName: String? = null,
    var userEmail: String? = null,
    var eventName: String? = null,
    var sessionTitle: String? = null
)

data class CheckInCreateDto(
    var registrationId: UUID,
    var sessionId: UUID? = null,
    var type: CheckInType = CheckInType.EVENT,
    var method: CheckInMethod = CheckInMethod.MANUAL,
    var checkedInBy: String? = null,
    var deviceId: String? = null,
    var deviceName: String? = null,
    var ipAddress: String? = null,
    var userAgent: String? = null,
    var location: String? = null,
    var verificationCode: String? = null,
    var qrCodeUsed: String? = null,
    var notes: String? = null,
    var metadata: String? = null
)

data class QRCheckInDto(
    var qrCode: String,
    var deviceId: String? = null,
    var deviceName: String? = null,
    var location: String? = null,
    var checkedInBy: String? = null,
    var notes: String? = null
)

data class BulkCheckInDto(
    var registrationIds: List<UUID>,
    var sessionId: UUID? = null,
    var type: CheckInType = CheckInType.EVENT,
    var checkedInBy: String,
    var location: String? = null,
    var notes: String? = null
)

data class CheckInStatsDto(
    var totalRegistrations: Int = 0,
    var totalCheckedIn: Int = 0,
    var eventCheckedIn: Int = 0,
    var sessionCheckedIn: Int = 0,
    var checkInRate: Double = 0.0,
    var recentCheckIns: List<CheckInDto> = emptyList(),
    var checkInsByHour: Map<String, Int> = emptyMap(),
    var checkInsByMethod: Map<CheckInMethod, Int> = emptyMap()
)

data class AttendanceReportDto(
    var eventId: UUID,
    var eventName: String,
    var totalSessions: Int = 0,
    var totalRegistrations: Int = 0,
    var totalAttendees: Int = 0,
    var overallAttendanceRate: Double = 0.0,
    var sessionAttendance: List<SessionAttendanceDto> = emptyList(),
    var attendeeDetails: List<AttendeeAttendanceDto> = emptyList(),
    var generatedAt: LocalDateTime = LocalDateTime.now()
)

data class SessionAttendanceDto(
    var sessionId: UUID,
    var sessionTitle: String,
    var sessionStartTime: LocalDateTime,
    var expectedAttendees: Int = 0,
    var actualAttendees: Int = 0,
    var attendanceRate: Double = 0.0,
    var noShows: Int = 0
)

data class AttendeeAttendanceDto(
    var registrationId: UUID,
    var userName: String,
    var userEmail: String,
    var totalSessions: Int = 0,
    var attendedSessions: Int = 0,
    var attendanceRate: Double = 0.0,
    var missedSessions: List<String> = emptyList()
)

data class QRCodeResponseDto(
    var qrCodeBase64: String,
    var qrCodeUrl: String,
    var expiresAt: LocalDateTime? = null,
    var type: String,
    var identifier: String
)

data class OfflineCheckInDto(
    var id: UUID? = null,
    var registrationId: UUID,
    var sessionId: UUID? = null,
    var type: CheckInType,
    var method: CheckInMethod,
    var checkedInAt: LocalDateTime,
    var checkedInBy: String? = null,
    var deviceId: String? = null,
    var qrCodeUsed: String? = null,
    var notes: String? = null,
    var needsSync: Boolean = true
)