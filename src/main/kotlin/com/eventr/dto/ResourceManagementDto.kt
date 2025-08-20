package com.eventr.dto

import com.eventr.model.ResourceBookingStatus
import com.eventr.model.ResourceStatus
import com.eventr.model.ResourceType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class ResourceDto(
    var id: UUID? = null,
    var name: String,
    var description: String? = null,
    var type: ResourceType = ResourceType.OTHER,
    var status: ResourceStatus = ResourceStatus.AVAILABLE,
    
    // Capacity and specifications
    var capacity: Int? = null,
    var location: String? = null,
    var floor: String? = null,
    var building: String? = null,
    var specifications: String? = null,
    var serialNumber: String? = null,
    var model: String? = null,
    var manufacturer: String? = null,
    
    // Availability
    var isBookable: Boolean = true,
    var requiresApproval: Boolean = false,
    var bookingLeadTimeHours: Int = 0,
    var maxBookingDurationHours: Int? = null,
    
    // Cost information
    var hourlyRate: BigDecimal? = null,
    var dailyRate: BigDecimal? = null,
    var setupCost: BigDecimal? = null,
    var cleanupCost: BigDecimal? = null,
    
    // Maintenance
    var lastMaintenanceDate: LocalDateTime? = null,
    var nextMaintenanceDate: LocalDateTime? = null,
    var maintenanceNotes: String? = null,
    var daysSinceLastMaintenance: Long? = null,
    var daysUntilMaintenance: Long? = null,
    
    // Contact and management
    var contactPerson: String? = null,
    var contactEmail: String? = null,
    var contactPhone: String? = null,
    var departmentOwner: String? = null,
    
    // Usage statistics
    var totalUsageHours: Int = 0,
    var usageThisMonth: Int = 0,
    var lastUsedAt: LocalDateTime? = null,
    var utilizationRate: Double = 0.0,
    
    // Categories and tags
    var tags: List<String> = emptyList(),
    var category: String? = null,
    
    // Availability status
    var isAvailable: Boolean = true,
    var currentBookings: Int = 0,
    var upcomingBookings: Int = 0
)

data class ResourceCreateDto(
    var name: String,
    var description: String? = null,
    var type: ResourceType = ResourceType.OTHER,
    var capacity: Int? = null,
    var location: String? = null,
    var floor: String? = null,
    var building: String? = null,
    var specifications: String? = null,
    var serialNumber: String? = null,
    var model: String? = null,
    var manufacturer: String? = null,
    var isBookable: Boolean = true,
    var requiresApproval: Boolean = false,
    var bookingLeadTimeHours: Int = 0,
    var maxBookingDurationHours: Int? = null,
    var hourlyRate: BigDecimal? = null,
    var dailyRate: BigDecimal? = null,
    var setupCost: BigDecimal? = null,
    var cleanupCost: BigDecimal? = null,
    var contactPerson: String? = null,
    var contactEmail: String? = null,
    var contactPhone: String? = null,
    var departmentOwner: String? = null,
    var tags: List<String> = emptyList(),
    var category: String? = null
)

data class SessionResourceDto(
    var id: UUID? = null,
    var sessionId: UUID? = null,
    var sessionTitle: String? = null,
    var resourceId: UUID,
    var resourceName: String? = null,
    var resourceType: ResourceType? = null,
    var quantityNeeded: Int = 1,
    var quantityAllocated: Int = 0,
    
    // Timing details
    var setupTimeMinutes: Int = 0,
    var usageTimeMinutes: Int = 0,
    var cleanupTimeMinutes: Int = 0,
    var bookingStart: LocalDateTime? = null,
    var bookingEnd: LocalDateTime? = null,
    
    var isRequired: Boolean = true,
    var notes: String? = null,
    
    // Cost tracking
    var estimatedCost: BigDecimal? = null,
    var actualCost: BigDecimal? = null,
    
    var status: ResourceBookingStatus = ResourceBookingStatus.REQUESTED,
    var approvedBy: String? = null,
    var approvedAt: LocalDateTime? = null
)

data class ResourceBookingDto(
    var sessionId: UUID,
    var resourceId: UUID,
    var quantityNeeded: Int = 1,
    var setupTimeMinutes: Int = 0,
    var usageTimeMinutes: Int = 0, // 0 means use session duration
    var cleanupTimeMinutes: Int = 0,
    var isRequired: Boolean = true,
    var notes: String? = null,
    var estimatedCost: BigDecimal? = null
)

data class ResourceAvailabilityDto(
    var resourceId: UUID,
    var resourceName: String,
    var resourceType: ResourceType,
    var isAvailable: Boolean = true,
    var availableQuantity: Int = 0,
    var totalQuantity: Int = 1,
    var conflictingBookings: List<ResourceConflictDto> = emptyList(),
    var nextAvailableSlot: LocalDateTime? = null,
    var availabilityWindows: List<AvailabilityWindowDto> = emptyList()
)

data class ResourceConflictDto(
    var sessionId: UUID,
    var sessionTitle: String,
    var bookingStart: LocalDateTime,
    var bookingEnd: LocalDateTime,
    var quantityAllocated: Int,
    var status: ResourceBookingStatus
)

data class AvailabilityWindowDto(
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,
    var availableQuantity: Int
)

data class ResourceSearchDto(
    var searchTerm: String? = null,
    var type: ResourceType? = null,
    var location: String? = null,
    var minCapacity: Int? = null,
    var maxCapacity: Int? = null,
    var availableFrom: LocalDateTime? = null,
    var availableTo: LocalDateTime? = null,
    var tags: List<String> = emptyList(),
    var includeUnavailable: Boolean = false,
    var sortBy: String = "name", // name, type, capacity, availability
    var sortOrder: String = "asc" // asc, desc
)

data class ResourceUtilizationDto(
    var resourceId: UUID,
    var resourceName: String,
    var resourceType: ResourceType,
    var totalHours: Int = 0,
    var bookedHours: Int = 0,
    var utilizationPercentage: Double = 0.0,
    var totalBookings: Int = 0,
    var averageBookingDuration: Double = 0.0,
    var peakUsagePeriods: List<String> = emptyList(),
    var monthlyUsage: Map<String, Int> = emptyMap(),
    var costEfficiency: BigDecimal? = null
)

data class ResourceAnalyticsDto(
    var totalResources: Int = 0,
    var resourcesByType: Map<ResourceType, Int> = emptyMap(),
    var resourcesByStatus: Map<ResourceStatus, Int> = emptyMap(),
    var averageUtilization: Double = 0.0,
    var mostUtilizedResources: List<ResourceUtilizationDto> = emptyList(),
    var underutilizedResources: List<ResourceUtilizationDto> = emptyList(),
    var resourcesDueForMaintenance: Int = 0,
    var totalResourceCosts: BigDecimal? = null,
    var costByResourceType: Map<ResourceType, BigDecimal> = emptyMap(),
    var bookingTrends: List<BookingTrendDto> = emptyList()
)

data class BookingTrendDto(
    var date: LocalDateTime,
    var totalBookings: Int,
    var uniqueResources: Int,
    var averageBookingDuration: Double,
    var totalCost: BigDecimal?
)