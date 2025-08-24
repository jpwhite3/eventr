package com.eventr.repository

import com.eventr.model.CheckIn
import com.eventr.model.CheckInMethod
import com.eventr.model.CheckInType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface CheckInRepository : JpaRepository<CheckIn, UUID> {
    
    fun findByRegistrationIdOrderByCheckedInAtDesc(registrationId: UUID): List<CheckIn>
    
    fun findBySessionIdOrderByCheckedInAtDesc(sessionId: UUID): List<CheckIn>
    
    @Query("SELECT c FROM CheckIn c WHERE c.registration.eventInstance.event.id = :eventId ORDER BY c.checkedInAt DESC")
    fun findByEventIdOrderByCheckedInAtDesc(@Param("eventId") eventId: UUID): List<CheckIn>
    
    fun findByRegistrationIdAndSessionId(registrationId: UUID, sessionId: UUID?): CheckIn?
    
    fun findByRegistrationIdAndType(registrationId: UUID, type: CheckInType): CheckIn?
    
    @Query("SELECT COUNT(c) FROM CheckIn c WHERE c.registration.eventInstance.event.id = :eventId")
    fun countByEventId(@Param("eventId") eventId: UUID): Long
    
    @Query("SELECT COUNT(c) FROM CheckIn c WHERE c.session.id = :sessionId")
    fun countBySessionId(@Param("sessionId") sessionId: UUID): Long
    
    @Query("SELECT c FROM CheckIn c WHERE c.checkedInAt >= :startTime AND c.checkedInAt <= :endTime ORDER BY c.checkedInAt DESC")
    fun findByCheckedInAtBetween(@Param("startTime") startTime: LocalDateTime, 
                                @Param("endTime") endTime: LocalDateTime): List<CheckIn>
    
    @Query("SELECT c FROM CheckIn c WHERE c.registration.eventInstance.event.id = :eventId AND c.checkedInAt >= :startTime ORDER BY c.checkedInAt DESC")
    fun findRecentEventCheckIns(@Param("eventId") eventId: UUID, 
                               @Param("startTime") startTime: LocalDateTime): List<CheckIn>
    
    fun findByCheckedInBy(checkedInBy: String): List<CheckIn>
    
    fun findByMethod(method: CheckInMethod): List<CheckIn>
    
    @Query("SELECT c FROM CheckIn c WHERE c.registration.eventInstance.event.id = :eventId AND c.method = :method")
    fun findByEventIdAndMethod(@Param("eventId") eventId: UUID, @Param("method") method: CheckInMethod): List<CheckIn>
    
    // Statistics queries
    @Query("SELECT COUNT(DISTINCT c.registration.id) FROM CheckIn c WHERE c.registration.eventInstance.event.id = :eventId")
    fun countUniqueAttendeesByEventId(@Param("eventId") eventId: UUID): Long
    
    @Query("SELECT COUNT(DISTINCT c.registration.id) FROM CheckIn c WHERE c.session.id = :sessionId")
    fun countUniqueAttendeesBySessionId(@Param("sessionId") sessionId: UUID): Long
    
    @Query("SELECT c.method, COUNT(c) FROM CheckIn c WHERE c.registration.eventInstance.event.id = :eventId GROUP BY c.method")
    fun getCheckInMethodStats(@Param("eventId") eventId: UUID): List<Array<Any>>
    
    @Query("SELECT EXTRACT(HOUR FROM c.checkedInAt), COUNT(c) FROM CheckIn c WHERE c.registration.eventInstance.event.id = :eventId " +
           "AND CAST(c.checkedInAt AS DATE) = CURRENT_DATE GROUP BY EXTRACT(HOUR FROM c.checkedInAt)")
    fun getTodayHourlyCheckInStats(@Param("eventId") eventId: UUID): List<Array<Any>>
    
    // Sync-related queries for offline support
    fun findByIsSyncedFalse(): List<CheckIn>
    
    fun findByDeviceIdAndIsSyncedFalse(deviceId: String): List<CheckIn>
    
    @Query("SELECT c FROM CheckIn c WHERE c.qrCodeUsed = :qrCode")
    fun findByQrCodeUsed(@Param("qrCode") qrCode: String): List<CheckIn>
    
    // Duplicate detection
    @Query("SELECT c FROM CheckIn c WHERE c.registration.id = :registrationId AND " +
           "c.session.id = :sessionId AND c.checkedInAt >= :startTime")
    fun findPotentialDuplicates(@Param("registrationId") registrationId: UUID,
                               @Param("sessionId") sessionId: UUID?,
                               @Param("startTime") startTime: LocalDateTime): List<CheckIn>
    
    // Attendance verification
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CheckIn c " +
           "WHERE c.registration.id = :registrationId AND c.session.id = :sessionId")
    fun isCheckedInToSession(@Param("registrationId") registrationId: UUID, 
                           @Param("sessionId") sessionId: UUID): Boolean
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CheckIn c " +
           "WHERE c.registration.id = :registrationId AND c.type = 'EVENT'")
    fun isCheckedInToEvent(@Param("registrationId") registrationId: UUID): Boolean
}