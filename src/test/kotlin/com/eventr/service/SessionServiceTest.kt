package com.eventr.service

import com.eventr.model.*
import com.eventr.repository.SessionRepository
import com.eventr.repository.EventRepository
import com.eventr.repository.SessionRegistrationRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class SessionServiceTest {

    @Mock
    private lateinit var sessionRepository: SessionRepository
    
    @Mock
    private lateinit var eventRepository: EventRepository
    
    @Mock
    private lateinit var sessionRegistrationRepository: SessionRegistrationRepository
    
    private lateinit var sessionService: SessionService

    @BeforeEach
    fun setUp() {
        sessionService = SessionService(sessionRepository, eventRepository, sessionRegistrationRepository)
    }

    companion object {
        private val TEST_SESSION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
        private val TEST_EVENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174002")
        private val TEST_REGISTRATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174003")
        private val NOW = LocalDateTime.now()
        private val START_TIME = NOW.plusHours(1)
        private val END_TIME = NOW.plusHours(2)
        
        fun createTestEvent(
            id: UUID = TEST_EVENT_ID,
            name: String = "Test Event"
        ): Event {
            return Event(id = id).apply {
                this.name = name
                this.description = "Test event description"
                this.venueName = "Test Venue"
                this.city = "Test City"
                this.capacity = 100
                this.status = EventStatus.PUBLISHED
            }
        }
        
        fun createTestSession(
            id: UUID = TEST_SESSION_ID,
            event: Event = createTestEvent(),
            title: String = "Test Session",
            type: SessionType = SessionType.PRESENTATION,
            startTime: LocalDateTime = START_TIME,
            endTime: LocalDateTime = END_TIME,
            isActive: Boolean = true
        ): Session {
            return Session(
                id = id,
                event = event,
                title = title,
                description = "Test session description",
                type = type,
                startTime = startTime,
                endTime = endTime,
                location = "Room A",
                presenter = "John Doe",
                presenterBio = "Expert presenter",
                capacity = 50,
                prerequisites = "Basic knowledge",
                materialUrl = "https://example.com/materials",
                isActive = isActive,
                createdAt = NOW,
                updatedAt = NOW
            )
        }
        
        fun createTestSessionRegistration(
            id: UUID = UUID.randomUUID(),
            session: Session = createTestSession(),
            status: SessionRegistrationStatus = SessionRegistrationStatus.REGISTERED
        ): SessionRegistration {
            val registration = Registration(id = TEST_REGISTRATION_ID).apply {
                this.userName = "John Smith"
                this.userEmail = "john.smith@example.com"
            }
            
            return SessionRegistration(id = id).apply {
                this.session = session
                this.registration = registration
                this.status = status
                this.registeredAt = NOW
            }
        }
        
        fun createCreateSessionDto(
            eventId: UUID = TEST_EVENT_ID,
            title: String = "New Session",
            startDateTime: String = START_TIME.toString(),
            endDateTime: String = END_TIME.toString(),
            sessionType: String? = "PRESENTATION",
            capacity: Int? = 30
        ): CreateSessionDto {
            return CreateSessionDto(
                eventId = eventId,
                title = title,
                description = "New session description",
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                location = "Room B",
                speakerName = "Jane Doe",
                speakerBio = "Session speaker",
                capacity = capacity,
                sessionType = sessionType,
                requirements = listOf("requirement1", "requirement2"),
                materials = listOf("material1", "material2")
            )
        }
        
        fun createUpdateSessionDto(
            title: String? = "Updated Session",
            startDateTime: String? = START_TIME.plusHours(1).toString(),
            endDateTime: String? = END_TIME.plusHours(1).toString(),
            capacity: Int? = 40,
            isActive: Boolean? = true
        ): UpdateSessionDto {
            return UpdateSessionDto(
                title = title,
                description = "Updated description",
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                location = "Updated Room",
                speakerName = "Updated Speaker",
                speakerBio = "Updated bio",
                capacity = capacity,
                sessionType = "WORKSHOP",
                isActive = isActive,
                requirements = listOf("updated req1", "updated req2"),
                materials = listOf("updated mat1", "updated mat2")
            )
        }
    }

    // ================================================================================
    // CRUD Operations Tests
    // ================================================================================

    @Test
    fun `getSessionsByEvent should return active sessions for event`() {
        // Arrange
        val event = createTestEvent()
        val sessions = listOf(
            createTestSession(id = UUID.randomUUID(), event = event, title = "Session 1"),
            createTestSession(id = UUID.randomUUID(), event = event, title = "Session 2", type = SessionType.WORKSHOP)
        )
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(TEST_EVENT_ID)).thenReturn(sessions)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(any(), any())).thenReturn(25L)

        // Act
        val result = sessionService.getSessionsByEvent(TEST_EVENT_ID)

        // Assert
        assertEquals(2, result.size)
        assertEquals("Session 1", result[0].title)
        assertEquals("Session 2", result[1].title)
        assertEquals("PRESENTATION", result[0].sessionType)
        assertEquals("WORKSHOP", result[1].sessionType)
        assertEquals(25, result[0].attendeeCount)
        verify(sessionRepository).findByEventIdAndIsActiveTrue(TEST_EVENT_ID)
    }

    @Test
    fun `getSessionsByEvent should return empty list when no active sessions`() {
        // Arrange
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(TEST_EVENT_ID)).thenReturn(emptyList())

        // Act
        val result = sessionService.getSessionsByEvent(TEST_EVENT_ID)

        // Assert
        assertEquals(0, result.size)
        verify(sessionRepository).findByEventIdAndIsActiveTrue(TEST_EVENT_ID)
    }

    @Test
    fun `getSessionById should return session DTO when found`() {
        // Arrange
        val session = createTestSession()
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(session))
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(TEST_SESSION_ID, SessionRegistrationStatus.REGISTERED)).thenReturn(30L)

        // Act
        val result = sessionService.getSessionById(TEST_SESSION_ID)

        // Assert
        assertNotNull(result)
        assertEquals(TEST_SESSION_ID, result!!.id)
        assertEquals("Test Session", result.title)
        assertEquals("PRESENTATION", result.sessionType)
        assertEquals(50, result.capacity)
        assertEquals(30, result.attendeeCount)
        assertEquals("John Doe", result.speakerName)
        assertEquals(listOf("Basic knowledge"), result.requirements)
        assertEquals(listOf("https://example.com/materials"), result.materials)
        verify(sessionRepository).findById(TEST_SESSION_ID)
    }

    @Test
    fun `getSessionById should return null when session not found`() {
        // Arrange
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.empty())

        // Act
        val result = sessionService.getSessionById(TEST_SESSION_ID)

        // Assert
        assertNull(result)
        verify(sessionRepository).findById(TEST_SESSION_ID)
    }

    @Test
    fun `createSession should create and return new session DTO`() {
        // Arrange
        val event = createTestEvent()
        val createDto = createCreateSessionDto()
        val createdSession = createTestSession().apply {
            this.title = "New Session"
            this.type = SessionType.PRESENTATION
            this.capacity = 30
            this.location = "Room B"
            this.presenter = "Jane Doe"
            this.presenterBio = "Session speaker"
            this.prerequisites = "requirement1; requirement2"
            this.materialUrl = "material1; material2"
        }
        
        whenever(eventRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.of(event))
        whenever(sessionRepository.save(any<Session>())).thenReturn(createdSession)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(any(), any())).thenReturn(0L)

        // Act
        val result = sessionService.createSession(createDto)

        // Assert
        assertEquals("New Session", result.title)
        assertEquals("PRESENTATION", result.sessionType)
        assertEquals(30, result.capacity)
        assertEquals("Room B", result.location)
        assertEquals("Jane Doe", result.speakerName)
        assertEquals("Session speaker", result.speakerBio)
        assertEquals(listOf("requirement1", "requirement2"), result.requirements)
        assertEquals(listOf("material1", "material2"), result.materials)
        
        verify(eventRepository).findById(TEST_EVENT_ID)
        verify(sessionRepository).save(any<Session>())
    }

    @Test
    fun `createSession should handle invalid event ID`() {
        // Arrange
        val createDto = createCreateSessionDto()
        whenever(eventRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            sessionService.createSession(createDto)
        }
        
        verify(eventRepository).findById(TEST_EVENT_ID)
        verify(sessionRepository, never()).save(any<Session>())
    }

    @Test
    fun `createSession should handle default session type`() {
        // Arrange
        val event = createTestEvent()
        val createDto = createCreateSessionDto(sessionType = null)
        val createdSession = createTestSession(type = SessionType.PRESENTATION)
        
        whenever(eventRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.of(event))
        whenever(sessionRepository.save(any<Session>())).thenReturn(createdSession)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(any(), any())).thenReturn(0L)

        // Act
        val result = sessionService.createSession(createDto)

        // Assert
        assertEquals("PRESENTATION", result.sessionType)
        verify(sessionRepository).save(any<Session>())
    }

    @Test
    fun `updateSession should update existing session and return DTO`() {
        // Arrange
        val existingSession = createTestSession()
        val updateDto = createUpdateSessionDto()
        val updatedSession = existingSession.apply {
            this.title = "Updated Session"
            this.capacity = 40
            this.location = "Updated Room"
            this.type = SessionType.WORKSHOP
        }
        
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(existingSession))
        whenever(sessionRepository.save(any<Session>())).thenReturn(updatedSession)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(any(), any())).thenReturn(20L)

        // Act
        val result = sessionService.updateSession(TEST_SESSION_ID, updateDto)

        // Assert
        assertNotNull(result)
        assertEquals("Updated Session", result!!.title)
        assertEquals(40, result.capacity)
        assertEquals("Updated Room", result.location)
        assertEquals("WORKSHOP", result.sessionType)
        assertEquals(20, result.attendeeCount)
        
        verify(sessionRepository).findById(TEST_SESSION_ID)
        verify(sessionRepository).save(any<Session>())
    }

    @Test
    fun `updateSession should return null when session not found`() {
        // Arrange
        val updateDto = createUpdateSessionDto()
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.empty())

        // Act
        val result = sessionService.updateSession(TEST_SESSION_ID, updateDto)

        // Assert
        assertNull(result)
        verify(sessionRepository).findById(TEST_SESSION_ID)
        verify(sessionRepository, never()).save(any<Session>())
    }

    @Test
    fun `deleteSession should soft delete existing session`() {
        // Arrange
        val existingSession = createTestSession()
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(existingSession))
        whenever(sessionRepository.save(any<Session>())).thenReturn(existingSession)

        // Act
        val result = sessionService.deleteSession(TEST_SESSION_ID)

        // Assert
        assertTrue(result)
        verify(sessionRepository).findById(TEST_SESSION_ID)
        verify(sessionRepository).save(any<Session>())
    }

    @Test
    fun `deleteSession should return false when session not found`() {
        // Arrange
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.empty())

        // Act
        val result = sessionService.deleteSession(TEST_SESSION_ID)

        // Assert
        assertFalse(result)
        verify(sessionRepository).findById(TEST_SESSION_ID)
        verify(sessionRepository, never()).save(any<Session>())
    }

    // ================================================================================
    // Business Logic Tests
    // ================================================================================

    @Test
    fun `getSessionAttendees should return attendee DTOs for session`() {
        // Arrange
        val sessionRegistrations = listOf(
            createTestSessionRegistration(
                session = createTestSession(),
                status = SessionRegistrationStatus.REGISTERED
            ),
            createTestSessionRegistration(
                id = UUID.randomUUID(),
                session = createTestSession(),
                status = SessionRegistrationStatus.ATTENDED
            )
        )
        whenever(sessionRegistrationRepository.findBySessionIdOrderByRegisteredAtAsc(TEST_SESSION_ID))
            .thenReturn(sessionRegistrations)

        // Act
        val result = sessionService.getSessionAttendees(TEST_SESSION_ID)

        // Assert
        assertEquals(2, result.size)
        assertEquals("John", result[0].firstName)
        assertEquals("Smith", result[0].lastName)
        assertEquals("john.smith@example.com", result[0].email)
        assertEquals("REGISTERED", result[0].checkInStatus)
        assertEquals(TEST_SESSION_ID, result[0].sessionId)
        assertEquals("ATTENDED", result[1].checkInStatus)
        verify(sessionRegistrationRepository).findBySessionIdOrderByRegisteredAtAsc(TEST_SESSION_ID)
    }

    @Test
    fun `getSessionAttendees should handle empty registrations`() {
        // Arrange
        whenever(sessionRegistrationRepository.findBySessionIdOrderByRegisteredAtAsc(TEST_SESSION_ID))
            .thenReturn(emptyList())

        // Act
        val result = sessionService.getSessionAttendees(TEST_SESSION_ID)

        // Assert
        assertEquals(0, result.size)
        verify(sessionRegistrationRepository).findBySessionIdOrderByRegisteredAtAsc(TEST_SESSION_ID)
    }

    @Test
    fun `getSessionAttendees should handle attendees with single names`() {
        // Arrange
        val singleNameRegistration = Registration(id = TEST_REGISTRATION_ID).apply {
            this.userName = "Madonna"
            this.userEmail = "madonna@example.com"
        }
        val sessionReg = SessionRegistration(
            id = UUID.randomUUID(),
            session = createTestSession(),
            registration = singleNameRegistration,
            status = SessionRegistrationStatus.REGISTERED,
            registeredAt = NOW
        )
        whenever(sessionRegistrationRepository.findBySessionIdOrderByRegisteredAtAsc(TEST_SESSION_ID))
            .thenReturn(listOf(sessionReg))

        // Act
        val result = sessionService.getSessionAttendees(TEST_SESSION_ID)

        // Assert
        assertEquals(1, result.size)
        assertEquals("Madonna", result[0].firstName)
        assertEquals("", result[0].lastName)
        verify(sessionRegistrationRepository).findBySessionIdOrderByRegisteredAtAsc(TEST_SESSION_ID)
    }

    // ================================================================================
    // DTO Conversion Tests
    // ================================================================================

    @Test
    fun `toDto should convert Session entity to SessionDto correctly`() {
        // Arrange
        val session = createTestSession().apply {
            this.title = "Advanced Workshop"
            this.type = SessionType.WORKSHOP
            this.presenter = "Expert Speaker"
            this.capacity = 75
            this.prerequisites = "req1; req2; req3"
            this.materialUrl = "mat1; mat2"
        }
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(session))
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(TEST_SESSION_ID, SessionRegistrationStatus.REGISTERED)).thenReturn(45L)

        // Act
        val result = sessionService.getSessionById(TEST_SESSION_ID)!!

        // Assert
        assertEquals(TEST_SESSION_ID, result.id)
        assertEquals(TEST_EVENT_ID, result.eventId)
        assertEquals("Advanced Workshop", result.title)
        assertEquals("WORKSHOP", result.sessionType)
        assertEquals("Expert Speaker", result.speakerName)
        assertEquals(75, result.capacity)
        assertEquals(45, result.attendeeCount)
        assertEquals(listOf("req1", "req2", "req3"), result.requirements)
        assertEquals(listOf("mat1", "mat2"), result.materials)
        assertTrue(result.isActive)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun `toDto should handle null optional fields correctly`() {
        // Arrange
        val minimalSession = Session(id = TEST_SESSION_ID).apply {
            this.event = createTestEvent()
            this.title = "Minimal Session"
            this.type = SessionType.OTHER
            this.isActive = true
            this.createdAt = NOW
            this.updatedAt = NOW
        }
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(minimalSession))
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(TEST_SESSION_ID, SessionRegistrationStatus.REGISTERED)).thenReturn(0L)

        // Act
        val result = sessionService.getSessionById(TEST_SESSION_ID)!!

        // Assert
        assertEquals("Minimal Session", result.title)
        assertEquals("OTHER", result.sessionType)
        assertNull(result.description)
        assertNull(result.location)
        assertNull(result.speakerName)
        assertNull(result.speakerBio)
        assertNull(result.capacity)
        assertEquals(0, result.attendeeCount)
        assertEquals(emptyList<String>(), result.requirements)
        assertEquals(emptyList<String>(), result.materials)
        assertTrue(result.isActive)
    }

    @Test
    fun `toDto should handle empty prerequisite and material strings`() {
        // Arrange
        val session = createTestSession().apply {
            this.prerequisites = ""
            this.materialUrl = ""
        }
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(session))
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(any(), any())).thenReturn(10L)

        // Act
        val result = sessionService.getSessionById(TEST_SESSION_ID)!!

        // Assert
        assertEquals(listOf(""), result.requirements)
        assertEquals(listOf(""), result.materials)
    }
}