package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*

@DisplayName("PrerequisiteValidationService Tests")
class PrerequisiteValidationServiceTest {

    private lateinit var sessionPrerequisiteRepository: SessionPrerequisiteRepository
    private lateinit var sessionDependencyRepository: SessionDependencyRepository
    private lateinit var sessionRegistrationRepository: SessionRegistrationRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var checkInRepository: CheckInRepository
    private lateinit var prerequisiteValidationService: PrerequisiteValidationService

    @BeforeEach
    fun setUp() {
        sessionPrerequisiteRepository = mock()
        sessionDependencyRepository = mock()
        sessionRegistrationRepository = mock()
        sessionRepository = mock()
        checkInRepository = mock()
        
        prerequisiteValidationService = PrerequisiteValidationService(
            sessionPrerequisiteRepository,
            sessionDependencyRepository,
            sessionRegistrationRepository,
            sessionRepository,
            checkInRepository
        )
    }

    @Test
    @DisplayName("Should validate prerequisites successfully when all requirements met")
    fun shouldValidatePrerequisitesSuccessfullyWhenAllRequirementsMet() {
        // Given
        val sessionId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val prerequisiteSessionId = UUID.randomUUID()
        
        val prerequisite = createSessionPrerequisite(
            sessionId = sessionId,
            type = PrerequisiteType.SESSION_ATTENDANCE,
            isRequired = true,
            prerequisiteSessionId = prerequisiteSessionId
        )
        
        val checkIn = createCheckIn(registrationId, prerequisiteSessionId, CheckInType.SESSION)
        
        whenever(sessionPrerequisiteRepository.findBySessionIdAndIsActiveTrue(sessionId)).thenReturn(listOf(prerequisite))
        whenever(checkInRepository.findByRegistrationIdOrderByCheckedInAtDesc(registrationId)).thenReturn(listOf(checkIn))

        // When
        val result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId)

        // Then
        assertNotNull(result)
        assertEquals(sessionId, result.sessionId)
        assertEquals(registrationId, result.registrationId)
        assertTrue(result.canRegister)
        assertEquals("PASSED", result.overallStatus)
        assertEquals(1, result.validationResults.size)
        assertTrue(result.validationResults.first().isPassed)
    }

    @Test
    @DisplayName("Should fail validation when required prerequisites not met")
    fun shouldFailValidationWhenRequiredPrerequisitesNotMet() {
        // Given
        val sessionId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val prerequisiteSessionId = UUID.randomUUID()
        
        val prerequisite = createSessionPrerequisite(
            sessionId = sessionId,
            type = PrerequisiteType.SESSION_ATTENDANCE,
            isRequired = true,
            prerequisiteSessionId = prerequisiteSessionId
        )
        
        whenever(sessionPrerequisiteRepository.findBySessionIdAndIsActiveTrue(sessionId)).thenReturn(listOf(prerequisite))
        whenever(checkInRepository.findByRegistrationIdOrderByCheckedInAtDesc(registrationId)).thenReturn(emptyList())

        // When
        val result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId)

        // Then
        assertNotNull(result)
        assertFalse(result.canRegister)
        assertEquals("FAILED", result.overallStatus)
        assertFalse(result.validationResults.first().isPassed)
        assertTrue(result.failureReasons.isNotEmpty())
    }

    @Test
    @DisplayName("Should allow admin override when configured")
    fun shouldAllowAdminOverrideWhenConfigured() {
        // Given
        val sessionId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val prerequisiteSessionId = UUID.randomUUID()
        
        val prerequisite = createSessionPrerequisite(
            sessionId = sessionId,
            type = PrerequisiteType.SESSION_ATTENDANCE,
            isRequired = true,
            prerequisiteSessionId = prerequisiteSessionId,
            allowAdminOverride = true
        )
        
        whenever(sessionPrerequisiteRepository.findBySessionIdAndIsActiveTrue(sessionId)).thenReturn(listOf(prerequisite))
        whenever(checkInRepository.findByRegistrationIdOrderByCheckedInAtDesc(registrationId)).thenReturn(emptyList())

        // When
        val result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId)

        // Then
        assertNotNull(result)
        assertFalse(result.canRegister)
        assertEquals("FAILED_OVERRIDABLE", result.overallStatus)
        assertTrue(result.canAdminOverride)
    }

    @Test
    @DisplayName("Should validate session registration prerequisite")
    fun shouldValidateSessionRegistrationPrerequisite() {
        // Given
        val sessionId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val prerequisiteSessionId = UUID.randomUUID()
        
        val prerequisite = createSessionPrerequisite(
            sessionId = sessionId,
            type = PrerequisiteType.SESSION_REGISTRATION,
            isRequired = true,
            prerequisiteSessionId = prerequisiteSessionId
        )
        
        val sessionReg = createSessionRegistration(registrationId, prerequisiteSessionId, SessionRegistrationStatus.REGISTERED)
        
        whenever(sessionPrerequisiteRepository.findBySessionIdAndIsActiveTrue(sessionId)).thenReturn(listOf(prerequisite))
        whenever(sessionRegistrationRepository.findByRegistrationIdAndSessionId(registrationId, prerequisiteSessionId)).thenReturn(sessionReg)

        // When
        val result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId)

        // Then
        assertNotNull(result)
        assertTrue(result.canRegister)
        assertEquals("PASSED", result.overallStatus)
        assertTrue(result.validationResults.first().isPassed)
    }

    @Test
    @DisplayName("Should validate session dependencies")
    fun shouldValidateSessionDependencies() {
        // Given
        val sessionId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val parentSessionId = UUID.randomUUID()
        
        val parentSession = createSession(parentSessionId, "Parent Session")
        val dependency = createSessionDependency(parentSession, sessionId, DependencyType.SEQUENCE)
        
        whenever(sessionDependencyRepository.findByDependentSessionId(sessionId)).thenReturn(listOf(dependency))
        whenever(checkInRepository.findByRegistrationIdOrderByCheckedInAtDesc(registrationId)).thenReturn(emptyList())

        // When
        val result = prerequisiteValidationService.validateSessionDependencies(sessionId, registrationId)

        // Then
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertTrue(result.first().contains("Must complete 'Parent Session'"))
    }

    @Test
    @DisplayName("Should detect circular dependencies")
    fun shouldDetectCircularDependencies() {
        // Given
        val eventId = UUID.randomUUID()
        val session1 = createSession(UUID.randomUUID(), "Session 1")
        val session2 = createSession(UUID.randomUUID(), "Session 2")
        
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(listOf(session1, session2))
        whenever(sessionDependencyRepository.detectCircularDependencies(any())).thenReturn(
            listOf(arrayOf(session1.id.toString(), session2.id.toString()))
        )
        whenever(sessionRepository.findById(session1.id!!)).thenReturn(Optional.of(session1))
        whenever(sessionRepository.findById(session2.id!!)).thenReturn(Optional.of(session2))

        // When
        val result = prerequisiteValidationService.detectCircularDependencies(eventId)

        // Then
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(2, result.first().affectedSessions.size)
        assertEquals("ERROR", result.first().severity)
    }

    @Test
    @DisplayName("Should analyze dependency structure")
    fun shouldAnalyzeDependencyStructure() {
        // Given
        val eventId = UUID.randomUUID()
        val session1 = createSession(UUID.randomUUID(), "Session 1")
        val session2 = createSession(UUID.randomUUID(), "Session 2")
        val sessions = listOf(session1, session2)
        
        val prerequisite = createSessionPrerequisite(session1.id!!, PrerequisiteType.SESSION_ATTENDANCE, true, session2.id!!)
        val dependency = createSessionDependency(session1, session2.id!!, DependencyType.SEQUENCE)
        
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(sessions)
        whenever(sessionPrerequisiteRepository.findByEventId(eventId)).thenReturn(listOf(prerequisite))
        whenever(sessionDependencyRepository.findByEventId(eventId)).thenReturn(listOf(dependency))
        whenever(sessionDependencyRepository.findByParentSessionId(any())).thenReturn(emptyList())

        // When
        val result = prerequisiteValidationService.analyzeDependencyStructure(eventId)

        // Then
        assertNotNull(result)
        assertEquals(eventId, result.eventId)
        assertEquals(2, result.totalSessions)
        assertTrue(result.sessionsWithPrerequisites >= 0)
        assertTrue(result.sessionsWithDependencies >= 0)
        assertNotNull(result.dependencyMap)
        assertNotNull(result.prerequisiteMap)
    }

    @Test
    @DisplayName("Should get session dependency path")
    fun shouldGetSessionDependencyPath() {
        // Given
        val fromSessionId = UUID.randomUUID()
        val toSessionId = UUID.randomUUID()
        val intermediateSession = createSession(UUID.randomUUID(), "Intermediate Session")
        
        val pathResults = listOf(
            arrayOf("path_id", fromSessionId.toString()),
            arrayOf("path_id", intermediateSession.id.toString()),
            arrayOf("path_id", toSessionId.toString())
        )
        
        whenever(sessionDependencyRepository.findDependencyPath(fromSessionId, toSessionId)).thenReturn(pathResults as List<Array<Any>>)
        whenever(sessionRepository.findById(fromSessionId)).thenReturn(Optional.of(createSession(fromSessionId, "From Session")))
        whenever(sessionRepository.findById(intermediateSession.id!!)).thenReturn(Optional.of(intermediateSession))
        whenever(sessionRepository.findById(toSessionId)).thenReturn(Optional.of(createSession(toSessionId, "To Session")))

        // When
        val result = prerequisiteValidationService.getSessionDependencyPath(fromSessionId, toSessionId)

        // Then
        assertNotNull(result)
        assertEquals(fromSessionId, result.startSessionId)
        assertEquals(toSessionId, result.endSessionId)
        assertTrue(result.isValid)
        assertEquals(3, result.totalPathLength)
        assertNotNull(result.pathSessions)
    }

    @Test
    @DisplayName("Should return invalid path when no dependency path exists")
    fun shouldReturnInvalidPathWhenNoDependencyPathExists() {
        // Given
        val fromSessionId = UUID.randomUUID()
        val toSessionId = UUID.randomUUID()
        
        whenever(sessionDependencyRepository.findDependencyPath(fromSessionId, toSessionId)).thenReturn(emptyList())

        // When
        val result = prerequisiteValidationService.getSessionDependencyPath(fromSessionId, toSessionId)

        // Then
        assertNotNull(result)
        assertEquals(fromSessionId, result.startSessionId)
        assertEquals(toSessionId, result.endSessionId)
        assertFalse(result.isValid)
    }

    // Helper methods
    private fun createSessionPrerequisite(
        sessionId: UUID,
        type: PrerequisiteType,
        isRequired: Boolean,
        prerequisiteSessionId: UUID,
        allowAdminOverride: Boolean = false
    ): SessionPrerequisite {
        val session = createSession(sessionId, "Main Session")
        val prerequisiteSession = createSession(prerequisiteSessionId, "Prerequisite Session")
        
        return SessionPrerequisite(
            id = UUID.randomUUID()
        ).apply {
            this.session = session
            this.prerequisiteSession = prerequisiteSession
            this.type = type
            this.isRequired = isRequired
            this.allowAdminOverride = allowAdminOverride
            this.isActive = true
        }
    }

    private fun createSession(id: UUID, title: String): Session {
        return Session(
            id = id,
            title = title,
            isActive = true
        )
    }

    private fun createCheckIn(registrationId: UUID, sessionId: UUID, type: CheckInType): CheckIn {
        val registration = Registration(
            id = registrationId
        )
        val session = createSession(sessionId, "Test Session")
        
        return CheckIn(
            id = UUID.randomUUID()
        ).apply {
            this.registration = registration
            this.session = session
            this.type = type
            this.checkedInAt = LocalDateTime.now()
        }
    }

    private fun createSessionRegistration(
        registrationId: UUID, 
        sessionId: UUID, 
        status: SessionRegistrationStatus
    ): SessionRegistration {
        val registration = Registration(
            id = registrationId
        )
        val session = createSession(sessionId, "Test Session")
        
        return SessionRegistration(
            id = UUID.randomUUID()
        ).apply {
            this.registration = registration
            this.session = session
            this.status = status
        }
    }

    private fun createSessionDependency(
        parentSession: Session,
        dependentSessionId: UUID,
        type: DependencyType
    ): SessionDependency {
        val dependentSession = createSession(dependentSessionId, "Dependent Session")
        
        return SessionDependency(
            id = UUID.randomUUID()
        ).apply {
            this.parentSession = parentSession
            this.dependentSession = dependentSession
            this.dependencyType = type
        }
    }
}