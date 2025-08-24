package com.eventr.repository

import com.eventr.model.ConflictResolution
import com.eventr.model.ConflictResolutionStatus
import com.eventr.model.ConflictSeverity
import com.eventr.model.ConflictType
import com.eventr.model.ScheduleConflict
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface ScheduleConflictRepository : JpaRepository<ScheduleConflict, UUID> {
    
    fun findByIsActiveTrueAndResolutionStatus(status: ConflictResolutionStatus): List<ScheduleConflict>
    
    fun findByTypeAndIsActiveTrue(type: ConflictType): List<ScheduleConflict>
    
    fun findBySeverityAndIsActiveTrueOrderByDetectedAtDesc(severity: ConflictSeverity): List<ScheduleConflict>
    
    @Query("SELECT sc FROM ScheduleConflict sc WHERE sc.primarySession.id = :sessionId OR sc.secondarySession.id = :sessionId")
    fun findBySessionId(@Param("sessionId") sessionId: UUID): List<ScheduleConflict>
    
    @Query("SELECT sc FROM ScheduleConflict sc WHERE sc.resource.id = :resourceId AND sc.isActive = true")
    fun findByResourceId(@Param("resourceId") resourceId: UUID): List<ScheduleConflict>
    
    @Query("SELECT sc FROM ScheduleConflict sc WHERE sc.registration.id = :registrationId AND sc.isActive = true")
    fun findByRegistrationId(@Param("registrationId") registrationId: UUID): List<ScheduleConflict>
    
    // Critical conflicts requiring immediate attention
    @Query("""
        SELECT sc FROM ScheduleConflict sc 
        WHERE sc.isActive = true 
        AND sc.resolutionStatus = 'UNRESOLVED'
        AND sc.severity IN ('ERROR', 'CRITICAL')
        ORDER BY sc.severity DESC, sc.detectedAt ASC
    """)
    fun findCriticalUnresolvedConflicts(): List<ScheduleConflict>
    
    // Auto-resolvable conflicts
    @Query("""
        SELECT sc FROM ScheduleConflict sc 
        WHERE sc.isActive = true 
        AND sc.canAutoResolve = true 
        AND sc.resolutionStatus = 'UNRESOLVED'
    """)
    fun findAutoResolvableConflicts(): List<ScheduleConflict>
    
    // Time-based conflict detection
    @Query("""
        SELECT sc FROM ScheduleConflict sc 
        WHERE sc.isActive = true
        AND sc.conflictStart <= :endTime 
        AND sc.conflictEnd >= :startTime
        ORDER BY sc.conflictStart ASC
    """)
    fun findConflictsDuringPeriod(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): List<ScheduleConflict>
    
    // Event-level conflict analysis
    @Query("""
        SELECT sc FROM ScheduleConflict sc 
        WHERE (sc.primarySession.event.id = :eventId OR sc.secondarySession.event.id = :eventId)
        AND sc.isActive = true
        ORDER BY sc.severity DESC, sc.detectedAt DESC
    """)
    fun findEventConflicts(@Param("eventId") eventId: UUID): List<ScheduleConflict>
    
    // Statistics and reporting
    @Query("""
        SELECT sc.type, sc.severity, COUNT(sc) 
        FROM ScheduleConflict sc 
        WHERE sc.isActive = true 
        AND sc.detectedAt >= :startDate
        GROUP BY sc.type, sc.severity
        ORDER BY COUNT(sc) DESC
    """)
    fun getConflictStatistics(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
    
    @Query("""
        SELECT COUNT(sc) FROM ScheduleConflict sc 
        WHERE sc.isActive = true 
        AND sc.resolutionStatus = 'UNRESOLVED'
        AND sc.detectedAt >= :startDate
    """)
    fun countUnresolvedConflictsSince(@Param("startDate") startDate: LocalDateTime): Long
    
    // Resolution time analysis
    @Query("""
        SELECT AVG(CAST((EXTRACT(EPOCH FROM sc.resolvedAt) - EXTRACT(EPOCH FROM sc.detectedAt)) / 3600 AS DOUBLE)) 
        FROM ScheduleConflict sc 
        WHERE sc.resolvedAt IS NOT NULL 
        AND sc.detectedAt >= :startDate
    """)
    fun getAverageResolutionTimeHours(@Param("startDate") startDate: LocalDateTime): Double?
    
    // Notification management
    @Query("SELECT sc FROM ScheduleConflict sc WHERE sc.notificationsSent = false AND sc.severity IN ('ERROR', 'CRITICAL')")
    fun findConflictsNeedingNotification(): List<ScheduleConflict>
    
    // Stale conflicts (detected long ago but not updated)
    @Query("""
        SELECT sc FROM ScheduleConflict sc 
        WHERE sc.isActive = true 
        AND sc.resolutionStatus = 'UNRESOLVED'
        AND sc.lastCheckedAt < :cutoffDate
        ORDER BY sc.lastCheckedAt ASC
    """)
    fun findStaleConflicts(@Param("cutoffDate") cutoffDate: LocalDateTime): List<ScheduleConflict>
}

interface ConflictResolutionRepository : JpaRepository<ConflictResolution, UUID> {
    
    fun findByConflictId(conflictId: UUID): List<ConflictResolution>
    
    fun findByImplementedBy(implementedBy: String): List<ConflictResolution>
    
    @Query("SELECT cr FROM ConflictResolution cr WHERE cr.implementedAt >= :startDate ORDER BY cr.implementedAt DESC")
    fun findRecentResolutions(@Param("startDate") startDate: LocalDateTime): List<ConflictResolution>
    
    // Resolution effectiveness analysis
    @Query("""
        SELECT cr.resolutionType, COUNT(cr), 
               AVG(cr.affectedSessions), AVG(cr.affectedRegistrations)
        FROM ConflictResolution cr 
        WHERE cr.implementedAt >= :startDate
        GROUP BY cr.resolutionType
        ORDER BY COUNT(cr) DESC
    """)
    fun getResolutionTypeStatistics(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
    
    @Query("""
        SELECT cr FROM ConflictResolution cr 
        WHERE cr.conflict.id IN (
            SELECT sc.id FROM ScheduleConflict sc 
            WHERE sc.primarySession.event.id = :eventId 
            OR sc.secondarySession.event.id = :eventId
        )
        ORDER BY cr.implementedAt DESC
    """)
    fun findEventResolutions(@Param("eventId") eventId: UUID): List<ConflictResolution>
}