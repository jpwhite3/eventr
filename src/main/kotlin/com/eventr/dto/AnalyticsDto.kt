package com.eventr.dto

data class ExecutiveMetricsDto(
    val totalEvents: Int,
    val totalRegistrations: Int,
    val totalRevenue: Double,
    val attendanceRate: Double,
    val activeEvents: Int,
    val upcomingEvents: Int,
    val completedEvents: Int,
    val avgEventCapacity: Double,
    val registrationTrend: Double,
    val revenueTrend: Double
)

data class EventSummaryDto(
    val id: String,
    val title: String,
    val registrations: Int,
    val capacity: Int,
    val attendanceRate: Double,
    val revenue: Double,
    val status: String,
    val startDate: String
)

data class ChartDataDto(
    val labels: List<String>,
    val registrationData: List<Int>,
    val revenueData: List<Double>,
    val attendanceData: List<Double>
)

data class RegistrationTrendsDto(
    val totalRegistrations: Int,
    val dailyRegistrations: Int,
    val weeklyGrowth: Double,
    val monthlyGrowth: Double,
    val conversionRate: Double,
    val averageTimeToRegister: Double,
    val peakRegistrationHour: String,
    val topReferralSources: List<ReferralSourceDto>,
    val registrationsByEventType: List<EventTypeRegistrationDto>,
    val registrationTrends: List<RegistrationTrendDto>,
    val demographicBreakdown: DemographicBreakdownDto
)

data class ReferralSourceDto(
    val source: String,
    val registrations: Int,
    val percentage: Double
)

data class EventTypeRegistrationDto(
    val type: String,
    val count: Int,
    val percentage: Double
)

data class RegistrationTrendDto(
    val date: String,
    val registrations: Int,
    val cumulativeRegistrations: Int
)

data class DemographicBreakdownDto(
    val ageGroups: List<AgeGroupDto>,
    val genderDistribution: List<GenderDto>,
    val locations: List<LocationDto>
)

data class AgeGroupDto(
    val group: String,
    val count: Int,
    val percentage: Double
)

data class GenderDto(
    val gender: String,
    val count: Int,
    val percentage: Double
)

data class LocationDto(
    val location: String,
    val count: Int,
    val percentage: Double
)

data class EventAnalyticsDto(
    val eventId: String,
    val eventName: String,
    val totalRegistrations: Int,
    val totalCheckIns: Int,
    val attendanceRate: Double,
    val sessionCount: Int,
    val avgSessionAttendance: Double,
    val revenue: Double,
    val registrationsByDay: List<DayRegistrationDto>,
    val checkInMethods: List<CheckInMethodDto>,
    val sessionAnalytics: List<SessionAnalyticsDto>
)

data class DayRegistrationDto(
    val date: String,
    val registrations: Int
)

data class CheckInMethodDto(
    val method: String,
    val count: Int,
    val percentage: Double
)

data class SessionAnalyticsDto(
    val sessionId: String,
    val sessionTitle: String,
    val registrations: Int,
    val checkedIn: Int,
    val attendanceRate: Double,
    val capacity: Int
)

data class AttendanceAnalyticsDto(
    val totalRegistrations: Int,
    val totalCheckIns: Int,
    val overallAttendanceRate: Double,
    val checkInsByMethod: List<CheckInMethodDto>,
    val checkInsByHour: List<HourlyCheckInDto>,
    val attendanceByEventType: List<EventTypeAttendanceDto>,
    val recentCheckIns: List<RecentCheckInDto>,
    val sessionsAttendance: List<SessionAttendanceDto>
)

data class HourlyCheckInDto(
    val hour: String,
    val checkIns: Int
)

data class EventTypeAttendanceDto(
    val eventType: String,
    val totalRegistrations: Int,
    val totalCheckIns: Int,
    val attendanceRate: Double
)

data class RecentCheckInDto(
    val id: String,
    val userName: String,
    val eventName: String,
    val sessionName: String?,
    val checkedInAt: String,
    val method: String
)

data class SessionAttendanceDto(
    val sessionId: String,
    val sessionTitle: String,
    val eventName: String,
    val registrations: Int,
    val checkedIn: Int,
    val attendanceRate: Double,
    val startTime: String?
)

data class AttendanceReportDto(
    val eventId: java.util.UUID,
    val eventName: String,
    val totalSessions: Int = 0,
    val totalRegistrations: Int = 0,
    val totalAttendees: Int = 0,
    val overallAttendanceRate: Double = 0.0,
    val sessionAttendance: List<SessionAttendanceDto> = emptyList(),
    val generatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)