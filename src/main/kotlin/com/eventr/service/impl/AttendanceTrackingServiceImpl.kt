package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.model.CheckInMethod
import com.eventr.model.CheckInType
import com.eventr.repository.*
import com.eventr.service.*
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Implementation of AttendanceTrackingService focused on attendance data operations.
 * 
 * Responsibilities:
 * - Session and event attendance tracking
 * - Attendance report generation
 * - Attendance data queries and statistics
 * - Real-time attendance monitoring
 */
@Service
class AttendanceTrackingServiceImpl(
    private val checkInRepository: CheckInRepository,
    private val registrationRepository: RegistrationRepository,
    private val sessionRepository: SessionRepository,
    private val eventRepository: EventRepository
) : AttendanceTrackingService {

    override fun getSessionAttendance(sessionId: UUID): List<CheckInDto> {
        val checkIns = checkInRepository.findBySessionIdOrderByCheckedInAtDesc(sessionId)
        return checkIns.map { convertToDto(it) }
    }

    override fun getAttendanceReport(eventId: UUID): AttendanceReportDto {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found: $eventId") }
        
        val registrations = registrationRepository.findByEventId(eventId)
        val checkIns = checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        
        val totalRegistrations = registrations.size
        val totalCheckIns = checkIns.size
        val attendanceRate = if (totalRegistrations > 0) {
            (totalCheckIns.toDouble() / totalRegistrations) * 100
        } else 0.0
        
        // Group check-ins by method
        val methodBreakdown = checkIns.groupBy { it.method }
            .mapValues { it.value.size }
        
        // Group check-ins by session
        val sessionBreakdown = checkIns.filter { it.session != null }
            .groupBy { it.session!!.id }
            .mapValues { (sessionId, sessionCheckIns) ->
                val session = sessionCheckIns.first().session!!
                val sessionRegistrations = registrations.filter { 
                    // This would need proper session registration tracking
                    true // Simplified for now
                }.size
                
                AttendanceSessionSummary(
                    sessionId = sessionId!!,
                    sessionName = session.title ?: "Unknown Session",
                    registrations = sessionRegistrations,
                    checkIns = sessionCheckIns.size,
                    attendanceRate = if (sessionRegistrations > 0) {
                        (sessionCheckIns.size.toDouble() / sessionRegistrations) * 100
                    } else 0.0
                )
            }
        
        // Time distribution (simplified - group by hour)
        val timeDistribution = checkIns.groupBy { checkIn ->
            checkIn.checkedInAt?.hour?.toString() ?: "Unknown"
        }.mapValues { it.value.size }
        
        // No-shows calculation
        val checkedInRegistrationIds = checkIns.map { it.registration?.id }.toSet()
        val noShows = registrations.filter { it.id !in checkedInRegistrationIds }
        
        return AttendanceReportDto(
            eventId = eventId,
            eventName = event.name ?: "Unknown Event",
            totalSessions = sessionBreakdown.size,
            totalRegistrations = totalRegistrations,
            totalAttendees = totalCheckIns,
            overallAttendanceRate = attendanceRate,
            sessionAttendance = sessionBreakdown.map { (sessionId, summary) ->
                SessionAttendanceDto(
                    sessionId = sessionId?.toString() ?: "",
                    sessionTitle = summary.sessionName,
                    eventName = event.name ?: "Unknown Event",
                    registrations = summary.registrations,
                    checkedIn = summary.checkIns,
                    attendanceRate = summary.attendanceRate,
                    startTime = null
                )
            },
            generatedAt = LocalDateTime.now()
        )
    }

    override fun getEventAttendance(eventId: UUID, sessionId: UUID?): List<CheckInDto> {
        val checkIns = if (sessionId != null) {
            checkInRepository.findBySessionIdOrderByCheckedInAtDesc(sessionId)
        } else {
            checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        }
        
        return checkIns.map { convertToDto(it) }
    }

    override fun getUserAttendance(userId: String, eventIds: List<UUID>?): List<CheckInDto> {
        // Simplified implementation - would need proper user-based queries
        val checkIns = if (eventIds.isNullOrEmpty()) {
            // Get all check-ins for this user - simplified approach
            emptyList<CheckIn>() // Would need proper implementation
        } else {
            // Get check-ins for specific events - simplified approach
            eventIds.flatMap { eventId ->
                checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
                    .filter { it.registration?.user?.id?.toString() == userId }
            }
        }
        
        return checkIns.map { convertToDto(it) }
    }

    override fun getAttendanceSummary(eventId: UUID): AttendanceSummaryDto {
        val registrations = registrationRepository.findByEventId(eventId)
        val checkIns = checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        
        val totalRegistrations = registrations.size
        val totalCheckIns = checkIns.size
        val attendanceRate = if (totalRegistrations > 0) {
            (totalCheckIns.toDouble() / totalRegistrations) * 100
        } else 0.0
        
        // Session breakdown
        val sessionBreakdown = checkIns.filter { it.session?.id != null }
            .groupBy { it.session!!.id!! }
            .mapValues { (sessionId, sessionCheckIns) ->
                val session = sessionCheckIns.first().session!!
                val sessionRegistrations = totalRegistrations // Simplified
                
                AttendanceSessionSummary(
                    sessionId = sessionId,
                    sessionName = session.title ?: "Unknown Session",
                    registrations = sessionRegistrations,
                    checkIns = sessionCheckIns.size,
                    attendanceRate = if (sessionRegistrations > 0) {
                        (sessionCheckIns.size.toDouble() / sessionRegistrations) * 100
                    } else 0.0
                )
            }
        
        // Method breakdown
        val methodBreakdown = checkIns.groupBy { it.method }
            .mapValues { it.value.size }
        
        // Time distribution (by hour)
        val timeDistribution = checkIns.groupBy { checkIn ->
            checkIn.checkedInAt?.hour?.toString() ?: "Unknown"
        }.mapValues { it.value.size }
        
        return AttendanceSummaryDto(
            eventId = eventId,
            totalRegistrations = totalRegistrations,
            totalCheckIns = totalCheckIns,
            attendanceRate = attendanceRate,
            sessionBreakdown = sessionBreakdown,
            checkInMethods = methodBreakdown,
            timeDistribution = timeDistribution
        )
    }

    override fun getLiveAttendanceCount(sessionId: UUID): Int {
        return checkInRepository.countBySessionId(sessionId).toInt()
    }

    override fun exportAttendanceData(eventId: UUID, format: ExportFormat, filters: AttendanceFilters?): ByteArray {
        // Get attendance data based on filters
        val checkIns = getEventAttendance(eventId, filters?.sessionIds?.firstOrNull())
        
        // Filter by method if specified
        val filteredCheckIns = filters?.checkInMethods?.let { methods ->
            checkIns.filter { it.method in methods }
        } ?: checkIns
        
        return when (format) {
            ExportFormat.CSV -> exportToCSV(filteredCheckIns)
            ExportFormat.PDF -> exportToPDF(filteredCheckIns)
            ExportFormat.EXCEL -> exportToExcel(filteredCheckIns)
        }
    }

    private fun exportToCSV(checkIns: List<CheckInDto>): ByteArray {
        val csv = StringBuilder()
        csv.append("Event,Session,User,Registration,Check-in Time,Method,Location,Notes\n")
        
        checkIns.forEach { checkIn ->
            csv.append("${checkIn.eventName ?: ""},")
            csv.append("${checkIn.sessionTitle ?: ""},")
            csv.append("${checkIn.userEmail ?: ""},")
            csv.append("${checkIn.registrationId ?: ""},")
            csv.append("${checkIn.checkedInAt ?: ""},")
            csv.append("${checkIn.method ?: ""},")
            csv.append("${checkIn.location ?: ""},")
            csv.append("${checkIn.notes ?: ""}\n")
        }
        
        return csv.toString().toByteArray()
    }

    private fun exportToPDF(checkIns: List<CheckInDto>): ByteArray {
        // Simplified PDF generation - in production would use proper PDF library
        val content = "Attendance Report\n\n" +
                checkIns.joinToString("\n") { checkIn ->
                    "${checkIn.userEmail} - ${checkIn.checkedInAt} - ${checkIn.method}"
                }
        return content.toByteArray()
    }

    private fun exportToExcel(checkIns: List<CheckInDto>): ByteArray {
        // Simplified Excel generation - in production would use Apache POI
        return exportToCSV(checkIns) // Return CSV for now
    }

    private fun convertToDto(checkIn: CheckIn): CheckInDto {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        return CheckInDto().apply {
            BeanUtils.copyProperties(checkIn, this)
            
            // Convert registration
            checkIn.registration?.let { reg ->
                this.registrationId = reg.id
                reg.eventInstance?.event?.let { event ->
                    this.eventName = event.name
                }
            }
            
            // Convert session
            checkIn.session?.let { sess ->
                this.sessionId = sess.id
                this.sessionTitle = sess.title
            }
            
            // Format timestamp
            this.checkedInAt = checkIn.checkedInAt
        }
    }

    private fun convertRegistrationToDto(registration: Registration): RegistrationDto {
        return RegistrationDto().apply {
            BeanUtils.copyProperties(registration, this)
            
            // Add event instance information
            registration.eventInstance?.let { eventInstance ->
                this.eventInstanceId = eventInstance.id
                eventInstance.event?.let { event ->
                    // RegistrationDto doesn't have eventId/eventName properties
                    // These would be accessed through the eventInstance relationship
                }
            }
        }
    }
}