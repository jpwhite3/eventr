package com.eventr.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class ResourceType {
    ROOM, EQUIPMENT, STAFF, VEHICLE, CATERING, TECHNOLOGY, OTHER
}

enum class ResourceStatus {
    AVAILABLE, OCCUPIED, MAINTENANCE, RESERVED, OUT_OF_SERVICE
}

@Entity
data class Resource(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    var name: String,
    var description: String? = null,
    
    @Enumerated(EnumType.STRING)
    var type: ResourceType = ResourceType.OTHER,
    
    @Enumerated(EnumType.STRING)
    var status: ResourceStatus = ResourceStatus.AVAILABLE,
    
    // Capacity and specifications
    var capacity: Int? = null, // For rooms, vehicles, etc.
    var location: String? = null,
    var floor: String? = null,
    var building: String? = null,
    
    // Equipment specifications
    var specifications: String? = null, // JSON for flexible specs
    var serialNumber: String? = null,
    var model: String? = null,
    var manufacturer: String? = null,
    
    // Availability and booking
    var isBookable: Boolean = true,
    var requiresApproval: Boolean = false,
    var bookingLeadTimeHours: Int = 0,
    var maxBookingDurationHours: Int? = null,
    
    // Cost information
    var hourlyRate: BigDecimal? = null,
    var dailyRate: BigDecimal? = null,
    var setupCost: BigDecimal? = null,
    var cleanupCost: BigDecimal? = null,
    
    // Maintenance and availability
    var lastMaintenanceDate: LocalDateTime? = null,
    var nextMaintenanceDate: LocalDateTime? = null,
    var maintenanceNotes: String? = null,
    
    // Contact and management
    var contactPerson: String? = null,
    var contactEmail: String? = null,
    var contactPhone: String? = null,
    var departmentOwner: String? = null,
    
    // Usage tracking
    var totalUsageHours: Int = 0,
    var usageThisMonth: Int = 0,
    var lastUsedAt: LocalDateTime? = null,
    
    // Tags and categories for filtering
    var tags: String? = null, // Comma-separated tags
    var category: String? = null,
    
    var isActive: Boolean = true,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
data class SessionResource(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    var session: Session? = null,
    
    @ManyToOne
    @JoinColumn(name = "resource_id", nullable = false)
    var resource: Resource? = null,
    
    var quantityNeeded: Int = 1,
    var quantityAllocated: Int = 0,
    
    // Timing details
    var setupTimeMinutes: Int = 0,
    var usageTimeMinutes: Int = 0, // 0 means use session duration
    var cleanupTimeMinutes: Int = 0,
    
    // Booking details
    var bookingStart: LocalDateTime? = null,
    var bookingEnd: LocalDateTime? = null,
    
    var isRequired: Boolean = true,
    var notes: String? = null,
    
    // Cost tracking
    var estimatedCost: BigDecimal? = null,
    var actualCost: BigDecimal? = null,
    
    var status: ResourceBookingStatus = ResourceBookingStatus.REQUESTED,
    
    var approvedBy: String? = null,
    var approvedAt: LocalDateTime? = null,
    
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ResourceBookingStatus {
    REQUESTED, APPROVED, ALLOCATED, IN_USE, COMPLETED, CANCELLED, CONFLICT
}