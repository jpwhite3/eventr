package com.eventr.repository

import com.eventr.model.SessionRegistration
import com.eventr.model.SessionRegistrationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface SessionRegistrationRepository : JpaRepository<SessionRegistration, UUID> {
    
    fun findBySessionIdAndRegistrationId(sessionId: UUID, registrationId: UUID): SessionRegistration?
    
    fun findBySessionIdOrderByRegisteredAtAsc(sessionId: UUID): List<SessionRegistration>
    
    fun findBySessionIdAndStatus(sessionId: UUID, status: SessionRegistrationStatus): List<SessionRegistration>
    
    fun findByRegistrationIdOrderBySessionStartTime(registrationId: UUID): List<SessionRegistration>
    
    @Query("SELECT sr FROM SessionRegistration sr WHERE sr.registration.id = :registrationId AND sr.status = :status")
    fun findByRegistrationIdAndStatus(@Param("registrationId") registrationId: UUID, 
                                     @Param("status") status: SessionRegistrationStatus): List<SessionRegistration>
    
    @Query("SELECT sr FROM SessionRegistration sr WHERE sr.session.id = :sessionId AND sr.status = 'WAITLIST' ORDER BY sr.waitlistRegisteredAt ASC")
    fun findWaitlistBySessionIdOrderByPosition(@Param("sessionId") sessionId: UUID): List<SessionRegistration>
    
    @Query("SELECT COUNT(sr) FROM SessionRegistration sr WHERE sr.session.id = :sessionId AND sr.status = 'REGISTERED'")
    fun countConfirmedBySessionId(@Param("sessionId") sessionId: UUID): Long
    
    @Query("SELECT COUNT(sr) FROM SessionRegistration sr WHERE sr.session.id = :sessionId AND sr.status = 'WAITLIST'")
    fun countWaitlistBySessionId(@Param("sessionId") sessionId: UUID): Long
    
    @Query("SELECT sr FROM SessionRegistration sr WHERE sr.session.id = :sessionId AND sr.status = 'ATTENDED'")
    fun findAttendedBySessionId(@Param("sessionId") sessionId: UUID): List<SessionRegistration>
    
    @Query("SELECT sr FROM SessionRegistration sr JOIN sr.session s WHERE sr.registration.userEmail = :email AND " +
           "s.startTime >= CURRENT_TIMESTAMP ORDER BY s.startTime")
    fun findUpcomingSessionsByEmail(@Param("email") email: String): List<SessionRegistration>
    
    // Check for time conflicts for a user
    @Query("SELECT sr FROM SessionRegistration sr JOIN sr.session s WHERE sr.registration.id = :registrationId AND " +
           "sr.status = 'REGISTERED' AND " +
           "((s.startTime <= :endTime AND s.endTime >= :startTime))")
    fun findConflictingUserSessions(@Param("registrationId") registrationId: UUID,
                                   @Param("startTime") startTime: java.time.LocalDateTime,
                                   @Param("endTime") endTime: java.time.LocalDateTime): List<SessionRegistration>
    
    // Additional methods needed by services
    fun findByRegistrationIdAndSessionId(registrationId: UUID, sessionId: UUID): SessionRegistration?
    
    fun countBySessionIdAndStatus(sessionId: UUID, status: SessionRegistrationStatus): Long
    
    fun findBySessionIdAndStatusOrderByWaitlistRegisteredAtAsc(sessionId: UUID, status: SessionRegistrationStatus): List<SessionRegistration>
}