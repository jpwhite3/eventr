package com.eventr.dto

import com.eventr.model.CapacityType
import com.eventr.model.WaitlistStrategy
import java.time.LocalDateTime
import java.util.*

data class SessionCapacityDto(
    var id: UUID? = null,
    var sessionId: UUID? = null,
    var sessionTitle: String? = null,
    var capacityType: CapacityType = CapacityType.FIXED,
    var maximumCapacity: Int = 0,
    var minimumCapacity: Int = 0,
    var currentRegistrations: Int = 0,
    var availableSpots: Int = 0,
    var utilizationPercentage: Double = 0.0,
    
    // Waitlist management
    var enableWaitlist: Boolean = true,
    var waitlistCapacity: Int? = null,
    var currentWaitlistCount: Int = 0,
    var waitlistStrategy: WaitlistStrategy = WaitlistStrategy.FIFO,
    
    // Dynamic capacity settings
    var allowOverbooking: Boolean = false,
    var overbookingPercentage: Double = 0.0,
    var autoPromoteFromWaitlist: Boolean = true,
    
    // Alert thresholds
    var lowCapacityThreshold: Int = 5,
    var highDemandThreshold: Double = 0.8,
    var isLowCapacity: Boolean = false,
    var isHighDemand: Boolean = false,
    
    var lastCapacityUpdate: LocalDateTime? = null
)

data class CapacityUpdateDto(
    var sessionId: UUID,
    var maximumCapacity: Int? = null,
    var minimumCapacity: Int? = null,
    var capacityType: CapacityType? = null,
    var enableWaitlist: Boolean? = null,
    var waitlistCapacity: Int? = null,
    var waitlistStrategy: WaitlistStrategy? = null,
    var allowOverbooking: Boolean? = null,
    var overbookingPercentage: Double? = null,
    var reason: String? = null
)

data class WaitlistPromotionDto(
    var sessionId: UUID,
    var registrationIds: List<UUID>,
    var promotionReason: String? = null,
    var notifyAttendees: Boolean = true
)

data class CapacityAnalyticsDto(
    var eventId: UUID,
    var eventName: String,
    var totalSessions: Int = 0,
    var averageUtilization: Double = 0.0,
    var fullSessionsCount: Int = 0,
    var underCapacitySessionsCount: Int = 0,
    var totalWaitlistCount: Int = 0,
    var overbookedSessionsCount: Int = 0,
    
    // Session breakdown
    var sessionCapacities: List<SessionCapacityDto> = emptyList(),
    var capacityTrends: List<CapacityTrendDto> = emptyList(),
    var waitlistDistribution: Map<WaitlistStrategy, Int> = emptyMap(),
    
    var generatedAt: LocalDateTime = LocalDateTime.now()
)

data class CapacityTrendDto(
    var sessionId: UUID,
    var sessionTitle: String,
    var timeSlot: LocalDateTime,
    var registrationCount: Int,
    var capacity: Int,
    var utilizationPercentage: Double,
    var waitlistCount: Int
)

data class CapacityOptimizationSuggestionDto(
    var sessionId: UUID,
    var sessionTitle: String,
    var currentCapacity: Int,
    var currentRegistrations: Int,
    var suggestedCapacity: Int,
    var optimizationType: String, // "INCREASE", "DECREASE", "REDISTRIBUTE"
    var reason: String,
    var potentialImpact: String,
    var priority: String // "LOW", "MEDIUM", "HIGH"
)