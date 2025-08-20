package com.eventr.repository

import com.eventr.model.PrerequisiteType
import com.eventr.model.SessionDependency
import com.eventr.model.SessionPrerequisite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface SessionPrerequisiteRepository : JpaRepository<SessionPrerequisite, UUID> {
    
    fun findBySessionIdAndIsActiveTrue(sessionId: UUID): List<SessionPrerequisite>
    
    fun findByPrerequisiteSessionId(prerequisiteSessionId: UUID): List<SessionPrerequisite>
    
    fun findByTypeAndIsActiveTrue(type: PrerequisiteType): List<SessionPrerequisite>
    
    fun findByGroupIdAndIsActiveTrue(groupId: String): List<SessionPrerequisite>
    
    @Query("SELECT sp FROM SessionPrerequisite sp WHERE sp.session.event.id = :eventId AND sp.isActive = true")
    fun findByEventId(@Param("eventId") eventId: UUID): List<SessionPrerequisite>
    
    @Query("""
        SELECT sp FROM SessionPrerequisite sp 
        WHERE sp.session.id = :sessionId 
        AND sp.isRequired = true 
        AND sp.isActive = true
        ORDER BY sp.priority ASC
    """)
    fun findRequiredPrerequisites(@Param("sessionId") sessionId: UUID): List<SessionPrerequisite>
    
    @Query("""
        SELECT COUNT(sp) FROM SessionPrerequisite sp 
        WHERE sp.session.id = :sessionId 
        AND sp.type = 'SESSION_ATTENDANCE'
        AND sp.isActive = true
    """)
    fun countSessionAttendancePrerequisites(@Param("sessionId") sessionId: UUID): Long
    
    // Complex dependency analysis
    @Query("""
        SELECT sp.session.id FROM SessionPrerequisite sp 
        WHERE sp.prerequisiteSession.id = :sessionId 
        AND sp.type = 'SESSION_ATTENDANCE'
        AND sp.isActive = true
    """)
    fun findDependentSessionIds(@Param("sessionId") sessionId: UUID): List<UUID>
}

interface SessionDependencyRepository : JpaRepository<SessionDependency, UUID> {
    
    fun findByParentSessionId(parentSessionId: UUID): List<SessionDependency>
    
    fun findByDependentSessionId(dependentSessionId: UUID): List<SessionDependency>
    
    @Query("SELECT sd FROM SessionDependency sd WHERE sd.parentSession.id = :sessionId OR sd.dependentSession.id = :sessionId")
    fun findBySessionId(@Param("sessionId") sessionId: UUID): List<SessionDependency>
    
    @Query("SELECT sd FROM SessionDependency sd WHERE sd.parentSession.event.id = :eventId OR sd.dependentSession.event.id = :eventId")
    fun findByEventId(@Param("eventId") eventId: UUID): List<SessionDependency>
    
    @Query("SELECT sd FROM SessionDependency sd WHERE sd.isStrict = true")
    fun findStrictDependencies(): List<SessionDependency>
    
    // Circular dependency detection
    @Query(value = """
        WITH RECURSIVE dependency_chain AS (
            SELECT parent_session_id, dependent_session_id, 1 as depth
            FROM session_dependency 
            WHERE parent_session_id = :sessionId
            
            UNION ALL
            
            SELECT sd.parent_session_id, sd.dependent_session_id, dc.depth + 1
            FROM session_dependency sd
            INNER JOIN dependency_chain dc ON sd.parent_session_id = dc.dependent_session_id
            WHERE dc.depth < 10
        )
        SELECT * FROM dependency_chain WHERE dependent_session_id = :sessionId
    """, nativeQuery = true)
    fun detectCircularDependencies(@Param("sessionId") sessionId: UUID): List<Array<Any>>
    
    // Path analysis between sessions
    @Query(value = """
        WITH RECURSIVE session_path AS (
            SELECT parent_session_id, dependent_session_id, 1 as path_length,
                   CAST(parent_session_id as VARCHAR(1000)) as path
            FROM session_dependency 
            WHERE parent_session_id = :fromSessionId
            
            UNION ALL
            
            SELECT sd.parent_session_id, sd.dependent_session_id, sp.path_length + 1,
                   sp.path || '->' || sd.dependent_session_id
            FROM session_dependency sd
            INNER JOIN session_path sp ON sd.parent_session_id = sp.dependent_session_id
            WHERE sp.path_length < 10
              AND sp.path NOT LIKE '%' || sd.dependent_session_id || '%'
        )
        SELECT * FROM session_path WHERE dependent_session_id = :toSessionId
    """, nativeQuery = true)
    fun findDependencyPath(@Param("fromSessionId") fromSessionId: UUID, 
                          @Param("toSessionId") toSessionId: UUID): List<Array<Any>>
}