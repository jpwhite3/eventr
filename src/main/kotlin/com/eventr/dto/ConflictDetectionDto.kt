package com.eventr.dto

import com.eventr.model.ConflictResolutionStatus
import com.eventr.model.ConflictSeverity
import com.eventr.model.ConflictType
import java.time.LocalDateTime
import java.util.*

data class ScheduleConflictDto(
    var id: UUID? = null,
    var type: ConflictType = ConflictType.TIME_OVERLAP,
    var severity: ConflictSeverity = ConflictSeverity.WARNING,
    var title: String,
    var description: String,
    
    // Related entities
    var primarySessionId: UUID? = null,
    var primarySessionTitle: String? = null,
    var secondarySessionId: UUID? = null,
    var secondarySessionTitle: String? = null,
    var resourceId: UUID? = null,
    var resourceName: String? = null,
    var registrationId: UUID? = null,
    var userEmail: String? = null,
    
    // Conflict details
    var conflictStart: LocalDateTime? = null,
    var conflictEnd: LocalDateTime? = null,
    var affectedCount: Int = 0,
    var duration: String? = null, // Human-readable duration
    
    // Resolution
    var resolutionStatus: ConflictResolutionStatus = ConflictResolutionStatus.UNRESOLVED,
    var resolutionNotes: String? = null,
    var resolvedBy: String? = null,
    var resolvedAt: LocalDateTime? = null,
    
    // Auto-resolution
    var canAutoResolve: Boolean = false,
    var autoResolutionStrategy: String? = null,
    var suggestedResolutions: List<String> = emptyList(),
    
    // Timing
    var detectedAt: LocalDateTime = LocalDateTime.now(),
    var lastCheckedAt: LocalDateTime = LocalDateTime.now(),
    var ageInHours: Long = 0,
    
    // Notifications
    var notificationsSent: Boolean = false,
    var notificationRecipients: List<String> = emptyList(),
    
    var isActive: Boolean = true
)

data class ConflictResolutionDto(
    var id: UUID? = null,
    var conflictId: UUID,
    var resolutionType: String,
    var description: String,
    var changesSummary: String? = null,
    var beforeState: String? = null,
    var afterState: String? = null,
    var implementedBy: String,
    var implementedAt: LocalDateTime = LocalDateTime.now(),
    var affectedSessions: Int = 0,
    var affectedRegistrations: Int = 0,
    var affectedResources: Int = 0,
    var notes: String? = null,
    var effectiveness: String? = null // SUCCESS, PARTIAL, FAILED
)

data class ConflictDetectionRequestDto(
    var sessionId: UUID? = null,
    var eventId: UUID? = null,
    var registrationId: UUID? = null,
    var resourceId: UUID? = null,
    var startTime: LocalDateTime? = null,
    var endTime: LocalDateTime? = null,
    var conflictTypes: List<ConflictType> = emptyList(),
    var includePotentialConflicts: Boolean = false
)

data class ConflictSummaryDto(
    var eventId: UUID? = null,
    var totalConflicts: Int = 0,
    var unresolvedConflicts: Int = 0,
    var criticalConflicts: Int = 0,
    var autoResolvableConflicts: Int = 0,
    var conflictsByType: Map<ConflictType, Int> = emptyMap(),
    var conflictsBySeverity: Map<ConflictSeverity, Int> = emptyMap(),
    var oldestUnresolvedConflict: LocalDateTime? = null,
    var averageResolutionTimeHours: Double = 0.0,
    var conflictTrends: List<ConflictTrendDto> = emptyList()
)

data class ConflictTrendDto(
    var date: LocalDateTime,
    var newConflicts: Int,
    var resolvedConflicts: Int,
    var activeConflicts: Int
)

data class ConflictResolutionSuggestionDto(
    var conflictId: UUID,
    var suggestionType: String, // "TIME_CHANGE", "RESOURCE_SWAP", "CAPACITY_INCREASE", etc.
    var title: String,
    var description: String,
    var impact: String, // "LOW", "MEDIUM", "HIGH"
    var feasibility: String, // "EASY", "MODERATE", "DIFFICULT"
    var estimatedEffort: String,
    var potentialSideEffects: List<String> = emptyList(),
    var requiredApprovals: List<String> = emptyList(),
    var canAutoImplement: Boolean = false,
    var priority: Int = 0
)

data class ConflictAnalyticsDto(
    var eventId: UUID,
    var analysisDate: LocalDateTime = LocalDateTime.now(),
    var totalSessions: Int = 0,
    var totalResources: Int = 0,
    var totalRegistrations: Int = 0,
    
    // Conflict statistics
    var conflictRate: Double = 0.0, // Conflicts per session
    var resolutionRate: Double = 0.0, // Percentage of conflicts resolved
    var averageResolutionTime: Double = 0.0, // Hours
    
    // Common patterns
    var mostCommonConflictType: ConflictType? = null,
    var peakConflictPeriods: List<String> = emptyList(),
    var conflictHotspots: List<ConflictHotspotDto> = emptyList(),
    
    // Resource analysis
    var mostConflictedResources: List<ResourceConflictSummaryDto> = emptyList(),
    var sessionsWithMostConflicts: List<SessionConflictSummaryDto> = emptyList(),
    
    // Prevention insights
    var preventionRecommendations: List<String> = emptyList(),
    var schedulingOptimizationSuggestions: List<String> = emptyList()
)

data class ConflictHotspotDto(
    var location: String?,
    var timeSlot: String,
    var conflictCount: Int,
    var affectedSessions: List<String>
)

data class ResourceConflictSummaryDto(
    var resourceId: UUID,
    var resourceName: String,
    var resourceType: String,
    var conflictCount: Int,
    var utilizationRate: Double,
    var averageConflictDuration: Double
)

data class SessionConflictSummaryDto(
    var sessionId: UUID,
    var sessionTitle: String,
    var conflictCount: Int,
    var conflictTypes: List<ConflictType>,
    var registrationCount: Int
)

data class ConflictNotificationDto(
    var conflictId: UUID,
    var recipients: List<String>,
    var subject: String,
    var message: String,
    var priority: String, // LOW, NORMAL, HIGH, URGENT
    var includeResolutionSuggestions: Boolean = true,
    var sendImmediately: Boolean = false
)