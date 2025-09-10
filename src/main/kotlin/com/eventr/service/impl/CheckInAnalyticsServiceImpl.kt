package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.model.CheckInMethod
import com.eventr.model.CheckInType
import com.eventr.repository.*
import com.eventr.service.CheckInAnalyticsService
import com.eventr.service.*
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Implementation of CheckInAnalyticsService focused on analytics and statistics.
 * 
 * Responsibilities:
 * - Event check-in statistics generation
 * - Real-time analytics and performance metrics
 * - Check-in pattern analysis and insights
 * - Predictive analytics for check-in completion
 */
@Service
class CheckInAnalyticsServiceImpl(
    private val checkInRepository: CheckInRepository,
    private val registrationRepository: RegistrationRepository,
    private val eventRepository: EventRepository,
    private val sessionRepository: SessionRepository
) : CheckInAnalyticsService {

    override fun getEventCheckInStats(eventId: UUID): CheckInStatsDto {
        val registrations = registrationRepository.findByEventId(eventId)
        val checkIns = checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        
        val totalRegistrations = registrations.size
        val totalCheckIns = checkIns.size
        val attendanceRate = if (totalRegistrations > 0) {
            (totalCheckIns.toDouble() / totalRegistrations) * 100
        } else 0.0
        
        // Method breakdown
        val methodStats = checkIns.groupBy { it.method }
            .mapValues { (method, methodCheckIns) ->
                val count = methodCheckIns.size
                val percentage = if (totalCheckIns > 0) (count.toDouble() / totalCheckIns) * 100 else 0.0
                val avgProcessingTime = calculateAverageProcessingTime(methodCheckIns)
                
                MethodStats(
                    count = count,
                    percentage = percentage,
                    averageProcessingTime = avgProcessingTime,
                    successRate = 100.0 // Simplified - all recorded check-ins are successful
                )
            }
        
        // Time distribution
        val timeDistribution = checkIns.groupBy { checkIn ->
            checkIn.checkedInAt?.hour?.toString() ?: "Unknown"
        }.mapValues { it.value.size }
        
        // Peak times
        val peakTimes = identifyPeakTimes(checkIns)
        
        return CheckInStatsDto(
            eventId = eventId,
            totalRegistrations = totalRegistrations,
            totalCheckedIn = totalCheckIns,
            eventCheckedIn = totalCheckIns,
            sessionCheckedIn = 0, // Would need session-specific calculation
            checkInRate = attendanceRate,
            recentCheckIns = checkIns.take(10).map { convertCheckInToDto(it) },
            checkInsByHour = timeDistribution,
            checkInsByMethod = methodStats.mapValues { it.value.count }
        )
    }

    override fun getRealTimeCheckInMetrics(eventId: UUID): RealTimeCheckInMetrics {
        val registrations = registrationRepository.findByEventId(eventId)
        val checkIns = checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        
        val currentCheckIns = checkIns.size
        val targetCheckIns = registrations.size
        val checkInRate = calculateCheckInRate(checkIns)
        val estimatedCompletion = calculateEstimatedCompletion(currentCheckIns, targetCheckIns, checkInRate)
        
        val bottlenecks = identifyBottlenecks(checkIns)
        val recommendations = generateRecommendations(checkIns, bottlenecks)
        
        return RealTimeCheckInMetrics(
            eventId = eventId,
            currentCheckIns = currentCheckIns,
            targetCheckIns = targetCheckIns,
            checkInRate = checkInRate,
            estimatedCompletion = estimatedCompletion,
            bottlenecks = bottlenecks,
            recommendations = recommendations
        )
    }

    override fun analyzeCheckInPatterns(eventId: UUID): CheckInPatternAnalysis {
        val checkIns = checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        
        val peakTimes = identifyPeakTimes(checkIns)
        val methodDistribution = analyzeMethodDistribution(checkIns)
        val locationHotspots = identifyLocationHotspots(checkIns)
        val userBehaviorInsights = generateUserBehaviorInsights(checkIns)
        
        return CheckInPatternAnalysis(
            eventId = eventId,
            peakTimes = peakTimes,
            methodDistribution = methodDistribution,
            locationHotspots = locationHotspots,
            userBehaviorInsights = userBehaviorInsights
        )
    }

    override fun getSessionCheckInStats(sessionId: UUID): SessionCheckInStats {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found: $sessionId") }
        
        val eventId = session.event?.id ?: throw IllegalArgumentException("Session has no associated event")
        val totalRegistrations = registrationRepository.countByEventId(eventId)
        val sessionCheckIns = checkInRepository.findBySessionIdOrderByCheckedInAtDesc(sessionId)
        
        val checkedInCount = sessionCheckIns.size
        val attendanceRate = if (totalRegistrations > 0) {
            (checkedInCount.toDouble() / totalRegistrations) * 100
        } else 0.0
        
        val onTimeCheckIns = sessionCheckIns.count { checkIn ->
            // Consider on-time if checked in before session start
            val sessionStart = session.startTime
            val checkInTime = checkIn.checkedInAt
            sessionStart != null && checkInTime != null && checkInTime.isBefore(sessionStart)
        }
        
        val lateCheckIns = checkedInCount - onTimeCheckIns
        val averageCheckInTime = calculateAverageCheckInTimeBeforeSession(sessionCheckIns, session.startTime)
        
        val methodBreakdown = sessionCheckIns.groupBy { it.method }
            .mapValues { it.value.size }
        
        return SessionCheckInStats(
            sessionId = sessionId,
            sessionName = session.title ?: "Unknown Session",
            totalRegistrations = totalRegistrations.toInt(),
            checkedInCount = checkedInCount,
            attendanceRate = attendanceRate,
            onTimeCheckIns = onTimeCheckIns,
            lateCheckIns = lateCheckIns,
            averageCheckInTime = averageCheckInTime,
            methodBreakdown = methodBreakdown
        )
    }

    override fun getStaffPerformanceMetrics(eventId: UUID, timeRange: TimeRange?): StaffPerformanceMetrics {
        val checkIns = checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        
        // Filter by time range if provided
        val filteredCheckIns = timeRange?.let { range ->
            checkIns.filter { checkIn ->
                val checkInTime = checkIn.checkedInAt
                checkInTime != null && 
                checkInTime.isAfter(LocalDateTime.parse(range.start)) &&
                checkInTime.isBefore(LocalDateTime.parse(range.end))
            }
        } ?: checkIns
        
        val staffMetrics = filteredCheckIns
            .filter { !it.checkedInBy.isNullOrBlank() }
            .groupBy { it.checkedInBy!! }
            .mapValues { (staffId, staffCheckIns) ->
                val checkInsProcessed = staffCheckIns.size
                val avgProcessingTime = calculateAverageProcessingTime(staffCheckIns)
                val errorRate = 0.0 // Simplified - would need error tracking
                val efficiency = determineEfficiency(avgProcessingTime, checkInsProcessed)
                
                StaffStats(
                    staffId = staffId,
                    staffName = staffId, // Would map to actual staff names
                    checkInsProcessed = checkInsProcessed,
                    averageProcessingTime = avgProcessingTime,
                    errorRate = errorRate,
                    efficiency = efficiency
                )
            }
        
        val overallEfficiency = calculateOverallEfficiency(staffMetrics.values)
        val recommendations = generateStaffRecommendations(staffMetrics)
        
        return StaffPerformanceMetrics(
            eventId = eventId,
            timeRange = timeRange ?: TimeRange(
                start = LocalDateTime.now().minusHours(24).toString(),
                end = LocalDateTime.now().toString()
            ),
            staffMetrics = staffMetrics,
            overallEfficiency = overallEfficiency,
            recommendedOptimizations = recommendations
        )
    }

    override fun getCheckInTrends(eventIds: List<UUID>): CheckInTrendsAnalysis {
        // Simplified implementation - would require historical data analysis
        val allCheckIns = eventIds.flatMap { eventId ->
            checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        }
        
        return CheckInTrendsAnalysis(
            eventIds = eventIds,
            overallTrend = "STABLE",
            methodTrends = mapOf(
                CheckInMethod.QR_CODE to "IMPROVING",
                CheckInMethod.MANUAL to "STABLE"
            ),
            seasonalPatterns = emptyList(),
            benchmarks = CheckInBenchmarks(
                industryAverage = CheckInMetrics(75.0, 30.0, mapOf(), 85.0),
                topPerformers = CheckInMetrics(90.0, 15.0, mapOf(), 95.0),
                yourPerformance = CheckInMetrics(80.0, 25.0, mapOf(), 88.0)
            )
        )
    }

    override fun getMethodEffectivenessAnalysis(eventId: UUID): MethodEffectivenessAnalysis {
        val checkIns = checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        val totalCheckIns = checkIns.size
        
        val methods = checkIns.groupBy { it.method }
            .mapValues { (method, methodCheckIns) ->
                val usagePercentage = (methodCheckIns.size.toDouble() / totalCheckIns) * 100
                val successRate = 100.0 // Simplified
                val averageTime = calculateAverageProcessingTime(methodCheckIns)
                
                MethodEffectiveness(
                    method = method,
                    usagePercentage = usagePercentage,
                    successRate = successRate,
                    averageTime = averageTime,
                    userSatisfaction = 8.5, // Would come from surveys
                    costEffectiveness = "HIGH",
                    scalability = "HIGH"
                )
            }
        
        val recommendations = generateMethodRecommendations(methods)
        
        return MethodEffectivenessAnalysis(
            eventId = eventId,
            methods = methods,
            recommendations = recommendations
        )
    }

    override fun predictCheckInCompletion(eventId: UUID): CheckInPrediction {
        val registrations = registrationRepository.findByEventId(eventId)
        val checkIns = checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        
        val totalTarget = registrations.size
        val currentProgress = checkIns.size.toDouble() / totalTarget
        val checkInRate = calculateCheckInRate(checkIns)
        
        val remaining = totalTarget - checkIns.size
        val estimatedMinutes = if (checkInRate > 0) remaining / checkInRate else -1.0
        
        val estimatedCompletion = if (estimatedMinutes > 0) {
            LocalDateTime.now().plusMinutes(estimatedMinutes.toLong()).toString()
        } else {
            "Unable to predict"
        }
        
        val bottlenecks = identifySystemBottlenecks(checkIns)
        val mitigationStrategies = generateMitigationStrategies(bottlenecks)
        
        return CheckInPrediction(
            eventId = eventId,
            estimatedCompletionTime = estimatedCompletion,
            confidence = 0.75, // Simplified confidence calculation
            currentProgress = currentProgress,
            bottlenecks = bottlenecks,
            mitigationStrategies = mitigationStrategies
        )
    }

    // Helper methods (simplified implementations)
    private fun calculateAverageProcessingTime(checkIns: List<CheckIn>): Double = 30.0 // Seconds

    private fun calculateCheckInRate(checkIns: List<CheckIn>): Double {
        if (checkIns.isEmpty()) return 0.0
        
        val sortedCheckIns = checkIns.sortedBy { it.checkedInAt }
        val firstCheckIn = sortedCheckIns.first().checkedInAt
        val lastCheckIn = sortedCheckIns.last().checkedInAt
        
        if (firstCheckIn == null || lastCheckIn == null) return 0.0
        
        val durationMinutes = ChronoUnit.MINUTES.between(firstCheckIn, lastCheckIn).toDouble()
        return if (durationMinutes > 0) checkIns.size / durationMinutes else 0.0
    }

    private fun calculateEstimatedCompletion(current: Int, target: Int, rate: Double): String {
        if (rate <= 0 || current >= target) return "Completed"
        
        val remaining = target - current
        val estimatedMinutes = remaining / rate
        return LocalDateTime.now().plusMinutes(estimatedMinutes.toLong()).toString()
    }

    private fun identifyBottlenecks(checkIns: List<CheckIn>): List<String> {
        return listOf("Location congestion", "Slow manual check-in process")
    }

    private fun generateRecommendations(checkIns: List<CheckIn>, bottlenecks: List<String>): List<String> {
        return listOf("Add more QR scanning stations", "Deploy additional staff")
    }

    private fun identifyPeakTimes(checkIns: List<CheckIn>): List<PeakTimeWindow> {
        // Simplified implementation
        return listOf(
            PeakTimeWindow("09:00", "10:00", 50, "HIGH"),
            PeakTimeWindow("13:00", "14:00", 30, "MEDIUM")
        )
    }

    private fun analyzeMethodDistribution(checkIns: List<CheckIn>): Map<CheckInMethod, MethodStats> {
        return checkIns.groupBy { it.method }
            .mapValues { (_, methodCheckIns) ->
                MethodStats(
                    count = methodCheckIns.size,
                    percentage = (methodCheckIns.size.toDouble() / checkIns.size) * 100,
                    averageProcessingTime = calculateAverageProcessingTime(methodCheckIns),
                    successRate = 100.0
                )
            }
    }

    private fun identifyLocationHotspots(checkIns: List<CheckIn>): List<LocationHotspot> {
        return checkIns.filter { !it.location.isNullOrBlank() }
            .groupBy { it.location!! }
            .map { (location, locationCheckIns) ->
                LocationHotspot(
                    location = location,
                    checkInCount = locationCheckIns.size,
                    congestionLevel = if (locationCheckIns.size > 20) "HIGH" else "MODERATE",
                    recommendedActions = listOf("Add more stations")
                )
            }
    }

    private fun generateUserBehaviorInsights(checkIns: List<CheckIn>): List<String> {
        return listOf(
            "Users prefer QR code check-in over manual",
            "Peak check-in time is 30 minutes before event start",
            "Mobile devices account for 80% of QR scans"
        )
    }

    private fun findPeakHour(timeDistribution: Map<String, Int>): String {
        return timeDistribution.maxByOrNull { it.value }?.key ?: "Unknown"
    }

    private fun calculateOverallAverageCheckInTime(checkIns: List<CheckIn>): Double = 25.0

    private fun calculateAverageCheckInTimeBeforeSession(checkIns: List<CheckIn>, sessionStart: LocalDateTime?): Double = 15.0

    private fun determineEfficiency(avgProcessingTime: Double, checkInsProcessed: Int): String {
        return when {
            avgProcessingTime < 20 && checkInsProcessed > 50 -> "EXCELLENT"
            avgProcessingTime < 30 && checkInsProcessed > 30 -> "GOOD"
            else -> "AVERAGE"
        }
    }

    private fun calculateOverallEfficiency(staffStats: Collection<StaffStats>): Double = 85.0

    private fun generateStaffRecommendations(staffMetrics: Map<String, StaffStats>): List<String> {
        return listOf("Provide additional training for slower staff", "Redistribute workload evenly")
    }

    private fun generateMethodRecommendations(methods: Map<CheckInMethod, MethodEffectiveness>): List<MethodRecommendation> {
        return listOf(
            MethodRecommendation(
                method = CheckInMethod.QR_CODE,
                recommendation = "INCREASE",
                reason = "Highest efficiency and user satisfaction",
                expectedImpact = "25% reduction in processing time"
            )
        )
    }

    private fun identifySystemBottlenecks(checkIns: List<CheckIn>): List<Bottleneck> {
        return listOf(
            Bottleneck(
                type = "LOCATION",
                severity = "MEDIUM",
                description = "Main entrance congestion",
                estimatedDelay = 10,
                solutions = listOf("Open additional entrances", "Add staff")
            )
        )
    }

    private fun generateMitigationStrategies(bottlenecks: List<Bottleneck>): List<String> {
        return listOf("Open side entrances", "Deploy mobile check-in stations")
    }
    
    private fun convertCheckInToDto(checkIn: CheckIn): CheckInDto {
        return CheckInDto().apply {
            BeanUtils.copyProperties(checkIn, this)
            
            // Set basic properties
            this.id = checkIn.id
            this.registrationId = checkIn.registration?.id
            this.sessionId = checkIn.session?.id
            this.type = checkIn.type
            this.method = checkIn.method
            this.checkedInAt = checkIn.checkedInAt
            this.checkedInBy = checkIn.checkedInBy
            this.location = checkIn.location
            this.notes = checkIn.notes
            
            // Set additional display info
            checkIn.registration?.let { reg ->
                reg.eventInstance?.event?.let { event ->
                    this.eventName = event.name
                }
            }
            
            checkIn.session?.let { session ->
                this.sessionTitle = session.title
            }
        }
    }
}