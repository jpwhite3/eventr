package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
@Transactional
class CheckInService(
    private val checkInRepository: CheckInRepository,
    private val registrationRepository: RegistrationRepository,
    private val sessionRepository: SessionRepository,
    private val eventRepository: EventRepository,
    private val qrCodeService: QRCodeService
) {

    fun checkInWithQR(qrCheckInDto: QRCheckInDto): CheckInDto {
        val qrContent = URLDecoder.decode(qrCheckInDto.qrCode, "UTF-8")
        
        // Parse QR code content
        val qrData = parseQRCode(qrContent)
        
        // Validate signature
        if (!qrCodeService.validateQRSignature(
                qrData.type, 
                qrData.identifier, 
                qrData.userId, 
                qrData.timestamp, 
                qrData.signature
            )) {
            throw IllegalArgumentException("Invalid or expired QR code")
        }
        
        return when (qrData.type) {
            "event" -> checkInToEvent(qrData, qrCheckInDto)
            "session" -> checkInToSession(qrData, qrCheckInDto)
            else -> throw IllegalArgumentException("Invalid QR code type")
        }
    }

    fun manualCheckIn(createDto: CheckInCreateDto): CheckInDto {
        val registration = registrationRepository.findById(createDto.registrationId)
            .orElseThrow { IllegalArgumentException("Registration not found") }
        
        // Check for duplicates
        val existingCheckIn = if (createDto.sessionId != null) {
            checkInRepository.findByRegistrationIdAndSessionId(createDto.registrationId, createDto.sessionId)
        } else {
            checkInRepository.findByRegistrationIdAndType(createDto.registrationId, createDto.type)
        }
        
        if (existingCheckIn != null) {
            throw IllegalArgumentException("Already checked in")
        }
        
        val checkIn = CheckIn().apply {
            this.registration = registration
            this.session = createDto.sessionId?.let { sessionRepository.findById(it).orElse(null) }
            this.type = createDto.type
            this.method = createDto.method
            this.checkedInBy = createDto.checkedInBy
            this.deviceId = createDto.deviceId
            this.deviceName = createDto.deviceName
            this.ipAddress = createDto.ipAddress
            this.userAgent = createDto.userAgent
            this.location = createDto.location
            this.verificationCode = createDto.verificationCode
            this.notes = createDto.notes
            this.metadata = createDto.metadata
        }
        
        val saved = checkInRepository.save(checkIn)
        
        // Update session registration status if applicable
        updateSessionRegistrationStatus(saved)
        
        return convertToDto(saved)
    }

    fun bulkCheckIn(bulkDto: BulkCheckInDto): List<CheckInDto> {
        val results = mutableListOf<CheckInDto>()
        
        for (registrationId in bulkDto.registrationIds) {
            try {
                val createDto = CheckInCreateDto(
                    registrationId = registrationId,
                    sessionId = bulkDto.sessionId,
                    type = bulkDto.type,
                    method = CheckInMethod.BULK,
                    checkedInBy = bulkDto.checkedInBy,
                    location = bulkDto.location,
                    notes = bulkDto.notes
                )
                results.add(manualCheckIn(createDto))
            } catch (e: Exception) {
                // Log error but continue with other check-ins
                println("Failed to check in registration $registrationId: ${e.message}")
            }
        }
        
        return results
    }

    fun getEventCheckInStats(eventId: UUID): CheckInStatsDto {
        val totalRegistrations = registrationRepository.countByEventId(eventId)
        val totalCheckedIn = checkInRepository.countUniqueAttendeesByEventId(eventId)
        val eventCheckedIn = checkInRepository.countByEventId(eventId)
        
        val recentCheckIns = checkInRepository.findRecentEventCheckIns(
            eventId, LocalDateTime.now().minusHours(24)
        ).take(10).map { convertToDto(it) }
        
        val methodStats = checkInRepository.getCheckInMethodStats(eventId)
            .associate { CheckInMethod.valueOf(it[0].toString()) to (it[1] as Long).toInt() }
        
        val hourlyStats = checkInRepository.getTodayHourlyCheckInStats(eventId)
            .associate { "${it[0]}:00" to (it[1] as Long).toInt() }
        
        return CheckInStatsDto(
            totalRegistrations = totalRegistrations.toInt(),
            totalCheckedIn = totalCheckedIn.toInt(),
            eventCheckedIn = eventCheckedIn.toInt(),
            checkInRate = if (totalRegistrations > 0) (totalCheckedIn.toDouble() / totalRegistrations.toDouble() * 100) else 0.0,
            recentCheckIns = recentCheckIns,
            checkInsByHour = hourlyStats,
            checkInsByMethod = methodStats
        )
    }

    fun getSessionAttendance(sessionId: UUID): List<CheckInDto> {
        return checkInRepository.findBySessionIdOrderByCheckedInAtDesc(sessionId)
            .map { convertToDto(it) }
    }

    fun generateEventQRCode(eventId: UUID, userId: String): QRCodeResponseDto {
        val qrData = qrCodeService.generateEventCheckInQR(eventId.toString(), userId)
        return QRCodeResponseDto(
            qrCodeBase64 = Base64.getEncoder().encodeToString(qrData.imageBytes),
            qrCodeUrl = qrData.content,
            expiresAt = qrData.expiresAt,
            type = "event",
            identifier = eventId.toString()
        )
    }

    fun generateSessionQRCode(sessionId: UUID, userId: String): QRCodeResponseDto {
        val qrData = qrCodeService.generateSessionCheckInQR(sessionId.toString(), userId)
        return QRCodeResponseDto(
            qrCodeBase64 = Base64.getEncoder().encodeToString(qrData.imageBytes),
            qrCodeUrl = qrData.content,
            expiresAt = qrData.expiresAt,
            type = "session",
            identifier = sessionId.toString()
        )
    }

    fun generateStaffQRCode(eventId: UUID, sessionId: UUID? = null): QRCodeResponseDto {
        val qrData = qrCodeService.generateStaffCheckInQR(eventId.toString(), sessionId?.toString())
        return QRCodeResponseDto(
            qrCodeBase64 = Base64.getEncoder().encodeToString(qrData.imageBytes),
            qrCodeUrl = qrData.content,
            type = if (sessionId != null) "staff-session" else "staff-event",
            identifier = sessionId?.toString() ?: eventId.toString()
        )
    }

    fun generateAttendeeBadge(eventId: UUID, userId: String, userName: String): QRCodeResponseDto {
        val qrData = qrCodeService.generateAttendeeBadge(eventId.toString(), userId, userName)
        return QRCodeResponseDto(
            qrCodeBase64 = Base64.getEncoder().encodeToString(qrData.imageBytes),
            qrCodeUrl = qrData.content,
            type = "badge",
            identifier = eventId.toString()
        )
    }

    fun syncOfflineCheckIns(offlineCheckIns: List<OfflineCheckInDto>): List<CheckInDto> {
        val results = mutableListOf<CheckInDto>()
        
        for (offlineCheckIn in offlineCheckIns) {
            try {
                val createDto = CheckInCreateDto(
                    registrationId = offlineCheckIn.registrationId,
                    sessionId = offlineCheckIn.sessionId,
                    type = offlineCheckIn.type,
                    method = offlineCheckIn.method,
                    checkedInBy = offlineCheckIn.checkedInBy,
                    deviceId = offlineCheckIn.deviceId,
                    qrCodeUsed = offlineCheckIn.qrCodeUsed,
                    notes = offlineCheckIn.notes
                )
                
                val result = manualCheckIn(createDto)
                
                // Mark as synced
                checkInRepository.findById(result.id!!).ifPresent { checkIn ->
                    checkIn.isSynced = true
                    checkIn.syncedAt = LocalDateTime.now()
                    checkInRepository.save(checkIn)
                }
                
                results.add(result)
            } catch (e: Exception) {
                println("Failed to sync offline check-in: ${e.message}")
            }
        }
        
        return results
    }

    fun getAttendanceReport(eventId: UUID): AttendanceReportDto {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found") }
        
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId)
        val totalRegistrations = registrationRepository.countByEventId(eventId)
        val totalAttendees = checkInRepository.countUniqueAttendeesByEventId(eventId)
        
        val sessionAttendance = sessions.map { session ->
            val expectedAttendees = session.sessionRegistrations?.count { it.status == SessionRegistrationStatus.REGISTERED } ?: 0
            val actualAttendees = checkInRepository.countUniqueAttendeesBySessionId(session.id!!).toInt()
            
            SessionAttendanceDto(
                sessionId = session.id.toString(),
                sessionTitle = session.title,
                eventName = event.name ?: "Unknown Event",
                registrations = expectedAttendees,
                checkedIn = actualAttendees,
                attendanceRate = if (expectedAttendees > 0) (actualAttendees.toDouble() / expectedAttendees.toDouble() * 100) else 0.0,
                startTime = session.startTime?.toString()
            )
        }
        
        return AttendanceReportDto(
            eventId = eventId,
            eventName = event.name ?: "Unknown Event",
            totalSessions = sessions.size,
            totalRegistrations = totalRegistrations.toInt(),
            totalAttendees = totalAttendees.toInt(),
            overallAttendanceRate = if (totalRegistrations > 0) (totalAttendees.toDouble() / totalRegistrations.toDouble() * 100) else 0.0,
            sessionAttendance = sessionAttendance
        )
    }

    private fun checkInToEvent(qrData: QRData, qrCheckInDto: QRCheckInDto): CheckInDto {
        val registration = findRegistrationByUserIdAndEventId(qrData.userId!!, UUID.fromString(qrData.identifier))
            ?: throw IllegalArgumentException("Registration not found")
        
        val createDto = CheckInCreateDto(
            registrationId = registration.id!!,
            type = CheckInType.EVENT,
            method = CheckInMethod.QR_CODE,
            checkedInBy = qrCheckInDto.checkedInBy,
            deviceId = qrCheckInDto.deviceId,
            deviceName = qrCheckInDto.deviceName,
            location = qrCheckInDto.location,
            qrCodeUsed = qrCheckInDto.qrCode,
            notes = qrCheckInDto.notes
        )
        
        return manualCheckIn(createDto)
    }

    private fun checkInToSession(qrData: QRData, qrCheckInDto: QRCheckInDto): CheckInDto {
        val registration = findRegistrationByUserIdAndSessionId(qrData.userId!!, UUID.fromString(qrData.identifier))
            ?: throw IllegalArgumentException("Registration not found")
        
        val createDto = CheckInCreateDto(
            registrationId = registration.id!!,
            sessionId = UUID.fromString(qrData.identifier),
            type = CheckInType.SESSION,
            method = CheckInMethod.QR_CODE,
            checkedInBy = qrCheckInDto.checkedInBy,
            deviceId = qrCheckInDto.deviceId,
            deviceName = qrCheckInDto.deviceName,
            location = qrCheckInDto.location,
            qrCodeUsed = qrCheckInDto.qrCode,
            notes = qrCheckInDto.notes
        )
        
        return manualCheckIn(createDto)
    }

    private fun updateSessionRegistrationStatus(checkIn: CheckIn) {
        if (checkIn.session != null && checkIn.registration != null) {
            // Find the session registration and mark as attended
            checkIn.registration!!.sessionRegistrations?.find { 
                it.session?.id == checkIn.session!!.id 
            }?.let { sessionReg ->
                sessionReg.status = SessionRegistrationStatus.ATTENDED
                sessionReg.checkedInAt = checkIn.checkedInAt
                sessionReg.attendanceVerified = true
                sessionReg.verificationMethod = checkIn.method.name
            }
        }
    }

    private fun findRegistrationByUserIdAndEventId(userId: String, eventId: UUID): Registration? {
        // This would typically involve a user service or different logic
        // For now, we'll assume we can find by email or user identifier
        return registrationRepository.findByUserEmailAndEventId(userId, eventId)
    }

    private fun findRegistrationByUserIdAndSessionId(userId: String, sessionId: UUID): Registration? {
        // Similar to above, would need proper user lookup
        val session = sessionRepository.findById(sessionId).orElse(null)
        return session?.let { 
            registrationRepository.findByUserEmailAndEventId(userId, it.event!!.id!!)
        }
    }

    private fun convertToDto(checkIn: CheckIn): CheckInDto {
        val dto = CheckInDto()
        BeanUtils.copyProperties(checkIn, dto)
        dto.registrationId = checkIn.registration?.id
        dto.sessionId = checkIn.session?.id
        dto.userName = checkIn.registration?.userName
        dto.userEmail = checkIn.registration?.userEmail
        dto.eventName = checkIn.registration?.eventInstance?.event?.name
        dto.sessionTitle = checkIn.session?.title
        return dto
    }

    private fun parseQRCode(qrContent: String): QRData {
        // Parse URL and extract parameters
        val url = java.net.URI.create(qrContent).toURL()
        val params = url.query.split("&").associate {
            val (key, value) = it.split("=")
            key to URLDecoder.decode(value, "UTF-8")
        }
        
        val pathParts = url.path.split("/")
        val type = when {
            pathParts.contains("event") -> "event"
            pathParts.contains("session") -> "session"
            else -> "unknown"
        }
        
        val identifier = pathParts.lastOrNull { it.matches(Regex("[0-9a-f-]{36}")) } 
            ?: throw IllegalArgumentException("Invalid QR code format")
        
        return QRData(
            type = type,
            identifier = identifier,
            userId = params["user"],
            timestamp = params["t"] ?: "",
            signature = params["sig"] ?: ""
        )
    }

    private data class QRData(
        val type: String,
        val identifier: String,
        val userId: String?,
        val timestamp: String,
        val signature: String
    )
}