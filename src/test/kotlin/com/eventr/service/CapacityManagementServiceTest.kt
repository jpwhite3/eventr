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

@DisplayName("CapacityManagementService Tests")
class CapacityManagementServiceTest {

    private lateinit var sessionCapacityRepository: SessionCapacityRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionRegistrationRepository: SessionRegistrationRepository
    private lateinit var registrationRepository: RegistrationRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var capacityManagementService: CapacityManagementService

    @BeforeEach
    fun setUp() {
        sessionCapacityRepository = mock()
        sessionRepository = mock()
        sessionRegistrationRepository = mock()
        registrationRepository = mock()
        eventRepository = mock()
        
        capacityManagementService = CapacityManagementService(
            sessionCapacityRepository,
            sessionRepository,
            sessionRegistrationRepository,
            registrationRepository,
            eventRepository
        )
    }

    @Test
    @DisplayName("Should create session capacity successfully")
    fun shouldCreateSessionCapacitySuccessfully() {
        // Given
        val sessionId = UUID.randomUUID()
        val session = createSession(sessionId, "Test Session")
        val capacityDto = SessionCapacityDto(
            sessionId = sessionId,
            capacityType = "FIXED",
            maximumCapacity = 50,
            minimumCapacity = 10,
            enableWaitlist = true,
            waitlistCapacity = 20
        )
        
        val savedCapacity = createSessionCapacity(sessionId, session)
        
        whenever(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session))
        whenever(sessionCapacityRepository.findBySessionId(sessionId)).thenReturn(null)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.REGISTERED)).thenReturn(0L)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.WAITLIST)).thenReturn(0L)
        whenever(sessionCapacityRepository.save(any<SessionCapacity>())).thenReturn(savedCapacity)

        // When
        val result = capacityManagementService.createSessionCapacity(sessionId, capacityDto)

        // Then
        assertNotNull(result)
        assertEquals(sessionId, result.sessionId)
        assertEquals(50, result.maximumCapacity)
        assertEquals(10, result.minimumCapacity)
        assertTrue(result.enableWaitlist ?: false)
        verify(sessionCapacityRepository).save(any<SessionCapacity>())
    }

    @Test
    @DisplayName("Should throw exception when session not found")
    fun shouldThrowExceptionWhenSessionNotFound() {
        // Given
        val sessionId = UUID.randomUUID()
        val capacityDto = SessionCapacityDto(sessionId = sessionId)
        
        whenever(sessionRepository.findById(sessionId)).thenReturn(Optional.empty())

        // When & Then
        assertThrows<IllegalArgumentException> {
            capacityManagementService.createSessionCapacity(sessionId, capacityDto)
        }
    }

    @Test
    @DisplayName("Should throw exception when capacity already exists")
    fun shouldThrowExceptionWhenCapacityAlreadyExists() {
        // Given
        val sessionId = UUID.randomUUID()
        val session = createSession(sessionId, "Test Session")
        val existingCapacity = createSessionCapacity(sessionId, session)
        val capacityDto = SessionCapacityDto(sessionId = sessionId)
        
        whenever(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session))
        whenever(sessionCapacityRepository.findBySessionId(sessionId)).thenReturn(existingCapacity)

        // When & Then
        assertThrows<IllegalArgumentException> {
            capacityManagementService.createSessionCapacity(sessionId, capacityDto)
        }
    }

    @Test
    @DisplayName("Should update session capacity successfully")
    fun shouldUpdateSessionCapacitySuccessfully() {
        // Given
        val sessionId = UUID.randomUUID()
        val session = createSession(sessionId, "Test Session")
        val existingCapacity = createSessionCapacity(sessionId, session)
        val updateDto = CapacityUpdateDto(
            maximumCapacity = 100,
            minimumCapacity = 20,
            reason = "Increased venue size"
        )
        
        whenever(sessionCapacityRepository.findBySessionId(sessionId)).thenReturn(existingCapacity)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.REGISTERED)).thenReturn(25L)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.WAITLIST)).thenReturn(5L)
        whenever(sessionCapacityRepository.save(any<SessionCapacity>())).thenReturn(existingCapacity)

        // When
        val result = capacityManagementService.updateSessionCapacity(sessionId, updateDto)

        // Then
        assertNotNull(result)
        assertEquals(100, result.maximumCapacity)
        assertEquals(20, result.minimumCapacity)
        verify(sessionCapacityRepository).save(any<SessionCapacity>())
    }

    @Test
    @DisplayName("Should get session capacity successfully")
    fun shouldGetSessionCapacitySuccessfully() {
        // Given
        val sessionId = UUID.randomUUID()
        val session = createSession(sessionId, "Test Session")
        val capacity = createSessionCapacity(sessionId, session)
        
        whenever(sessionCapacityRepository.findBySessionId(sessionId)).thenReturn(capacity)

        // When
        val result = capacityManagementService.getSessionCapacity(sessionId)

        // Then
        assertNotNull(result)
        assertEquals(sessionId, result.sessionId)
        assertEquals("Test Session", result.sessionTitle)
    }

    @Test
    @DisplayName("Should generate event capacity analytics")
    fun shouldGenerateEventCapacityAnalytics() {
        // Given
        val eventId = UUID.randomUUID()
        val event = createEvent(eventId, "Test Event")
        val session = createSession(UUID.randomUUID(), "Test Session")
        val capacity = createSessionCapacity(session.id!!, session)
        
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(listOf(session))
        whenever(sessionCapacityRepository.findBySessionId(session.id!!)).thenReturn(capacity)
        whenever(sessionCapacityRepository.getAverageCapacityUtilization(eventId)).thenReturn(75.0)
        whenever(sessionCapacityRepository.countFullSessions(eventId)).thenReturn(2L)
        whenever(sessionCapacityRepository.findUnderCapacitySessions(eventId)).thenReturn(emptyList())
        whenever(sessionCapacityRepository.getTotalWaitlistCount(eventId)).thenReturn(10L)
        whenever(sessionCapacityRepository.findOverBookedSessions()).thenReturn(emptyList())

        // When
        val result = capacityManagementService.getEventCapacityAnalytics(eventId)

        // Then
        assertNotNull(result)
        assertEquals(eventId, result.eventId)
        assertEquals("Test Event", result.eventName)
        assertEquals(1, result.totalSessions)
        assertEquals(75.0, result.averageUtilization)
        assertEquals(2, result.fullSessionsCount)
        assertEquals(10, result.totalWaitlistCount)
    }

    @Test
    @DisplayName("Should promote from waitlist successfully")
    fun shouldPromoteFromWaitlistSuccessfully() {
        // Given
        val sessionId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val session = createSession(sessionId, "Test Session")
        val capacity = createSessionCapacity(sessionId, session).apply {
            enableWaitlist = true
            currentWaitlistCount = 5
            maximumCapacity = 50
            currentRegistrations = 45
        }
        val sessionReg = createSessionRegistration(registrationId, sessionId, SessionRegistrationStatus.WAITLIST)
        val promotionDto = WaitlistPromotionDto(
            sessionId = sessionId,
            registrationIds = listOf(registrationId),
            promotionReason = "Space available"
        )
        
        whenever(sessionCapacityRepository.findBySessionId(sessionId)).thenReturn(capacity)
        whenever(sessionRegistrationRepository.findByRegistrationIdAndSessionId(registrationId, sessionId)).thenReturn(sessionReg)
        whenever(sessionRegistrationRepository.save(any<SessionRegistration>())).thenReturn(sessionReg.apply { 
            status = SessionRegistrationStatus.REGISTERED 
        })
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.REGISTERED)).thenReturn(46L)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.WAITLIST)).thenReturn(4L)
        whenever(sessionCapacityRepository.save(any<SessionCapacity>())).thenReturn(capacity)

        // When
        val result = capacityManagementService.promoteFromWaitlist(promotionDto)

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        verify(sessionRegistrationRepository).save(any<SessionRegistration>())
        verify(sessionCapacityRepository).save(any<SessionCapacity>())
    }

    @Test
    @DisplayName("Should generate capacity optimization suggestions")
    fun shouldGenerateCapacityOptimizationSuggestions() {
        // Given
        val eventId = UUID.randomUUID()
        val session1 = createSession(UUID.randomUUID(), "Overbooked Session")
        val session2 = createSession(UUID.randomUUID(), "Under-utilized Session")
        
        val capacity1 = createSessionCapacity(session1.id!!, session1).apply {
            maximumCapacity = 50
            currentRegistrations = 50
            currentWaitlistCount = 20
        }
        
        val capacity2 = createSessionCapacity(session2.id!!, session2).apply {
            maximumCapacity = 100
            currentRegistrations = 20
            minimumCapacity = 30
            currentWaitlistCount = 0
        }
        
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(eventId)).thenReturn(listOf(session1, session2))
        whenever(sessionCapacityRepository.findBySessionId(session1.id!!)).thenReturn(capacity1)
        whenever(sessionCapacityRepository.findBySessionId(session2.id!!)).thenReturn(capacity2)

        // When
        val result = capacityManagementService.getCapacityOptimizationSuggestions(eventId)

        // Then
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.optimizationType == "INCREASE" })
        assertTrue(result.any { it.optimizationType == "DECREASE" })
    }

    // Helper methods
    private fun createSession(id: UUID, title: String): Session {
        return Session().apply {
            this.id = id
            this.title = title
            this.isActive = true
        }
    }

    private fun createEvent(id: UUID, name: String): Event {
        return Event().apply {
            this.id = id
            this.name = name
        }
    }

    private fun createSessionCapacity(sessionId: UUID, session: Session): SessionCapacity {
        return SessionCapacity().apply {
            this.id = UUID.randomUUID()
            this.session = session
            this.maximumCapacity = 50
            this.minimumCapacity = 10
            this.currentRegistrations = 25
            this.currentWaitlistCount = 5
            this.availableSpots = 25
            this.enableWaitlist = true
            this.waitlistCapacity = 20
            this.capacityType = "FIXED"
            this.lowCapacityThreshold = 5
            this.highDemandThreshold = 90
        }
    }

    private fun createSessionRegistration(registrationId: UUID, sessionId: UUID, status: SessionRegistrationStatus): SessionRegistration {
        val registration = Registration().apply { 
            this.id = registrationId
            this.userName = "Test User"
            this.userEmail = "test@example.com"
        }
        val session = Session().apply { 
            this.id = sessionId
            this.title = "Test Session"
        }
        
        return SessionRegistration().apply {
            this.id = UUID.randomUUID()
            this.registration = registration
            this.session = session
            this.status = status
            if (status == SessionRegistrationStatus.WAITLIST) {
                this.waitlistPosition = 1
                this.waitlistRegisteredAt = LocalDateTime.now()
            }
        }
    }
}