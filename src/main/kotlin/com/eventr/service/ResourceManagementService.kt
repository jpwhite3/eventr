package com.eventr.service

import com.eventr.dto.ResourceDto as DtoResourceDto
import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class ResourceManagementService(
    private val resourceRepository: ResourceRepository,
    private val sessionResourceRepository: SessionResourceRepository,
    private val sessionRepository: SessionRepository
) {

    fun getAllResources(): List<DtoResourceDto> {
        return resourceRepository.findAll().map { resource ->
            DtoResourceDto(
                id = resource.id,
                name = resource.name,
                description = resource.description,
                type = resource.type,
                status = resource.status,
                capacity = resource.capacity,
                location = resource.location,
                floor = resource.floor,
                building = resource.building,
                specifications = resource.specifications,
                serialNumber = resource.serialNumber,
                model = resource.model,
                manufacturer = resource.manufacturer,
                isBookable = resource.isBookable,
                requiresApproval = resource.requiresApproval,
                bookingLeadTimeHours = resource.bookingLeadTimeHours,
                maxBookingDurationHours = resource.maxBookingDurationHours,
                hourlyRate = resource.hourlyRate,
                dailyRate = resource.dailyRate,
                setupCost = resource.setupCost,
                cleanupCost = resource.cleanupCost,
                lastMaintenanceDate = resource.lastMaintenanceDate,
                nextMaintenanceDate = resource.nextMaintenanceDate,
                maintenanceNotes = resource.maintenanceNotes,
                contactPerson = resource.contactPerson,
                contactEmail = resource.contactEmail,
                contactPhone = resource.contactPhone,
                departmentOwner = resource.departmentOwner,
                totalUsageHours = resource.totalUsageHours,
                usageThisMonth = resource.usageThisMonth,
                lastUsedAt = resource.lastUsedAt,
                tags = resource.tags?.split(",")?.map { it.trim() } ?: emptyList(),
                category = resource.category
            )
        }
    }

    fun getResourceById(resourceId: UUID): DtoResourceDto {
        val resource = resourceRepository.findById(resourceId)
            .orElseThrow { IllegalArgumentException("Resource not found with id: $resourceId") }
        
        return DtoResourceDto(
            id = resource.id,
            name = resource.name,
            description = resource.description,
            type = resource.type,
            status = resource.status,
            capacity = resource.capacity,
            location = resource.location,
            floor = resource.floor,
            building = resource.building,
            specifications = resource.specifications,
            serialNumber = resource.serialNumber,
            model = resource.model,
            manufacturer = resource.manufacturer,
            isBookable = resource.isBookable,
            requiresApproval = resource.requiresApproval,
            bookingLeadTimeHours = resource.bookingLeadTimeHours,
            maxBookingDurationHours = resource.maxBookingDurationHours,
            hourlyRate = resource.hourlyRate,
            dailyRate = resource.dailyRate,
            setupCost = resource.setupCost,
            cleanupCost = resource.cleanupCost,
            lastMaintenanceDate = resource.lastMaintenanceDate,
            nextMaintenanceDate = resource.nextMaintenanceDate,
            maintenanceNotes = resource.maintenanceNotes,
            contactPerson = resource.contactPerson,
            contactEmail = resource.contactEmail,
            contactPhone = resource.contactPhone,
            departmentOwner = resource.departmentOwner,
            totalUsageHours = resource.totalUsageHours,
            usageThisMonth = resource.usageThisMonth,
            lastUsedAt = resource.lastUsedAt,
            tags = resource.tags?.split(",")?.map { it.trim() } ?: emptyList(),
            category = resource.category
        )
    }

    fun createResource(createDto: ResourceCreateDto): DtoResourceDto {
        val resource = Resource(
            name = createDto.name,
            description = createDto.description,
            type = createDto.type,
            capacity = createDto.capacity,
            location = createDto.location,
            floor = createDto.floor,
            building = createDto.building,
            specifications = createDto.specifications,
            serialNumber = createDto.serialNumber,
            model = createDto.model,
            manufacturer = createDto.manufacturer,
            isBookable = createDto.isBookable,
            requiresApproval = createDto.requiresApproval,
            bookingLeadTimeHours = createDto.bookingLeadTimeHours,
            maxBookingDurationHours = createDto.maxBookingDurationHours,
            hourlyRate = createDto.hourlyRate,
            dailyRate = createDto.dailyRate,
            setupCost = createDto.setupCost,
            cleanupCost = createDto.cleanupCost,
            contactPerson = createDto.contactPerson,
            contactEmail = createDto.contactEmail,
            contactPhone = createDto.contactPhone,
            departmentOwner = createDto.departmentOwner,
            tags = createDto.tags.joinToString(","),
            category = createDto.category
        )
        
        val saved = resourceRepository.save(resource)
        return convertToDto(saved)
    }

    fun updateResource(resourceId: UUID, updateDto: ResourceCreateDto): DtoResourceDto {
        val resource = resourceRepository.findById(resourceId)
            .orElseThrow { IllegalArgumentException("Resource not found") }
        
        resource.apply {
            name = updateDto.name
            description = updateDto.description
            type = updateDto.type
            capacity = updateDto.capacity
            location = updateDto.location
            floor = updateDto.floor
            building = updateDto.building
            specifications = updateDto.specifications
            serialNumber = updateDto.serialNumber
            model = updateDto.model
            manufacturer = updateDto.manufacturer
            isBookable = updateDto.isBookable
            requiresApproval = updateDto.requiresApproval
            bookingLeadTimeHours = updateDto.bookingLeadTimeHours
            maxBookingDurationHours = updateDto.maxBookingDurationHours
            hourlyRate = updateDto.hourlyRate
            dailyRate = updateDto.dailyRate
            setupCost = updateDto.setupCost
            cleanupCost = updateDto.cleanupCost
            contactPerson = updateDto.contactPerson
            contactEmail = updateDto.contactEmail
            contactPhone = updateDto.contactPhone
            departmentOwner = updateDto.departmentOwner
            tags = updateDto.tags.joinToString(",")
            category = updateDto.category
            updatedAt = LocalDateTime.now()
        }
        
        val saved = resourceRepository.save(resource)
        return convertToDto(saved)
    }

    fun bookResourceForSession(sessionId: UUID, bookingDto: ResourceBookingDto): SessionResourceDto {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val resource = resourceRepository.findById(bookingDto.resourceId)
            .orElseThrow { IllegalArgumentException("Resource not found") }
        
        // Validate booking constraints
        validateResourceBooking(resource, session, bookingDto)
        
        // Check for conflicts
        val conflicts = findResourceConflicts(bookingDto.resourceId, session.startTime!!, session.endTime!!)
        if (conflicts.isNotEmpty()) {
            throw IllegalArgumentException("Resource is not available during session time. Conflicts: ${conflicts.size}")
        }
        
        // Calculate booking times including setup/cleanup
        val setupTime = if (bookingDto.setupTimeMinutes > 0) 
            session.startTime!!.minusMinutes(bookingDto.setupTimeMinutes.toLong()) else session.startTime!!
        val cleanupTime = if (bookingDto.cleanupTimeMinutes > 0) 
            session.endTime!!.plusMinutes(bookingDto.cleanupTimeMinutes.toLong()) else session.endTime!!
        
        val sessionResource = SessionResource().apply {
            this.session = session
            this.resource = resource
            quantityNeeded = bookingDto.quantityNeeded
            quantityAllocated = if (resource.requiresApproval) 0 else bookingDto.quantityNeeded
            setupTimeMinutes = bookingDto.setupTimeMinutes
            usageTimeMinutes = bookingDto.usageTimeMinutes.takeIf { it > 0 } 
                ?: ChronoUnit.MINUTES.between(session.startTime, session.endTime).toInt()
            cleanupTimeMinutes = bookingDto.cleanupTimeMinutes
            bookingStart = setupTime
            bookingEnd = cleanupTime
            isRequired = bookingDto.isRequired
            notes = bookingDto.notes
            estimatedCost = calculateEstimatedCost(resource, this)
            status = if (resource.requiresApproval) ResourceBookingStatus.REQUESTED else ResourceBookingStatus.ALLOCATED
        }
        
        val saved = sessionResourceRepository.save(sessionResource)
        
        // Update resource usage tracking
        updateResourceUsage(resource, saved)
        
        return convertSessionResourceToDto(saved)
    }

    fun findAvailableResources(searchDto: ResourceSearchDto): List<ResourceAvailabilityDto> {
        val availableResources = if (searchDto.availableFrom != null && searchDto.availableTo != null) {
            resourceRepository.findAvailableResources(
                searchDto.availableFrom!!,
                searchDto.availableTo!!,
                searchDto.location,
                searchDto.minCapacity
            )
        } else {
            resourceRepository.findByIsBookableTrueAndIsActiveTrueAndStatus(ResourceStatus.AVAILABLE)
        }
        
        return availableResources.filter { resource ->
            matchesSearchCriteria(resource, searchDto)
        }.map { resource ->
            val availability = calculateResourceAvailability(
                resource, 
                searchDto.availableFrom, 
                searchDto.availableTo
            )
            availability
        }
    }

    fun getResourceUtilization(resourceId: UUID, startDate: LocalDateTime, endDate: LocalDateTime): ResourceUtilizationDto {
        val resource = resourceRepository.findById(resourceId)
            .orElseThrow { IllegalArgumentException("Resource not found") }
        
        val bookings = sessionResourceRepository.findResourceUsageDuringPeriod(resourceId, startDate, endDate)
        
        val totalHours = ChronoUnit.HOURS.between(startDate, endDate).toInt()
        val bookedHours = bookings.sumOf { booking ->
            ChronoUnit.HOURS.between(booking.bookingStart, booking.bookingEnd).toInt()
        }
        
        val utilizationPercentage = if (totalHours > 0) {
            (bookedHours.toDouble() / totalHours.toDouble()) * 100
        } else 0.0
        
        val averageBookingDuration = if (bookings.isNotEmpty()) {
            bookings.map { 
                ChronoUnit.HOURS.between(it.bookingStart, it.bookingEnd).toDouble() 
            }.average()
        } else 0.0
        
        return ResourceUtilizationDto(
            resourceId = resourceId,
            resourceName = resource.name,
            resourceType = resource.type,
            totalHours = totalHours,
            bookedHours = bookedHours,
            utilizationPercentage = utilizationPercentage,
            totalBookings = bookings.size,
            averageBookingDuration = averageBookingDuration,
            peakUsagePeriods = identifyPeakUsagePeriods(bookings),
            monthlyUsage = calculateMonthlyUsage(bookings),
            costEfficiency = calculateCostEfficiency(resource, bookings)
        )
    }

    fun getResourceAnalytics(startDate: LocalDateTime, endDate: LocalDateTime): ResourceAnalyticsDto {
        val allResources = resourceRepository.findByIsActiveTrue()
        // TODO: Implement utilization and demand statistics
        // val utilizationStats = resourceRepository.getResourceUtilizationByType()
        // val demandStats = sessionResourceRepository.getResourceDemandStatistics()
        
        val resourcesByType = allResources.groupBy { it.type }
            .mapValues { it.value.size }
        
        val resourcesByStatus = allResources.groupBy { it.status }
            .mapValues { it.value.size }
        
        val utilizationData = allResources.map { resource ->
            getResourceUtilization(resource.id!!, startDate, endDate)
        }
        
        val averageUtilization = utilizationData.map { it.utilizationPercentage }.average()
        
        val mostUtilized = utilizationData
            .sortedByDescending { it.utilizationPercentage }
            .take(10)
        
        val underutilized = utilizationData
            .filter { it.utilizationPercentage < 30.0 }
            .sortedBy { it.utilizationPercentage }
        
        val resourcesDueForMaintenance = resourceRepository
            .findResourcesDueForMaintenance(LocalDateTime.now().plusDays(30))
            .size
        
        return ResourceAnalyticsDto(
            totalResources = allResources.size,
            resourcesByType = resourcesByType,
            resourcesByStatus = resourcesByStatus,
            averageUtilization = averageUtilization,
            mostUtilizedResources = mostUtilized,
            underutilizedResources = underutilized,
            resourcesDueForMaintenance = resourcesDueForMaintenance,
            totalResourceCosts = calculateTotalResourceCosts(startDate, endDate),
            costByResourceType = calculateCostByResourceType(startDate, endDate),
            bookingTrends = generateBookingTrends(startDate, endDate)
        )
    }

    fun approveResourceBooking(sessionResourceId: UUID, approverName: String): SessionResourceDto {
        val sessionResource = sessionResourceRepository.findById(sessionResourceId)
            .orElseThrow { IllegalArgumentException("Session resource booking not found") }
        
        if (sessionResource.status != ResourceBookingStatus.REQUESTED) {
            throw IllegalArgumentException("Booking is not in requested status")
        }
        
        // Check for conflicts again
        val conflicts = findResourceConflicts(
            sessionResource.resource!!.id!!, 
            sessionResource.bookingStart!!, 
            sessionResource.bookingEnd!!
        )
        
        if (conflicts.any { it.status == ResourceBookingStatus.ALLOCATED }) {
            throw IllegalArgumentException("Resource has conflicting approved bookings")
        }
        
        sessionResource.apply {
            status = ResourceBookingStatus.APPROVED
            quantityAllocated = quantityNeeded
            approvedBy = approverName
            approvedAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }
        
        val saved = sessionResourceRepository.save(sessionResource)
        updateResourceUsage(sessionResource.resource!!, saved)
        
        return convertSessionResourceToDto(saved)
    }

    private fun validateResourceBooking(resource: Resource, session: Session, bookingDto: ResourceBookingDto) {
        if (!resource.isBookable) {
            throw IllegalArgumentException("Resource '${resource.name}' is not bookable")
        }
        
        if (!resource.isActive) {
            throw IllegalArgumentException("Resource '${resource.name}' is not active")
        }
        
        if (resource.status != ResourceStatus.AVAILABLE) {
            throw IllegalArgumentException("Resource '${resource.name}' is currently ${resource.status}")
        }
        
        // Check lead time
        val hoursUntilSession = ChronoUnit.HOURS.between(LocalDateTime.now(), session.startTime)
        if (hoursUntilSession < resource.bookingLeadTimeHours) {
            throw IllegalArgumentException("Resource requires ${resource.bookingLeadTimeHours} hours lead time")
        }
        
        // Check max booking duration
        resource.maxBookingDurationHours?.let { maxHours ->
            val sessionDuration = ChronoUnit.HOURS.between(session.startTime, session.endTime)
            if (sessionDuration > maxHours) {
                throw IllegalArgumentException("Session duration exceeds maximum booking duration of $maxHours hours")
            }
        }
        
        // Check capacity
        resource.capacity?.let { capacity ->
            if (bookingDto.quantityNeeded > capacity) {
                throw IllegalArgumentException("Requested quantity (${bookingDto.quantityNeeded}) exceeds resource capacity ($capacity)")
            }
        }
    }

    private fun findResourceConflicts(resourceId: UUID, startTime: LocalDateTime, endTime: LocalDateTime): List<SessionResource> {
        return sessionResourceRepository.findConflictingBookings(resourceId, startTime, endTime)
    }

    private fun calculateResourceAvailability(
        resource: Resource, 
        startTime: LocalDateTime?, 
        endTime: LocalDateTime?
    ): ResourceAvailabilityDto {
        
        val conflicts = if (startTime != null && endTime != null) {
            findResourceConflicts(resource.id!!, startTime, endTime)
        } else emptyList()
        
        val conflictDtos = conflicts.map { conflict ->
            ResourceConflictDto(
                sessionId = conflict.session!!.id!!,
                sessionTitle = conflict.session!!.title,
                bookingStart = conflict.bookingStart!!,
                bookingEnd = conflict.bookingEnd!!,
                quantityAllocated = conflict.quantityAllocated,
                status = conflict.status
            )
        }
        
        val totalQuantity = resource.capacity ?: 1
        val allocatedQuantity = conflicts.sumOf { it.quantityAllocated }
        val availableQuantity = maxOf(0, totalQuantity - allocatedQuantity)
        
        return ResourceAvailabilityDto(
            resourceId = resource.id!!,
            resourceName = resource.name,
            resourceType = resource.type,
            isAvailable = availableQuantity > 0 && resource.status == ResourceStatus.AVAILABLE,
            availableQuantity = availableQuantity,
            totalQuantity = totalQuantity,
            conflictingBookings = conflictDtos,
            nextAvailableSlot = findNextAvailableSlot(resource, conflicts)
        )
    }

    private fun matchesSearchCriteria(resource: Resource, searchDto: ResourceSearchDto): Boolean {
        searchDto.searchTerm?.let { searchTerm ->
            if (searchTerm.isNotBlank()) {
                val term = searchTerm.lowercase()
            val matches = resource.name.lowercase().contains(term) ||
                    resource.description?.lowercase()?.contains(term) == true ||
                    resource.location?.lowercase()?.contains(term) == true ||
                    resource.tags?.lowercase()?.contains(term) == true
                if (!matches) return false
            }
        }
        
        if (searchDto.type != null && resource.type != searchDto.type) return false
        if (searchDto.location != null && resource.location != searchDto.location) return false
        searchDto.minCapacity?.let { minCapacity ->
            if ((resource.capacity ?: 0) < minCapacity) return false
        }
        searchDto.maxCapacity?.let { maxCapacity ->
            if ((resource.capacity ?: Int.MAX_VALUE) > maxCapacity) return false
        }
        
        if (searchDto.tags.isNotEmpty()) {
            val resourceTags = resource.tags?.split(",")?.map { it.trim() } ?: emptyList()
            if (!resourceTags.any { it in searchDto.tags }) return false
        }
        
        if (!searchDto.includeUnavailable && resource.status != ResourceStatus.AVAILABLE) return false
        
        return true
    }

    private fun calculateEstimatedCost(resource: Resource, sessionResource: SessionResource): BigDecimal? {
        val hourlyRate = resource.hourlyRate ?: return null
        val usageHours = sessionResource.usageTimeMinutes / 60.0
        
        var cost = hourlyRate.multiply(BigDecimal(usageHours))
        
        resource.setupCost?.let { cost = cost.add(it) }
        resource.cleanupCost?.let { cost = cost.add(it) }
        
        return cost
    }

    private fun updateResourceUsage(resource: Resource, sessionResource: SessionResource) {
        val usageHours = sessionResource.usageTimeMinutes / 60
        resource.totalUsageHours += usageHours
        resource.usageThisMonth += usageHours
        resource.lastUsedAt = sessionResource.bookingStart
        resource.updatedAt = LocalDateTime.now()
        
        resourceRepository.save(resource)
    }

    private fun identifyPeakUsagePeriods(bookings: List<SessionResource>): List<String> {
        // Analyze booking patterns to identify peak periods
        val hourlyUsage = bookings.groupBy { booking ->
            booking.bookingStart?.hour ?: 0
        }.mapValues { it.value.size }
        
        return hourlyUsage.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { "${it.key}:00 - ${it.key + 1}:00 (${it.value} bookings)" }
    }

    private fun calculateMonthlyUsage(bookings: List<SessionResource>): Map<String, Int> {
        return bookings.groupBy { booking ->
            "${booking.bookingStart?.year}-${booking.bookingStart?.monthValue?.toString()?.padStart(2, '0')}"
        }.mapValues { entry ->
            entry.value.sumOf { 
                ChronoUnit.HOURS.between(it.bookingStart, it.bookingEnd).toInt()
            }
        }
    }

    private fun calculateCostEfficiency(@Suppress("UNUSED_PARAMETER") resource: Resource, bookings: List<SessionResource>): BigDecimal? {
        val totalCost = bookings.mapNotNull { it.actualCost ?: it.estimatedCost }.fold(BigDecimal.ZERO) { acc, cost -> acc.add(cost) }
        val totalHours = bookings.sumOf { ChronoUnit.HOURS.between(it.bookingStart, it.bookingEnd).toInt() }
        
        return if (totalHours > 0) totalCost.divide(BigDecimal(totalHours)) else null
    }

    private fun findNextAvailableSlot(@Suppress("UNUSED_PARAMETER") resource: Resource, conflicts: List<SessionResource>): LocalDateTime? {
        // Find the earliest time when resource becomes available
        val now = LocalDateTime.now()
        val sortedConflicts = conflicts.sortedBy { it.bookingEnd }
        
        return sortedConflicts.lastOrNull()?.bookingEnd?.plusMinutes(15) ?: now
    }

    @Suppress("UNUSED_PARAMETER")
    private fun calculateTotalResourceCosts(startDate: LocalDateTime, endDate: LocalDateTime): BigDecimal {
        // This would need to be implemented with proper event-based queries
        return BigDecimal.ZERO
    }

    @Suppress("UNUSED_PARAMETER")
    private fun calculateCostByResourceType(startDate: LocalDateTime, endDate: LocalDateTime): Map<ResourceType, BigDecimal> {
        // This would be implemented with proper queries
        return emptyMap()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun generateBookingTrends(startDate: LocalDateTime, endDate: LocalDateTime): List<BookingTrendDto> {
        // This would generate day-by-day booking trends
        return emptyList()
    }

    private fun convertToDto(resource: Resource): DtoResourceDto {
        val now = LocalDateTime.now()
        
        return DtoResourceDto(
            id = resource.id,
            name = resource.name,
            description = resource.description,
            type = resource.type,
            status = resource.status,
            capacity = resource.capacity,
            location = resource.location,
            floor = resource.floor,
            building = resource.building,
            specifications = resource.specifications,
            serialNumber = resource.serialNumber,
            model = resource.model,
            manufacturer = resource.manufacturer,
            isBookable = resource.isBookable,
            requiresApproval = resource.requiresApproval,
            bookingLeadTimeHours = resource.bookingLeadTimeHours,
            maxBookingDurationHours = resource.maxBookingDurationHours,
            hourlyRate = resource.hourlyRate,
            dailyRate = resource.dailyRate,
            setupCost = resource.setupCost,
            cleanupCost = resource.cleanupCost,
            lastMaintenanceDate = resource.lastMaintenanceDate,
            nextMaintenanceDate = resource.nextMaintenanceDate,
            maintenanceNotes = resource.maintenanceNotes,
            daysSinceLastMaintenance = resource.lastMaintenanceDate?.let { 
                ChronoUnit.DAYS.between(it, now) 
            },
            daysUntilMaintenance = resource.nextMaintenanceDate?.let { 
                ChronoUnit.DAYS.between(now, it) 
            },
            contactPerson = resource.contactPerson,
            contactEmail = resource.contactEmail,
            contactPhone = resource.contactPhone,
            departmentOwner = resource.departmentOwner,
            totalUsageHours = resource.totalUsageHours,
            usageThisMonth = resource.usageThisMonth,
            lastUsedAt = resource.lastUsedAt,
            tags = resource.tags?.split(",")?.map { it.trim() } ?: emptyList(),
            category = resource.category,
            isAvailable = resource.status == ResourceStatus.AVAILABLE && resource.isActive
        )
    }

    private fun convertSessionResourceToDto(sessionResource: SessionResource): SessionResourceDto {
        return SessionResourceDto(
            id = sessionResource.id,
            sessionId = sessionResource.session?.id,
            sessionTitle = sessionResource.session?.title,
            resourceId = sessionResource.resource!!.id!!,
            resourceName = sessionResource.resource?.name,
            resourceType = sessionResource.resource?.type,
            quantityNeeded = sessionResource.quantityNeeded,
            quantityAllocated = sessionResource.quantityAllocated,
            setupTimeMinutes = sessionResource.setupTimeMinutes,
            usageTimeMinutes = sessionResource.usageTimeMinutes,
            cleanupTimeMinutes = sessionResource.cleanupTimeMinutes,
            bookingStart = sessionResource.bookingStart,
            bookingEnd = sessionResource.bookingEnd,
            isRequired = sessionResource.isRequired,
            notes = sessionResource.notes,
            estimatedCost = sessionResource.estimatedCost,
            actualCost = sessionResource.actualCost,
            status = sessionResource.status,
            approvedBy = sessionResource.approvedBy,
            approvedAt = sessionResource.approvedAt
        )
    }
}