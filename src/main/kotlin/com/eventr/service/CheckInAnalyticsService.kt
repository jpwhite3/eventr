package com.eventr.service

import com.eventr.dto.*
import java.util.*

/**
 * Service interface for check-in analytics and statistics.
 * 
 * Focuses exclusively on check-in analytics:
 * - Event check-in statistics generation
 * - Real-time check-in metrics
 * - Check-in pattern analysis
 * - Performance metrics and insights
 * 
 * Follows Single Responsibility Principle by handling only analytics concerns.
 */
interface CheckInAnalyticsService {

    /**
     * Generate comprehensive check-in statistics for an event.
     * 
     * @param eventId Event ID
     * @return Detailed check-in statistics and metrics
     */
    fun getEventCheckInStats(eventId: UUID): CheckInStatsDto

    /**
     * Get real-time check-in metrics for ongoing event.
     * 
     * @param eventId Event ID
     * @return Real-time check-in metrics
     */
    fun getRealTimeCheckInMetrics(eventId: UUID): RealTimeCheckInMetrics

    /**
     * Analyze check-in patterns for event optimization.
     * 
     * @param eventId Event ID
     * @return Check-in pattern analysis with insights
     */
    fun analyzeCheckInPatterns(eventId: UUID): CheckInPatternAnalysis

    /**
     * Get session-specific check-in statistics.
     * 
     * @param sessionId Session ID
     * @return Session check-in statistics
     */
    fun getSessionCheckInStats(sessionId: UUID): SessionCheckInStats

    /**
     * Get check-in performance metrics for event staff.
     * 
     * @param eventId Event ID
     * @param timeRange Optional time range filter
     * @return Staff performance metrics
     */
    fun getStaffPerformanceMetrics(eventId: UUID, timeRange: TimeRange? = null): StaffPerformanceMetrics

    /**
     * Generate check-in trends analysis across multiple events.
     * 
     * @param eventIds List of event IDs to analyze
     * @return Trends analysis with comparisons
     */
    fun getCheckInTrends(eventIds: List<UUID>): CheckInTrendsAnalysis

    /**
     * Get check-in method effectiveness analysis.
     * 
     * @param eventId Event ID
     * @return Analysis of different check-in methods' effectiveness
     */
    fun getMethodEffectivenessAnalysis(eventId: UUID): MethodEffectivenessAnalysis

    /**
     * Calculate predicted check-in completion time.
     * 
     * @param eventId Event ID
     * @return Predicted completion time and bottleneck analysis
     */
    fun predictCheckInCompletion(eventId: UUID): CheckInPrediction
}

/**
 * Real-time check-in metrics.
 */
data class RealTimeCheckInMetrics(
    val eventId: UUID,
    val currentCheckIns: Int,
    val targetCheckIns: Int,
    val checkInRate: Double,        // Check-ins per minute
    val estimatedCompletion: String, // Estimated completion time
    val bottlenecks: List<String>,   // Identified bottlenecks
    val recommendations: List<String> // Performance recommendations
)

/**
 * Check-in pattern analysis.
 */
data class CheckInPatternAnalysis(
    val eventId: UUID,
    val peakTimes: List<PeakTimeWindow>,
    val methodDistribution: Map<CheckInMethod, MethodStats>,
    val locationHotspots: List<LocationHotspot>,
    val userBehaviorInsights: List<String>
)

/**
 * Peak time window analysis.
 */
data class PeakTimeWindow(
    val startTime: String,
    val endTime: String,
    val checkInCount: Int,
    val intensity: String // LOW, MEDIUM, HIGH, CRITICAL
)

/**
 * Method statistics.
 */
data class MethodStats(
    val count: Int,
    val percentage: Double,
    val averageProcessingTime: Double, // Seconds
    val successRate: Double
)

/**
 * Location hotspot analysis.
 */
data class LocationHotspot(
    val location: String,
    val checkInCount: Int,
    val congestionLevel: String, // LOW, MODERATE, HIGH
    val recommendedActions: List<String>
)

/**
 * Session check-in statistics.
 */
data class SessionCheckInStats(
    val sessionId: UUID,
    val sessionName: String,
    val totalRegistrations: Int,
    val checkedInCount: Int,
    val attendanceRate: Double,
    val onTimeCheckIns: Int,
    val lateCheckIns: Int,
    val averageCheckInTime: Double, // Minutes before session start
    val methodBreakdown: Map<CheckInMethod, Int>
)

/**
 * Staff performance metrics.
 */
data class StaffPerformanceMetrics(
    val eventId: UUID,
    val timeRange: TimeRange,
    val staffMetrics: Map<String, StaffStats>, // Staff ID -> Stats
    val overallEfficiency: Double,
    val recommendedOptimizations: List<String>
)

/**
 * Individual staff statistics.
 */
data class StaffStats(
    val staffId: String,
    val staffName: String,
    val checkInsProcessed: Int,
    val averageProcessingTime: Double, // Seconds per check-in
    val errorRate: Double,
    val efficiency: String // EXCELLENT, GOOD, AVERAGE, NEEDS_IMPROVEMENT
)

/**
 * Check-in trends analysis.
 */
data class CheckInTrendsAnalysis(
    val eventIds: List<UUID>,
    val overallTrend: String, // IMPROVING, STABLE, DECLINING
    val methodTrends: Map<CheckInMethod, String>,
    val seasonalPatterns: List<SeasonalPattern>,
    val benchmarks: CheckInBenchmarks
)

/**
 * Seasonal pattern analysis.
 */
data class SeasonalPattern(
    val period: String, // MONTHLY, QUARTERLY, etc.
    val pattern: String,
    val confidence: Double
)

/**
 * Check-in benchmarks.
 */
data class CheckInBenchmarks(
    val industryAverage: CheckInMetrics,
    val topPerformers: CheckInMetrics,
    val yourPerformance: CheckInMetrics
)

/**
 * Method effectiveness analysis.
 */
data class MethodEffectivenessAnalysis(
    val eventId: UUID,
    val methods: Map<CheckInMethod, MethodEffectiveness>,
    val recommendations: List<MethodRecommendation>
)

/**
 * Method effectiveness metrics.
 */
data class MethodEffectiveness(
    val method: CheckInMethod,
    val usagePercentage: Double,
    val successRate: Double,
    val averageTime: Double,
    val userSatisfaction: Double,
    val costEffectiveness: String,
    val scalability: String
)

/**
 * Method recommendation.
 */
data class MethodRecommendation(
    val method: CheckInMethod,
    val recommendation: String, // INCREASE, DECREASE, OPTIMIZE, ELIMINATE
    val reason: String,
    val expectedImpact: String
)

/**
 * Check-in prediction.
 */
data class CheckInPrediction(
    val eventId: UUID,
    val estimatedCompletionTime: String,
    val confidence: Double,
    val currentProgress: Double, // 0.0 to 1.0
    val bottlenecks: List<Bottleneck>,
    val mitigationStrategies: List<String>
)

/**
 * Bottleneck identification.
 */
data class Bottleneck(
    val type: String, // LOCATION, METHOD, STAFF, TECHNICAL
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val description: String,
    val estimatedDelay: Int, // Minutes
    val solutions: List<String>
)

/**
 * Time range for analysis.
 */
data class TimeRange(
    val start: String, // ISO datetime
    val end: String    // ISO datetime
)

/**
 * General check-in metrics.
 */
data class CheckInMetrics(
    val averageAttendanceRate: Double,
    val averageProcessingTime: Double,
    val methodDistribution: Map<CheckInMethod, Double>,
    val peakEfficiency: Double
)