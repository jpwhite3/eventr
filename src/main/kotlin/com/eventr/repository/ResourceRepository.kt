package com.eventr.repository

import com.eventr.model.Resource
import com.eventr.model.ResourceStatus
import com.eventr.model.ResourceType
import com.eventr.model.SessionResource
import com.eventr.model.ResourceBookingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

interface ResourceRepository : JpaRepository<Resource, UUID> {
    
    fun findByTypeAndIsActiveTrue(type: ResourceType): List<Resource>
    
    fun findByStatusAndIsActiveTrue(status: ResourceStatus): List<Resource>
    
    fun findByIsBookableTrueAndIsActiveTrueAndStatus(status: ResourceStatus): List<Resource>
    
    @Query("SELECT r FROM Resource r WHERE r.location = :location AND r.isActive = true")
    fun findByLocation(@Param("location") location: String): List<Resource>
    
    @Query("SELECT r FROM Resource r WHERE r.capacity >= :minCapacity AND r.type = :type AND r.isActive = true")
    fun findByCapacityAndType(@Param("minCapacity") minCapacity: Int, @Param("type") type: ResourceType): List<Resource>
    
    // Availability queries
    @Query("""
        SELECT r FROM Resource r 
        WHERE r.type = :type 
        AND r.isBookable = true 
        AND r.isActive = true
        AND r.status = com.eventr.model.ResourceStatus.AVAILABLE
        AND r.id NOT IN (
            SELECT sr.resource.id FROM SessionResource sr 
            WHERE sr.bookingStart <= :endTime 
            AND sr.bookingEnd >= :startTime
            AND sr.status NOT IN (com.eventr.model.ResourceBookingStatus.CANCELLED, com.eventr.model.ResourceBookingStatus.COMPLETED)
        )
    """)
    fun findAvailableResourcesByType(
        @Param("type") type: ResourceType,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): List<Resource>
    
    @Query("""
        SELECT r FROM Resource r 
        WHERE r.isBookable = true 
        AND r.isActive = true
        AND r.status = com.eventr.model.ResourceStatus.AVAILABLE
        AND (:location IS NULL OR r.location = :location)
        AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
        AND r.id NOT IN (
            SELECT sr.resource.id FROM SessionResource sr 
            WHERE sr.bookingStart <= :endTime 
            AND sr.bookingEnd >= :startTime
            AND sr.status NOT IN (com.eventr.model.ResourceBookingStatus.CANCELLED, com.eventr.model.ResourceBookingStatus.COMPLETED)
        )
    """)
    fun findAvailableResources(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
        @Param("location") location: String?,
        @Param("minCapacity") minCapacity: Int?
    ): List<Resource>
    
    // Maintenance and scheduling
    @Query("SELECT r FROM Resource r WHERE r.nextMaintenanceDate <= :date AND r.isActive = true")
    fun findResourcesDueForMaintenance(@Param("date") date: LocalDateTime): List<Resource>
    
    // Usage analytics
    @Query("SELECT r FROM Resource r WHERE r.lastUsedAt < :date AND r.isActive = true ORDER BY r.lastUsedAt ASC")
    fun findUnderutilizedResources(@Param("date") date: LocalDateTime): List<Resource>
    
    @Query("""
        SELECT r.type, AVG(r.usageThisMonth), COUNT(r) 
        FROM Resource r 
        WHERE r.isActive = true 
        GROUP BY r.type
    """)
    fun getResourceUtilizationByType(): List<Array<Any>>
    
    // Search functionality
    @Query("""
        SELECT r FROM Resource r 
        WHERE r.isActive = true
        AND (
            LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(r.location) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
    """)
    fun searchResources(@Param("searchTerm") searchTerm: String): List<Resource>
    
    fun findByIsActiveTrue(): List<Resource>
}

interface SessionResourceRepository : JpaRepository<SessionResource, UUID> {
    
    fun findBySessionId(sessionId: UUID): List<SessionResource>
    
    fun findByResourceId(resourceId: UUID): List<SessionResource>
    
    fun findBySessionIdAndIsRequiredTrue(sessionId: UUID): List<SessionResource>
    
    @Query("SELECT sr FROM SessionResource sr WHERE sr.status = :status")
    fun findByStatus(@Param("status") status: ResourceBookingStatus): List<SessionResource>
    
    // Conflict detection
    @Query("""
        SELECT sr FROM SessionResource sr 
        WHERE sr.resource.id = :resourceId 
        AND sr.bookingStart < :endTime 
        AND sr.bookingEnd > :startTime
        AND sr.status NOT IN (com.eventr.model.ResourceBookingStatus.CANCELLED, com.eventr.model.ResourceBookingStatus.COMPLETED)
    """)
    fun findConflictingBookings(
        @Param("resourceId") resourceId: UUID,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): List<SessionResource>
    
    @Query("""
        SELECT sr FROM SessionResource sr 
        WHERE sr.resource.id = :resourceId 
        AND sr.quantityAllocated > 0
        AND sr.bookingStart < :endTime 
        AND sr.bookingEnd > :startTime
        AND sr.status NOT IN (com.eventr.model.ResourceBookingStatus.CANCELLED, com.eventr.model.ResourceBookingStatus.COMPLETED)
    """)
    fun findResourceUsageDuringPeriod(
        @Param("resourceId") resourceId: UUID,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): List<SessionResource>
    
    // Resource allocation optimization
    @Query("SELECT sr FROM SessionResource sr WHERE sr.quantityNeeded > sr.quantityAllocated AND sr.isRequired = true")
    fun findUnfulfilledRequiredBookings(): List<SessionResource>
    
    @Query("""
        SELECT r.id, COUNT(sr), SUM(sr.quantityAllocated) 
        FROM Resource r
        LEFT JOIN SessionResource sr ON r.id = sr.resource.id
        WHERE r.isActive = true
        GROUP BY r.id
        ORDER BY COUNT(sr) DESC
    """)
    fun getResourceDemandStatistics(): List<Array<Any>>
    
    // Cost analysis
    @Query("SELECT SUM(sr.estimatedCost) FROM SessionResource sr WHERE sr.session.id = :sessionId")
    fun getTotalEstimatedCostForSession(@Param("sessionId") sessionId: UUID): BigDecimal?
    
    @Query("SELECT SUM(sr.actualCost) FROM SessionResource sr WHERE sr.session.event.id = :eventId AND sr.actualCost IS NOT NULL")
    fun getTotalActualCostForEvent(@Param("eventId") eventId: UUID): BigDecimal?
}