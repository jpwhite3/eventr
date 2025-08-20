package com.eventr.repository

import com.eventr.model.Session
import com.eventr.model.SessionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface SessionRepository : JpaRepository<Session, UUID> {
    
    fun findByEventIdOrderByStartTime(eventId: UUID): List<Session>
    
    fun findByEventIdAndIsActiveTrue(eventId: UUID): List<Session>
    
    fun findByEventIdAndType(eventId: UUID, type: SessionType): List<Session>
    
    @Query("SELECT s FROM Session s WHERE s.event.id = :eventId AND s.startTime >= :startTime AND s.endTime <= :endTime ORDER BY s.startTime")
    fun findByEventIdAndTimeRange(@Param("eventId") eventId: UUID, 
                                 @Param("startTime") startTime: LocalDateTime, 
                                 @Param("endTime") endTime: LocalDateTime): List<Session>
    
    @Query("SELECT s FROM Session s WHERE s.event.id = :eventId AND s.room = :room AND " +
           "((s.startTime <= :endTime AND s.endTime >= :startTime))")
    fun findConflictingSessions(@Param("eventId") eventId: UUID,
                               @Param("room") room: String,
                               @Param("startTime") startTime: LocalDateTime,
                               @Param("endTime") endTime: LocalDateTime): List<Session>
    
    @Query("SELECT s FROM Session s WHERE s.presenter = :presenter AND " +
           "((s.startTime <= :endTime AND s.endTime >= :startTime))")
    fun findSessionsByPresenterAndTimeConflict(@Param("presenter") presenter: String,
                                              @Param("startTime") startTime: LocalDateTime,
                                              @Param("endTime") endTime: LocalDateTime): List<Session>
    
    @Query("SELECT COUNT(sr) FROM SessionRegistration sr WHERE sr.session.id = :sessionId AND sr.status IN ('REGISTERED', 'WAITLIST')")
    fun countRegistrationsBySessionId(@Param("sessionId") sessionId: UUID): Long
    
    @Query("SELECT COUNT(sr) FROM SessionRegistration sr WHERE sr.session.id = :sessionId AND sr.status = 'REGISTERED'")
    fun countConfirmedRegistrationsBySessionId(@Param("sessionId") sessionId: UUID): Long
    
    @Query("SELECT s FROM Session s WHERE :tag MEMBER OF s.tags")
    fun findByTag(@Param("tag") tag: String): List<Session>
}