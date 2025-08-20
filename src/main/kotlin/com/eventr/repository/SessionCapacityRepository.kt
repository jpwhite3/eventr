package com.eventr.repository

import com.eventr.model.CapacityType
import com.eventr.model.SessionCapacity
import com.eventr.model.WaitlistStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface SessionCapacityRepository : JpaRepository<SessionCapacity, UUID> {
    
    fun findBySessionId(sessionId: UUID): SessionCapacity?
    
    fun findBySessionIdIn(sessionIds: List<UUID>): List<SessionCapacity>
    
    // Capacity analysis queries
    @Query("SELECT sc FROM SessionCapacity sc WHERE sc.availableSpots <= :threshold AND sc.session.isActive = true")
    fun findLowCapacitySessions(@Param("threshold") threshold: Int): List<SessionCapacity>
    
    @Query("SELECT sc FROM SessionCapacity sc WHERE (CAST(sc.currentRegistrations AS double) / sc.maximumCapacity) >= :percentage AND sc.session.isActive = true")
    fun findHighDemandSessions(@Param("percentage") percentage: Double): List<SessionCapacity>
    
    @Query("SELECT sc FROM SessionCapacity sc WHERE sc.currentWaitlistCount > 0 AND sc.session.isActive = true ORDER BY sc.currentWaitlistCount DESC")
    fun findSessionsWithWaitlists(): List<SessionCapacity>
    
    @Query("SELECT sc FROM SessionCapacity sc WHERE sc.capacityType = :type")
    fun findByCapacityType(@Param("type") type: CapacityType): List<SessionCapacity>
    
    @Query("SELECT sc FROM SessionCapacity sc WHERE sc.waitlistStrategy = :strategy AND sc.currentWaitlistCount > 0")
    fun findByWaitlistStrategy(@Param("strategy") strategy: WaitlistStrategy): List<SessionCapacity>
    
    // Overbooking analysis
    @Query("SELECT sc FROM SessionCapacity sc WHERE sc.allowOverbooking = true AND sc.currentRegistrations > sc.maximumCapacity")
    fun findOverBookedSessions(): List<SessionCapacity>
    
    // Statistics queries
    @Query("SELECT AVG(CAST(sc.currentRegistrations AS double) / sc.maximumCapacity) FROM SessionCapacity sc WHERE sc.session.event.id = :eventId")
    fun getAverageCapacityUtilization(@Param("eventId") eventId: UUID): Double?
    
    @Query("SELECT COUNT(sc) FROM SessionCapacity sc WHERE sc.session.event.id = :eventId AND sc.currentRegistrations >= sc.maximumCapacity")
    fun countFullSessions(@Param("eventId") eventId: UUID): Long
    
    @Query("SELECT SUM(sc.currentWaitlistCount) FROM SessionCapacity sc WHERE sc.session.event.id = :eventId")
    fun getTotalWaitlistCount(@Param("eventId") eventId: UUID): Long?
    
    // Resource optimization queries
    @Query("""
        SELECT sc FROM SessionCapacity sc 
        WHERE sc.session.event.id = :eventId 
        AND sc.currentRegistrations < sc.minimumCapacity 
        AND sc.session.isActive = true
    """)
    fun findUnderCapacitySessions(@Param("eventId") eventId: UUID): List<SessionCapacity>
    
    // Dynamic capacity management
    @Query("""
        SELECT sc FROM SessionCapacity sc 
        WHERE sc.capacityType = 'DYNAMIC' 
        AND sc.autoPromoteFromWaitlist = true 
        AND sc.currentWaitlistCount > 0
        AND sc.availableSpots > 0
    """)
    fun findSessionsForAutoPromotion(): List<SessionCapacity>
}