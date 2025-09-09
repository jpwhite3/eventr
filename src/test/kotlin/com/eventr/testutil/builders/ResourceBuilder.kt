package com.eventr.testutil.builders

import com.eventr.model.*
import com.eventr.testutil.TestConstants
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Fluent builder for creating Resource entities in tests.
 * Provides sensible defaults and allows easy customization for different test scenarios.
 */
class ResourceBuilder {
    private var id: UUID = TestConstants.DEFAULT_RESOURCE_ID
    private var name: String = TestConstants.DEFAULT_RESOURCE_NAME
    private var type: ResourceType = ResourceType.ROOM
    private var description: String? = TestConstants.DEFAULT_RESOURCE_DESCRIPTION
    private var status: ResourceStatus = ResourceStatus.AVAILABLE
    private var isActive: Boolean = true
    private var isBookable: Boolean = true
    private var capacity: Int? = TestConstants.DEFAULT_RESOURCE_CAPACITY
    private var hourlyRate: BigDecimal? = BigDecimal.valueOf(TestConstants.DEFAULT_HOURLY_RATE)
    private var location: String? = TestConstants.DEFAULT_RESOURCE_LOCATION
    private var contactEmail: String? = TestConstants.DEFAULT_CONTACT_EMAIL
    private var contactPhone: String? = TestConstants.DEFAULT_CONTACT_PHONE
    private var specifications: String? = TestConstants.DEFAULT_RESOURCE_SPECS
    private var tags: String? = TestConstants.DEFAULT_RESOURCE_TAGS
    private var createdAt: LocalDateTime = TestConstants.BASE_TIME
    private var updatedAt: LocalDateTime = TestConstants.BASE_TIME
    
    // Additional fields that might be used
    private var maintenanceNotes: String? = null
    private var nextMaintenanceDate: LocalDateTime? = null
    private var lastUsedAt: LocalDateTime? = null
    private var usageThisMonth: Int = 0
    
    // ================================================================================
    // Basic Property Setters
    // ================================================================================
    
    fun withId(id: UUID) = apply { this.id = id }
    fun withName(name: String) = apply { this.name = name }
    fun withType(type: ResourceType) = apply { this.type = type }
    fun withDescription(description: String?) = apply { this.description = description }
    fun withStatus(status: ResourceStatus) = apply { this.status = status }
    fun withCapacity(capacity: Int?) = apply { this.capacity = capacity }
    fun withHourlyRate(rate: Double) = apply { this.hourlyRate = BigDecimal.valueOf(rate) }
    fun withHourlyRate(rate: BigDecimal) = apply { this.hourlyRate = rate }
    fun withLocation(location: String) = apply { this.location = location }
    
    // ================================================================================
    // Contact Information
    // ================================================================================
    
    fun withContactInfo(email: String, phone: String? = null) = apply {
        this.contactEmail = email
        this.contactPhone = phone
    }
    
    fun withContactEmail(email: String) = apply { this.contactEmail = email }
    fun withContactPhone(phone: String) = apply { this.contactPhone = phone }
    
    // ================================================================================
    // Specifications and Features
    // ================================================================================
    
    fun withSpecifications(specs: String) = apply { this.specifications = specs }
    fun withTags(tags: String) = apply { this.tags = tags }
    fun withTagsList(vararg tags: String) = apply { this.tags = tags.joinToString(", ") }
    
    fun withBasicSpecs() = apply {
        this.specifications = "Basic meeting room setup"
        this.tags = "meeting, basic"
    }
    
    fun withAdvancedSpecs() = apply {
        this.specifications = "Projector, Whiteboard, Video Conferencing, Sound System"
        this.tags = "conference, advanced, av-equipment"
    }
    
    fun withMinimalSpecs() = apply {
        this.specifications = null
        this.tags = null
    }
    
    // ================================================================================
    // Availability and Status
    // ================================================================================
    
    fun asAvailable() = apply { this.status = ResourceStatus.AVAILABLE }
    fun asOccupied() = apply { this.status = ResourceStatus.OCCUPIED }
    fun asMaintenance() = apply { this.status = ResourceStatus.MAINTENANCE }
    fun asReserved() = apply { this.status = ResourceStatus.RESERVED }
    fun asOutOfService() = apply { this.status = ResourceStatus.OUT_OF_SERVICE }
    
    fun asActive() = apply { this.isActive = true }
    fun asInactive() = apply { this.isActive = false }
    
    fun asBookable() = apply { this.isBookable = true }
    fun asNotBookable() = apply { this.isBookable = false }
    
    // ================================================================================
    // Resource Type Configurations
    // ================================================================================
    
    fun asRoom() = apply {
        this.type = ResourceType.ROOM
        this.name = TestConstants.ROOM_RESOURCE_NAME
        this.capacity = TestConstants.MEDIUM_CAPACITY
        this.hourlyRate = BigDecimal.valueOf(TestConstants.STANDARD_HOURLY_RATE)
        this.specifications = "Meeting room with table and chairs"
        this.tags = "room, meeting, table, chairs"
    }
    
    fun asConferenceRoom() = apply {
        this.type = ResourceType.ROOM
        this.name = TestConstants.DEFAULT_RESOURCE_NAME
        this.capacity = TestConstants.LARGE_CAPACITY
        this.hourlyRate = BigDecimal.valueOf(TestConstants.PREMIUM_HOURLY_RATE)
        this.specifications = TestConstants.DEFAULT_RESOURCE_SPECS
        this.tags = TestConstants.DEFAULT_RESOURCE_TAGS
    }
    
    fun asEquipment() = apply {
        this.type = ResourceType.EQUIPMENT
        this.name = TestConstants.EQUIPMENT_RESOURCE_NAME
        this.capacity = null // Equipment doesn't have capacity
        this.hourlyRate = BigDecimal.valueOf(TestConstants.LOW_HOURLY_RATE)
        this.specifications = "Portable projector with cables"
        this.tags = "equipment, projector, av"
    }
    
    fun asVehicle() = apply {
        this.type = ResourceType.VEHICLE
        this.name = TestConstants.VEHICLE_RESOURCE_NAME
        this.capacity = 8 // Passenger capacity
        this.hourlyRate = BigDecimal.valueOf(50.0)
        this.specifications = "8-passenger van with GPS"
        this.tags = "vehicle, transportation, van"
    }
    
    fun asStaff() = apply {
        this.type = ResourceType.STAFF
        this.name = "Event Coordinator"
        this.capacity = null // Staff doesn't have capacity
        this.hourlyRate = BigDecimal.valueOf(75.0)
        this.specifications = "Professional event coordination services"
        this.tags = "staff, coordinator, service"
    }
    
    fun asCatering() = apply {
        this.type = ResourceType.CATERING
        this.name = "Lunch Catering Service"
        this.capacity = TestConstants.LARGE_CAPACITY // People served
        this.hourlyRate = BigDecimal.valueOf(25.0) // Per person
        this.specifications = "Full lunch service with dietary options"
        this.tags = "catering, food, lunch, dietary"
    }
    
    fun asTechnology() = apply {
        this.type = ResourceType.TECHNOLOGY
        this.name = "Video Streaming Setup"
        this.capacity = null
        this.hourlyRate = BigDecimal.valueOf(150.0)
        this.specifications = "Professional streaming equipment with technical support"
        this.tags = "technology, streaming, video, support"
    }
    
    fun asOther() = apply {
        this.type = ResourceType.OTHER
        this.name = "Special Service"
        this.capacity = null
        this.hourlyRate = BigDecimal.valueOf(100.0)
        this.specifications = "Custom service as needed"
        this.tags = "other, custom, service"
    }
    
    // ================================================================================
    // Pricing Configurations
    // ================================================================================
    
    fun asFree() = apply { this.hourlyRate = null }
    fun asLowCost() = apply { this.hourlyRate = BigDecimal.valueOf(TestConstants.LOW_HOURLY_RATE) }
    fun asStandardRate() = apply { this.hourlyRate = BigDecimal.valueOf(TestConstants.STANDARD_HOURLY_RATE) }
    fun asPremiumRate() = apply { this.hourlyRate = BigDecimal.valueOf(TestConstants.PREMIUM_HOURLY_RATE) }
    
    // ================================================================================
    // Capacity Configurations
    // ================================================================================
    
    fun withSmallCapacity() = apply { this.capacity = TestConstants.SMALL_CAPACITY }
    fun withMediumCapacity() = apply { this.capacity = TestConstants.MEDIUM_CAPACITY }
    fun withLargeCapacity() = apply { this.capacity = TestConstants.LARGE_CAPACITY }
    fun withExtraLargeCapacity() = apply { this.capacity = TestConstants.EXTRA_LARGE_CAPACITY }
    fun withoutCapacity() = apply { this.capacity = null }
    
    // ================================================================================
    // Maintenance and Usage
    // ================================================================================
    
    fun withMaintenanceNotes(notes: String) = apply { this.maintenanceNotes = notes }
    fun withNextMaintenance(date: LocalDateTime) = apply { this.nextMaintenanceDate = date }
    fun withLastUsed(date: LocalDateTime) = apply { this.lastUsedAt = date }
    fun withUsageThisMonth(usage: Int) = apply { this.usageThisMonth = usage }
    
    fun needsMaintenance() = apply {
        this.status = ResourceStatus.MAINTENANCE
        this.isBookable = false
        this.maintenanceNotes = "Scheduled maintenance required"
        this.nextMaintenanceDate = TestConstants.BASE_TIME.plusDays(1)
    }
    
    fun recentlyUsed() = apply {
        this.lastUsedAt = TestConstants.BASE_TIME.minusHours(2)
        this.usageThisMonth = 15
    }
    
    fun underutilized() = apply {
        this.lastUsedAt = TestConstants.BASE_TIME.minusDays(30)
        this.usageThisMonth = 2
    }
    
    fun heavilyUsed() = apply {
        this.lastUsedAt = TestConstants.BASE_TIME.minusHours(1)
        this.usageThisMonth = 50
    }
    
    // ================================================================================
    // Time-based Configurations
    // ================================================================================
    
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }
    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }
    
    fun asNewResource() = apply {
        this.createdAt = TestConstants.BASE_TIME
        this.updatedAt = TestConstants.BASE_TIME
        this.lastUsedAt = null
        this.usageThisMonth = 0
    }
    
    fun asEstablishedResource() = apply {
        this.createdAt = TestConstants.BASE_TIME.minusMonths(6)
        this.updatedAt = TestConstants.BASE_TIME.minusDays(1)
        this.lastUsedAt = TestConstants.BASE_TIME.minusDays(2)
        this.usageThisMonth = 20
    }
    
    // ================================================================================
    // Relationship Builders
    // ================================================================================
    
    fun withBookings(count: Int): ResourceWithBookingsBuilder {
        return ResourceWithBookingsBuilder(this, count)
    }
    
    fun withBookingConflicts(): ResourceWithConflictsBuilder {
        return ResourceWithConflictsBuilder(this)
    }
    
    // ================================================================================
    // Test Scenario Configurations
    // ================================================================================
    
    fun forAvailabilityTesting() = apply {
        asRoom()
        asAvailable()
        asBookable()
        withMediumCapacity()
        asStandardRate()
    }
    
    fun forConflictTesting() = apply {
        asRoom()
        asOccupied() // Already has a conflict
        withMediumCapacity()
    }
    
    fun forMaintenanceTesting() = apply {
        asRoom()
        needsMaintenance()
    }
    
    fun forSearchTesting(searchTerm: String) = apply {
        withName("Resource containing $searchTerm")
        withDescription("Description with $searchTerm keyword")
        withTags("$searchTerm, searchable, test")
    }
    
    fun forBulkOperations(index: Int) = apply {
        withId(UUID.randomUUID())
        withName("Bulk Resource $index")
        asRoom()
        asAvailable()
        withCapacity(TestConstants.MEDIUM_CAPACITY + index)
    }
    
    // ================================================================================
    // Build Method
    // ================================================================================
    
    fun build(): Resource {
        return Resource(
            id = id,
            name = name
        ).apply {
            this.name = this@ResourceBuilder.name
            this.type = this@ResourceBuilder.type
            this.description = this@ResourceBuilder.description
            this.status = this@ResourceBuilder.status
            this.isActive = this@ResourceBuilder.isActive
            this.isBookable = this@ResourceBuilder.isBookable
            this.capacity = this@ResourceBuilder.capacity
            this.hourlyRate = this@ResourceBuilder.hourlyRate
            this.location = this@ResourceBuilder.location
            this.contactEmail = this@ResourceBuilder.contactEmail
            this.contactPhone = this@ResourceBuilder.contactPhone
            this.specifications = this@ResourceBuilder.specifications
            this.tags = this@ResourceBuilder.tags
            this.createdAt = this@ResourceBuilder.createdAt
            this.updatedAt = this@ResourceBuilder.updatedAt
        }
    }
}