package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.CheckInMethod
import java.util.*

/**
 * Service interface for attendance tracking operations.
 * 
 * Focuses exclusively on attendance data retrieval:
 * - Session attendance tracking
 * - Attendance report generation
 * - Attendance data queries and filtering
 * 
 * Follows Single Responsibility Principle by handling only attendance tracking concerns.
 */
interface AttendanceTrackingService {

    /**
     * Get attendance records for a specific session.
     * 
     * @param sessionId Session ID
     * @return List of check-in records for the session
     */
    fun getSessionAttendance(sessionId: UUID): List<CheckInDto>

    /**
     * Generate comprehensive attendance report for an event.
     * 
     * @param eventId Event ID
     * @return Detailed attendance report with statistics and breakdown
     */
    fun getAttendanceReport(eventId: UUID): AttendanceReportDto

    /**
     * Get attendance records for specific event.
     * 
     * @param eventId Event ID
     * @param sessionId Optional session ID to filter by session
     * @return List of check-in records
     */
    fun getEventAttendance(eventId: UUID, sessionId: UUID? = null): List<CheckInDto>

    /**
     * Get attendance records for specific user across events.
     * 
     * @param userId User ID
     * @param eventIds Optional list of event IDs to filter by
     * @return List of user's check-in records
     */
    fun getUserAttendance(userId: String, eventIds: List<UUID>? = null): List<CheckInDto>

    /**
     * Get attendance summary for event with counts and percentages.
     * 
     * @param eventId Event ID
     * @return Attendance summary with statistics
     */
    fun getAttendanceSummary(eventId: UUID): AttendanceSummaryDto

    /**
     * Get real-time attendance count for ongoing session.
     * 
     * @param sessionId Session ID
     * @return Current attendance count
     */
    fun getLiveAttendanceCount(sessionId: UUID): Int

    /**
     * Export attendance data in specified format.
     * 
     * @param eventId Event ID
     * @param format Export format (CSV, PDF, EXCEL)
     * @param filters Optional filters for data
     * @return Exported data as byte array
     */
    fun exportAttendanceData(eventId: UUID, format: ExportFormat, filters: AttendanceFilters? = null): ByteArray
}

/**
 * Attendance summary with statistics.
 */
data class AttendanceSummaryDto(
    val eventId: UUID,
    val totalRegistrations: Int,
    val totalCheckIns: Int,
    val attendanceRate: Double,
    val sessionBreakdown: Map<UUID, AttendanceSessionSummary>,
    val checkInMethods: Map<CheckInMethod, Int>,
    val timeDistribution: Map<String, Int> // Hour -> Count
)

/**
 * Session-specific attendance summary.
 */
data class AttendanceSessionSummary(
    val sessionId: UUID,
    val sessionName: String,
    val registrations: Int,
    val checkIns: Int,
    val attendanceRate: Double
)

/**
 * Filters for attendance data export.
 */
data class AttendanceFilters(
    val sessionIds: List<UUID>? = null,
    val checkInMethods: List<CheckInMethod>? = null,
    val dateRange: DateRange? = null,
    val includeNoShows: Boolean = true
)

/**
 * Export format enumeration.
 */
enum class ExportFormat {
    CSV, PDF, EXCEL
}

/**
 * Date range filter.
 */
data class DateRange(
    val start: String,  // ISO date string
    val end: String     // ISO date string
)