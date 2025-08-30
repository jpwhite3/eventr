package com.eventr.repository

import com.eventr.model.Registration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.List
import java.util.UUID

interface RegistrationRepository : JpaRepository<Registration, UUID> {
    fun findByUserEmail(userEmail: String): List<Registration>
    
    fun findByUserId(userId: UUID): List<Registration>
    
    @Query("SELECT r FROM Registration r JOIN r.eventInstance ei JOIN ei.event e WHERE e.id = :eventId AND (:name IS NULL OR LOWER(r.userName) LIKE LOWER(CONCAT('%', :name, '%')))")
    fun findByEventIdAndUserName(
        @Param("eventId") eventId: UUID, 
        @Param("name") name: String?
    ): List<Registration>
    
    @Query("SELECT r FROM Registration r JOIN r.eventInstance ei JOIN ei.event e WHERE e.id = :eventId")
    fun findByEventId(@Param("eventId") eventId: UUID): List<Registration>
    
    @Query("SELECT r FROM Registration r WHERE r.userEmail = :userEmail AND r.eventInstance.event.id = :eventId")
    fun findByUserEmailAndEventId(@Param("userEmail") userEmail: String, @Param("eventId") eventId: UUID): Registration?
    
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.eventInstance.event.id = :eventId")
    fun countByEventId(@Param("eventId") eventId: UUID): Long
}
