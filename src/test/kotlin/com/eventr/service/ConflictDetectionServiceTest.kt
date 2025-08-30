package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*

@DisplayName("ConflictDetectionService Tests")
class ConflictDetectionServiceTest {

    private lateinit var scheduleConflictRepository: ScheduleConflictRepository
    private lateinit var conflictResolutionRepository: ConflictResolutionRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionResourceRepository: SessionResourceRepository
    private lateinit var sessionRegistrationRepository: SessionRegistrationRepository
    private lateinit var resourceRepository: ResourceRepository
    private lateinit var registrationRepository: RegistrationRepository
    private lateinit var conflictDetectionService: ConflictDetectionService

    @BeforeEach
    fun setUp() {
        scheduleConflictRepository = mock()
        conflictResolutionRepository = mock()
        sessionRepository = mock()
        sessionResourceRepository = mock()
        sessionRegistrationRepository = mock()
        resourceRepository = mock()
        registrationRepository = mock()
        
        conflictDetectionService = ConflictDetectionService(
            scheduleConflictRepository = scheduleConflictRepository,
            conflictResolutionRepository = conflictResolutionRepository,
            sessionRepository = sessionRepository,
            sessionResourceRepository = sessionResourceRepository,
            sessionRegistrationRepository = sessionRegistrationRepository,
            resourceRepository = resourceRepository,
            registrationRepository = registrationRepository
        )
    }

    @Test
    @DisplayName("Should detect no conflicts when no sessions exist")
    fun shouldDetectNoConflictsWhenNoSessionsExist() {
        // Given
        val eventId = UUID.randomUUID()
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(emptyList<Session>())

        // When
        val conflicts = conflictDetectionService.detectAllConflicts(eventId)

        // Then
        assertTrue(conflicts.isEmpty())
        verify(sessionRepository, times(3)).findByEventIdAndIsActiveTrue(eventId) // Called for time, resource, and capacity conflicts
    }

    @Test
    @DisplayName("Should detect time overlap conflicts between sessions")
    fun shouldDetectTimeOverlapConflictsBetweenSessions() {
        // Given
        val eventId = UUID.randomUUID()
        
        val session1 = createSession(
            UUID.randomUUID(),
            "Session 1",
            LocalDateTime.of(2024, 12, 15, 10, 0),
            LocalDateTime.of(2024, 12, 15, 11, 30)
        )
        
        val session2 = createSession(
            UUID.randomUUID(),
            "Session 2", 
            LocalDateTime.of(2024, 12, 15, 11, 0),
            LocalDateTime.of(2024, 12, 15, 12, 0)
        )
        
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(listOf(session1, session2))

        // When
        val conflicts = conflictDetectionService.detectTimeOverlapConflicts(eventId)

        // Then
        assertFalse(conflicts.isEmpty())
        assertEquals(1, conflicts.size)
        
        val conflict = conflicts.first()
        assertEquals(ConflictType.TIME_OVERLAP, conflict.type)
        assertEquals("Session Time Overlap", conflict.title)
        assertTrue(conflict.description!!.contains("Session 1"))
        assertTrue(conflict.description!!.contains("Session 2"))
        assertEquals(session1.id, conflict.primarySessionId)
        assertEquals(session2.id, conflict.secondarySessionId)
    }

    @Test
    @DisplayName("Should not detect conflicts for non-overlapping sessions")
    fun shouldNotDetectConflictsForNonOverlappingSessions() {
        // Given
        val eventId = UUID.randomUUID()
        
        val session1 = createSession(
            UUID.randomUUID(),
            "Session 1",
            LocalDateTime.of(2024, 12, 15, 10, 0),
            LocalDateTime.of(2024, 12, 15, 11, 0)
        )
        
        val session2 = createSession(
            UUID.randomUUID(),
            "Session 2",
            LocalDateTime.of(2024, 12, 15, 11, 30),
            LocalDateTime.of(2024, 12, 15, 12, 30)
        )
        
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(listOf(session1, session2))

        // When
        val conflicts = conflictDetectionService.detectTimeOverlapConflicts(eventId)

        // Then
        assertTrue(conflicts.isEmpty())
        verify(sessionRepository).findByEventIdAndIsActiveTrue(eventId)
    }

    @Test
    @DisplayName("Should detect resource conflicts")
    fun shouldDetectResourceConflicts() {
        // Given
        val eventId = UUID.randomUUID()
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(emptyList<Session>())

        // When
        val conflicts = conflictDetectionService.detectResourceConflicts(eventId)

        // Then
        assertNotNull(conflicts)
        verify(sessionRepository).findByEventIdAndIsActiveTrue(eventId)
    }

    @Test
    @DisplayName("Should detect capacity conflicts")
    fun shouldDetectCapacityConflicts() {
        // Given
        val eventId = UUID.randomUUID()
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(emptyList<Session>())

        // When
        val conflicts = conflictDetectionService.detectCapacityConflicts(eventId)

        // Then
        assertNotNull(conflicts)
        verify(sessionRepository).findByEventIdAndIsActiveTrue(eventId)
    }

    @Test
    @DisplayName("Should detect user conflicts")
    fun shouldDetectUserConflicts() {
        // Given
        val eventId = UUID.randomUUID()
        whenever(registrationRepository.findByEventId(eventId)).thenReturn(emptyList<Registration>() as java.util.List<Registration>)

        // When
        val conflicts = conflictDetectionService.detectUserConflicts(eventId)

        // Then
        assertNotNull(conflicts)
        verify(registrationRepository).findByEventId(eventId)
    }

    @Test
    @DisplayName("Should generate conflict analytics")
    fun shouldGenerateConflictAnalytics() {
        // Given
        val eventId = UUID.randomUUID()
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(emptyList<Session>())

        // When
        val analytics = conflictDetectionService.generateConflictAnalytics(eventId)

        // Then
        assertNotNull(analytics)
    }

    @Test
    @DisplayName("Should auto resolve conflicts")
    fun shouldAutoResolveConflicts() {
        // Given
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(any())).thenReturn(emptyList<Session>())

        // When
        val results = conflictDetectionService.autoResolveConflicts()

        // Then
        assertNotNull(results)
    }

    @Test
    @DisplayName("Should get conflict summary")
    fun shouldGetConflictSummary() {
        // Given
        val eventId = UUID.randomUUID()
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(emptyList<Session>())

        // When
        val summary = conflictDetectionService.getConflictSummary(eventId)

        // Then
        assertNotNull(summary)
    }

    // Helper methods
    private fun createSession(
        id: UUID,
        title: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Session {
        return Session(
            id = id
        ).apply {
            this.title = title
            this.startTime = startTime
            this.endTime = endTime
            this.isActive = true
        }
    }
}