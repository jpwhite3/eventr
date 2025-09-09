package com.eventr.service

import com.eventr.model.*
import com.eventr.repository.EventRepository
import com.eventr.repository.RegistrationRepository
import com.eventr.repository.SessionRegistrationRepository
import com.eventr.repository.SessionRepository
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
class CheckInStatsServiceTest {

    @Mock
    private lateinit var eventRepository: EventRepository
    
    @Mock
    private lateinit var registrationRepository: RegistrationRepository
    
    @Mock
    private lateinit var sessionRepository: SessionRepository
    
    @Mock
    private lateinit var sessionRegistrationRepository: SessionRegistrationRepository
    
    private lateinit var checkInStatsService: CheckInStatsService

    @BeforeEach
    fun setUp() {
        checkInStatsService = CheckInStatsService(
            eventRepository,
            registrationRepository,
            sessionRepository,
            sessionRegistrationRepository
        )
    }

    companion object {
        private val TEST_EVENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
        private val TEST_SESSION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174002")
        private val TEST_REGISTRATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174003")
        private val TEST_SESSION_REGISTRATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174004")
        private val NOW = LocalDateTime.now()
        
        fun createTestEvent(
            id: UUID = TEST_EVENT_ID,
            name: String = "Test Event"
        ): Event {
            return Event(id = id).apply {
                this.name = name
                this.description = "Test event description"
                this.capacity = 100
                this.status = EventStatus.PUBLISHED
            }
        }
        
        fun createTestSession(
            id: UUID = TEST_SESSION_ID,
            event: Event = createTestEvent(),
            title: String = "Test Session",
            capacity: Int? = 50
        ): Session {
            return Session(id = id).apply {
                this.event = event
                this.title = title
                this.description = "Test session description"
                this.type = SessionType.PRESENTATION
                this.startTime = NOW.plusHours(1)
                this.endTime = NOW.plusHours(2)
                this.capacity = capacity
                this.isActive = true
            }
        }
        
        fun createTestRegistration(
            id: UUID = TEST_REGISTRATION_ID,
            userName: String = "John Doe",
            userEmail: String = "john.doe@example.com",
            checkedIn: Boolean = false
        ): Registration {
            return Registration(id = id).apply {
                this.userName = userName
                this.userEmail = userEmail
                this.checkedIn = checkedIn
                this.status = RegistrationStatus.REGISTERED
            }
        }
        
        fun createTestSessionRegistration(
            id: UUID = TEST_SESSION_REGISTRATION_ID,
            session: Session = createTestSession(),
            registration: Registration = createTestRegistration(),
            status: SessionRegistrationStatus = SessionRegistrationStatus.REGISTERED
        ): SessionRegistration {
            return SessionRegistration(id = id).apply {
                this.session = session
                this.registration = registration
                this.status = status
                this.registeredAt = NOW
                this.checkedInAt = if (status == SessionRegistrationStatus.ATTENDED) NOW else null
                this.attendanceVerified = status == SessionRegistrationStatus.ATTENDED
            }
        }
    }

    // ================================================================================
    // Event Check-in Stats Tests
    // ================================================================================

    @Test
    fun `getEventCheckInStats should return stats for valid event`() {
        // Arrange
        val event = createTestEvent()
        val registrations = listOf(
            createTestRegistration(id = UUID.randomUUID(), checkedIn = true),
            createTestRegistration(id = UUID.randomUUID(), checkedIn = false),
            createTestRegistration(id = UUID.randomUUID(), checkedIn = true)
        )
        val sessions = listOf(
            createTestSession(id = UUID.randomUUID(), title = "Session 1", capacity = 30),
            createTestSession(id = UUID.randomUUID(), title = "Session 2", capacity = 40)
        )
        
        whenever(eventRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.of(event))
        whenever(registrationRepository.countByEventId(TEST_EVENT_ID)).thenReturn(3L)
        whenever(registrationRepository.findByEventId(TEST_EVENT_ID)).thenReturn(registrations as java.util.List<Registration>)
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(TEST_EVENT_ID)).thenReturn(sessions)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(any(), eq(SessionRegistrationStatus.REGISTERED))).thenReturn(15L)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(any(), eq(SessionRegistrationStatus.ATTENDED))).thenReturn(10L)

        // Act
        val result = checkInStatsService.getEventCheckInStats(TEST_EVENT_ID)

        // Assert
        assertEquals(TEST_EVENT_ID.toString(), result["eventId"])
        assertEquals(3L, result["totalRegistrations"])
        assertEquals(2, result["totalCheckedIn"]) // 2 out of 3 are checked in
        assertEquals(66.66666666666667, result["checkInRate"] as Double, 0.01)
        assertNotNull(result["lastUpdated"])
        
        @Suppress("UNCHECKED_CAST")
        val sessionStats = result["sessions"] as List<Map<String, Any>>
        assertEquals(2, sessionStats.size)
        assertEquals("Session 1", sessionStats[0]["name"])
        assertEquals(10L, sessionStats[0]["checkedIn"])
        assertEquals(30, sessionStats[0]["capacity"])
        
        verify(eventRepository).findById(TEST_EVENT_ID)
        verify(registrationRepository).countByEventId(TEST_EVENT_ID)
        verify(registrationRepository).findByEventId(TEST_EVENT_ID)
    }

    @Test
    fun `getEventCheckInStats should return empty stats for non-existent event`() {
        // Arrange
        whenever(eventRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.empty())

        // Act
        val result = checkInStatsService.getEventCheckInStats(TEST_EVENT_ID)

        // Assert
        assertEquals(TEST_EVENT_ID.toString(), result["eventId"])
        assertEquals(0, result["totalRegistrations"])
        assertEquals(0, result["totalCheckedIn"])
        assertEquals(0.0, result["checkInRate"])
        assertNotNull(result["lastUpdated"])
        
        @Suppress("UNCHECKED_CAST")
        val sessions = result["sessions"] as List<Map<String, Any>>
        assertEquals(0, sessions.size)
        
        verify(eventRepository).findById(TEST_EVENT_ID)
        verify(registrationRepository, never()).countByEventId(any())
    }

    @Test
    fun `getEventCheckInStats should handle zero registrations`() {
        // Arrange
        val event = createTestEvent()
        
        whenever(eventRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.of(event))
        whenever(registrationRepository.countByEventId(TEST_EVENT_ID)).thenReturn(0L)
        whenever(registrationRepository.findByEventId(TEST_EVENT_ID)).thenReturn(emptyList<Registration>() as java.util.List<Registration>)
        whenever(sessionRepository.findByEventIdAndIsActiveTrue(TEST_EVENT_ID)).thenReturn(emptyList())

        // Act
        val result = checkInStatsService.getEventCheckInStats(TEST_EVENT_ID)

        // Assert
        assertEquals(0L, result["totalRegistrations"])
        assertEquals(0, result["totalCheckedIn"])
        assertEquals(0.0, result["checkInRate"])
    }

    // ================================================================================
    // Session Check-in Stats Tests
    // ================================================================================

    @Test
    fun `getSessionCheckInStats should return stats for valid session`() {
        // Arrange
        val session = createTestSession(capacity = 50)
        
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(session))
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(TEST_SESSION_ID, SessionRegistrationStatus.REGISTERED)).thenReturn(40L)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(TEST_SESSION_ID, SessionRegistrationStatus.ATTENDED)).thenReturn(30L)

        // Act
        val result = checkInStatsService.getSessionCheckInStats(TEST_SESSION_ID)

        // Assert
        assertEquals(TEST_SESSION_ID.toString(), result["sessionId"])
        assertEquals("Test Session", result["sessionName"])
        assertEquals(40L, result["totalRegistrations"])
        assertEquals(30L, result["totalCheckedIn"])
        assertEquals(75.0, result["checkInRate"])
        assertEquals(50, result["capacity"])
        assertNotNull(result["startTime"])
        assertNotNull(result["lastUpdated"])
        
        verify(sessionRepository).findById(TEST_SESSION_ID)
        verify(sessionRegistrationRepository).countBySessionIdAndStatus(TEST_SESSION_ID, SessionRegistrationStatus.REGISTERED)
        verify(sessionRegistrationRepository).countBySessionIdAndStatus(TEST_SESSION_ID, SessionRegistrationStatus.ATTENDED)
    }

    @Test
    fun `getSessionCheckInStats should return empty stats for non-existent session`() {
        // Arrange
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.empty())

        // Act
        val result = checkInStatsService.getSessionCheckInStats(TEST_SESSION_ID)

        // Assert
        assertEquals(TEST_SESSION_ID.toString(), result["sessionId"])
        assertEquals("Unknown Session", result["sessionName"])
        assertEquals(0, result["totalRegistrations"])
        assertEquals(0, result["totalCheckedIn"])
        assertEquals(0.0, result["checkInRate"])
        assertEquals(0, result["capacity"])
        assertEquals("", result["startTime"])
        
        verify(sessionRepository).findById(TEST_SESSION_ID)
        verify(sessionRegistrationRepository, never()).countBySessionIdAndStatus(any(), any())
    }

    @Test
    fun `getSessionCheckInStats should handle zero registrations`() {
        // Arrange
        val session = createTestSession()
        
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(session))
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(TEST_SESSION_ID, SessionRegistrationStatus.REGISTERED)).thenReturn(0L)
        whenever(sessionRegistrationRepository.countBySessionIdAndStatus(TEST_SESSION_ID, SessionRegistrationStatus.ATTENDED)).thenReturn(0L)

        // Act
        val result = checkInStatsService.getSessionCheckInStats(TEST_SESSION_ID)

        // Assert
        assertEquals(0L, result["totalRegistrations"])
        assertEquals(0L, result["totalCheckedIn"])
        assertEquals(0.0, result["checkInRate"])
    }

    // ================================================================================
    // Event Check-in Tests
    // ================================================================================

    @Test
    fun `checkInToEvent should successfully check in user with QR code`() {
        // Arrange
        val registration = createTestRegistration(checkedIn = false)
        val checkInData = mapOf(
            "email" to "john.doe@example.com",
            "qrCode" to "test-qr-code"
        )
        
        whenever(registrationRepository.findByUserEmailAndEventId("john.doe@example.com", TEST_EVENT_ID))
            .thenReturn(registration)
        whenever(registrationRepository.save(any<Registration>())).thenReturn(registration)

        // Act
        val result = checkInStatsService.checkInToEvent(TEST_EVENT_ID, checkInData)

        // Assert
        assertTrue(result["success"] as Boolean)
        assertEquals("Successfully checked in to event", result["message"])
        assertNotNull(result["checkInTime"])
        
        @Suppress("UNCHECKED_CAST")
        val attendeeInfo = result["attendeeInfo"] as Map<String, Any>
        assertEquals("john.doe@example.com", attendeeInfo["email"])
        assertEquals("QR_CODE", attendeeInfo["checkInMethod"])
        assertEquals("John Doe", attendeeInfo["name"])
        
        assertTrue(registration.checkedIn)
        verify(registrationRepository).save(registration)
    }

    @Test
    fun `checkInToEvent should successfully check in user manually`() {
        // Arrange
        val registration = createTestRegistration(checkedIn = false)
        val checkInData = mapOf("email" to "john.doe@example.com")
        
        whenever(registrationRepository.findByUserEmailAndEventId("john.doe@example.com", TEST_EVENT_ID))
            .thenReturn(registration)
        whenever(registrationRepository.save(any<Registration>())).thenReturn(registration)

        // Act
        val result = checkInStatsService.checkInToEvent(TEST_EVENT_ID, checkInData)

        // Assert
        assertTrue(result["success"] as Boolean)
        @Suppress("UNCHECKED_CAST")
        val attendeeInfo = result["attendeeInfo"] as Map<String, Any>
        assertEquals("MANUAL", attendeeInfo["checkInMethod"])
    }

    @Test
    fun `checkInToEvent should throw error when email missing`() {
        // Arrange
        val checkInData = mapOf<String, String>()

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            checkInStatsService.checkInToEvent(TEST_EVENT_ID, checkInData)
        }
    }

    @Test
    fun `checkInToEvent should throw error when registration not found`() {
        // Arrange
        val checkInData = mapOf("email" to "nonexistent@example.com")
        
        whenever(registrationRepository.findByUserEmailAndEventId("nonexistent@example.com", TEST_EVENT_ID))
            .thenReturn(null)

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            checkInStatsService.checkInToEvent(TEST_EVENT_ID, checkInData)
        }
    }

    @Test
    fun `checkInToEvent should throw error when user already checked in`() {
        // Arrange
        val registration = createTestRegistration(checkedIn = true)
        val checkInData = mapOf("email" to "john.doe@example.com")
        
        whenever(registrationRepository.findByUserEmailAndEventId("john.doe@example.com", TEST_EVENT_ID))
            .thenReturn(registration)

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            checkInStatsService.checkInToEvent(TEST_EVENT_ID, checkInData)
        }
    }

    // ================================================================================
    // Session Check-in Tests
    // ================================================================================

    @Test
    fun `checkInToSession should successfully check in user to session`() {
        // Arrange
        val event = createTestEvent()
        val session = createTestSession(event = event)
        val registration = createTestRegistration()
        val sessionRegistration = createTestSessionRegistration(
            session = session,
            registration = registration,
            status = SessionRegistrationStatus.REGISTERED
        )
        val checkInData = mapOf(
            "email" to "john.doe@example.com",
            "qrCode" to "session-qr-code"
        )
        
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(session))
        whenever(registrationRepository.findByUserEmailAndEventId("john.doe@example.com", TEST_EVENT_ID))
            .thenReturn(registration)
        whenever(sessionRegistrationRepository.findByRegistrationIdAndSessionId(TEST_REGISTRATION_ID, TEST_SESSION_ID))
            .thenReturn(sessionRegistration)
        whenever(sessionRegistrationRepository.save(any<SessionRegistration>())).thenReturn(sessionRegistration)

        // Act
        val result = checkInStatsService.checkInToSession(TEST_SESSION_ID, checkInData)

        // Assert
        assertTrue(result["success"] as Boolean)
        assertEquals("Successfully checked in to session", result["message"])
        assertNotNull(result["checkInTime"])
        
        @Suppress("UNCHECKED_CAST")
        val sessionInfo = result["sessionInfo"] as Map<String, Any>
        assertEquals(TEST_SESSION_ID.toString(), sessionInfo["sessionId"])
        assertEquals("Test Session", sessionInfo["sessionName"])
        
        @Suppress("UNCHECKED_CAST")
        val attendeeInfo = result["attendeeInfo"] as Map<String, Any>
        assertEquals("john.doe@example.com", attendeeInfo["email"])
        assertEquals("QR_CODE", attendeeInfo["checkInMethod"])
        
        assertEquals(SessionRegistrationStatus.ATTENDED, sessionRegistration.status)
        assertTrue(sessionRegistration.attendanceVerified)
        assertNotNull(sessionRegistration.checkedInAt)
        verify(sessionRegistrationRepository).save(sessionRegistration)
    }

    @Test
    fun `checkInToSession should throw error when session not found`() {
        // Arrange
        val checkInData = mapOf("email" to "john.doe@example.com")
        
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            checkInStatsService.checkInToSession(TEST_SESSION_ID, checkInData)
        }
    }

    @Test
    fun `checkInToSession should throw error when user not registered for event`() {
        // Arrange
        val session = createTestSession()
        val checkInData = mapOf("email" to "unregistered@example.com")
        
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(session))
        whenever(registrationRepository.findByUserEmailAndEventId("unregistered@example.com", TEST_EVENT_ID))
            .thenReturn(null)

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            checkInStatsService.checkInToSession(TEST_SESSION_ID, checkInData)
        }
    }

    @Test
    fun `checkInToSession should throw error when user not registered for session`() {
        // Arrange
        val session = createTestSession()
        val registration = createTestRegistration()
        val checkInData = mapOf("email" to "john.doe@example.com")
        
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(session))
        whenever(registrationRepository.findByUserEmailAndEventId("john.doe@example.com", TEST_EVENT_ID))
            .thenReturn(registration)
        whenever(sessionRegistrationRepository.findByRegistrationIdAndSessionId(TEST_REGISTRATION_ID, TEST_SESSION_ID))
            .thenReturn(null)

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            checkInStatsService.checkInToSession(TEST_SESSION_ID, checkInData)
        }
    }

    @Test
    fun `checkInToSession should throw error when user already checked in to session`() {
        // Arrange
        val session = createTestSession()
        val registration = createTestRegistration()
        val sessionRegistration = createTestSessionRegistration(
            session = session,
            registration = registration,
            status = SessionRegistrationStatus.ATTENDED
        )
        val checkInData = mapOf("email" to "john.doe@example.com")
        
        whenever(sessionRepository.findById(TEST_SESSION_ID)).thenReturn(Optional.of(session))
        whenever(registrationRepository.findByUserEmailAndEventId("john.doe@example.com", TEST_EVENT_ID))
            .thenReturn(registration)
        whenever(sessionRegistrationRepository.findByRegistrationIdAndSessionId(TEST_REGISTRATION_ID, TEST_SESSION_ID))
            .thenReturn(sessionRegistration)

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            checkInStatsService.checkInToSession(TEST_SESSION_ID, checkInData)
        }
    }

    // ================================================================================
    // Event Attendees Tests
    // ================================================================================

    @Test
    fun `getEventAttendees should return list of attendees`() {
        // Arrange
        val registrations = listOf(
            createTestRegistration(
                id = UUID.randomUUID(),
                userName = "John Doe",
                userEmail = "john@example.com",
                checkedIn = true
            ),
            createTestRegistration(
                id = UUID.randomUUID(),
                userName = "Jane Smith",
                userEmail = "jane@example.com",
                checkedIn = false
            ),
            createTestRegistration(
                id = UUID.randomUUID(),
                userName = "Bob",
                userEmail = "bob@example.com",
                checkedIn = true
            )
        )
        
        whenever(registrationRepository.findByEventId(TEST_EVENT_ID)).thenReturn(registrations as java.util.List<Registration>)

        // Act
        val result = checkInStatsService.getEventAttendees(TEST_EVENT_ID)

        // Assert
        assertEquals(3, result.size)
        
        val firstAttendee = result[0]
        assertNotNull(firstAttendee["id"])
        assertEquals("John", firstAttendee["firstName"])
        assertEquals("Doe", firstAttendee["lastName"])
        assertEquals("john@example.com", firstAttendee["email"])
        assertEquals("CHECKED_IN", firstAttendee["checkInStatus"])
        
        val secondAttendee = result[1]
        assertEquals("Jane", secondAttendee["firstName"])
        assertEquals("Smith", secondAttendee["lastName"])
        assertEquals("REGISTERED", secondAttendee["checkInStatus"])
        
        val thirdAttendee = result[2]
        assertEquals("Bob", thirdAttendee["firstName"])
        assertEquals("", thirdAttendee["lastName"]) // Single name case
        assertEquals("CHECKED_IN", thirdAttendee["checkInStatus"])
        
        verify(registrationRepository).findByEventId(TEST_EVENT_ID)
    }

    @Test
    fun `getEventAttendees should return empty list when no registrations`() {
        // Arrange
        whenever(registrationRepository.findByEventId(TEST_EVENT_ID)).thenReturn(emptyList<Registration>() as java.util.List<Registration>)

        // Act
        val result = checkInStatsService.getEventAttendees(TEST_EVENT_ID)

        // Assert
        assertEquals(0, result.size)
        verify(registrationRepository).findByEventId(TEST_EVENT_ID)
    }
}