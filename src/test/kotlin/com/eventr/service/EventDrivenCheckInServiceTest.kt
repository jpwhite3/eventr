package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import com.eventr.service.impl.EventDrivenCheckInService
import com.eventr.service.interfaces.CheckInServiceInterface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*
import org.junit.jupiter.api.Assertions.*

class EventDrivenCheckInServiceTest {

    @Mock
    private lateinit var checkInRepository: CheckInRepository

    @Mock
    private lateinit var registrationRepository: RegistrationRepository

    @Mock
    private lateinit var sessionRepository: SessionRepository

    @Mock
    private lateinit var eventRepository: EventRepository

    private lateinit var checkInService: CheckInServiceInterface

    private val testRegistrationId = UUID.randomUUID()
    private val testSessionId = UUID.randomUUID()
    private val testEventId = UUID.randomUUID()
    private val testCheckInId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        checkInService = EventDrivenCheckInService(
            checkInRepository,
            registrationRepository,
            sessionRepository,
            eventRepository
        )
    }

    @Test
    fun `manualCheckIn should create check-in`() {
        // Arrange
        val registration = createTestRegistration()
        val session = createTestSession()
        val createDto = CheckInCreateDto(
            registrationId = testRegistrationId,
            sessionId = testSessionId,
            type = CheckInType.SESSION,
            method = CheckInMethod.MANUAL,
            checkedInBy = "staff@example.com"
        )

        whenever(registrationRepository.findById(testRegistrationId))
            .thenReturn(Optional.of(registration))
        whenever(sessionRepository.findById(testSessionId))
            .thenReturn(Optional.of(session))
        whenever(checkInRepository.findByRegistrationIdAndSessionId(testRegistrationId, testSessionId))
            .thenReturn(null) // No existing check-in
        whenever(checkInRepository.save(any<CheckIn>())).thenAnswer { invocation ->
            val checkIn = invocation.arguments[0] as CheckIn
            checkIn.copy(id = testCheckInId)
        }

        // Act
        val result = checkInService.manualCheckIn(createDto)

        // Assert
        assertNotNull(result)
        assertEquals(testCheckInId, result?.id)
        assertEquals(testRegistrationId, result.registrationId)
        assertEquals(testSessionId, result.sessionId)
        assertEquals(CheckInType.SESSION, result.type)
        assertEquals(CheckInMethod.MANUAL, result.method)

        verify(registrationRepository).findById(testRegistrationId)
        verify(sessionRepository).findById(testSessionId)
        verify(checkInRepository).findByRegistrationIdAndSessionId(testRegistrationId, testSessionId)
        verify(checkInRepository).save(any<CheckIn>())
    }

    @Test
    fun `manualCheckIn should throw exception when registration not found`() {
        // Arrange
        val createDto = CheckInCreateDto(registrationId = testRegistrationId)

        whenever(registrationRepository.findById(testRegistrationId))
            .thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            checkInService.manualCheckIn(createDto)
        }

        verify(registrationRepository).findById(testRegistrationId) // Only one call since exception is thrown early
        verify(checkInRepository, never()).save(any<CheckIn>())
    }

    @Test
    fun `manualCheckIn should throw exception for duplicate check-in`() {
        // Arrange
        val registration = createTestRegistration()
        val existingCheckIn = createTestCheckIn()
        val createDto = CheckInCreateDto(
            registrationId = testRegistrationId,
            sessionId = testSessionId,
            type = CheckInType.SESSION
        )

        whenever(registrationRepository.findById(testRegistrationId))
            .thenReturn(Optional.of(registration))
        whenever(checkInRepository.findByRegistrationIdAndSessionId(testRegistrationId, testSessionId))
            .thenReturn(existingCheckIn)

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            checkInService.manualCheckIn(createDto)
        }

        verify(registrationRepository).findById(testRegistrationId) // Only one call since exception is thrown early
        verify(checkInRepository).findByRegistrationIdAndSessionId(testRegistrationId, testSessionId)
        verify(checkInRepository, never()).save(any<CheckIn>())
    }

    @Test
    fun `bulkCheckIn should process multiple registrations`() {
        // Arrange
        val registrationIds = listOf(testRegistrationId, UUID.randomUUID())
        val registration1 = createTestRegistration()
        val registration2 = createTestRegistration().copy(
            id = registrationIds[1], 
            userEmail = "user2@example.com"
        )

        whenever(registrationRepository.findById(registrationIds[0]))
            .thenReturn(Optional.of(registration1))
        whenever(registrationRepository.findById(registrationIds[1]))
            .thenReturn(Optional.of(registration2))
        whenever(checkInRepository.findByRegistrationIdAndType(any(), any()))
            .thenReturn(null) // No existing check-ins
        whenever(checkInRepository.save(any<CheckIn>())).thenAnswer { invocation ->
            val checkIn = invocation.arguments[0] as CheckIn
            checkIn.copy(id = UUID.randomUUID())
        }

        // Act
        val results = checkInService.bulkCheckIn(registrationIds, null)

        // Assert
        assertEquals(2, results.size)
        verify(registrationRepository, times(2)).findById(any()) // 2 registrations
        verify(checkInRepository, times(2)).save(any<CheckIn>())
    }

    @Test
    fun `bulkCheckIn should skip duplicate check-ins and continue processing`() {
        // Arrange
        val registrationIds = listOf(testRegistrationId, UUID.randomUUID())
        val registration1 = createTestRegistration()
        val registration2 = createTestRegistration().copy(id = registrationIds[1])
        val existingCheckIn = createTestCheckIn()

        whenever(registrationRepository.findById(registrationIds[0]))
            .thenReturn(Optional.of(registration1))
        whenever(registrationRepository.findById(registrationIds[1]))
            .thenReturn(Optional.of(registration2))
        whenever(checkInRepository.findByRegistrationIdAndType(registrationIds[0], CheckInType.EVENT))
            .thenReturn(existingCheckIn) // First one has duplicate
        whenever(checkInRepository.findByRegistrationIdAndType(registrationIds[1], CheckInType.EVENT))
            .thenReturn(null) // Second one is new
        whenever(checkInRepository.save(any<CheckIn>())).thenAnswer { invocation ->
            val checkIn = invocation.arguments[0] as CheckIn
            checkIn.copy(id = UUID.randomUUID())
        }

        // Act
        val results = checkInService.bulkCheckIn(registrationIds, null)

        // Assert
        assertEquals(1, results.size) // Only one successful check-in
        verify(registrationRepository, times(2)).findById(any()) // 2 registrations attempted
        verify(checkInRepository, times(1)).save(any<CheckIn>()) // Only one save
    }

    @Test
    fun `getCheckInStatistics should return correct statistics`() {
        // Arrange
        val event = createTestEvent()
        val registrations = listOf(
            createTestRegistration(),
            createTestRegistration().copy(id = UUID.randomUUID()),
            createTestRegistration().copy(id = UUID.randomUUID())
        )
        val checkIns = listOf(
            createTestCheckIn(),
            createTestCheckIn().copy(id = UUID.randomUUID(), registration = registrations[1])
        )

        whenever(eventRepository.findById(testEventId))
            .thenReturn(Optional.of(event))
        whenever(registrationRepository.findByEventId(testEventId))
            .thenReturn(registrations )
        whenever(checkInRepository.findByEventIdOrderByCheckedInAtDesc(testEventId))
            .thenReturn(checkIns)

        // Act
        val statistics = checkInService.getCheckInStatistics(testEventId)

        // Assert
        assertEquals(testEventId, statistics.eventId)
        assertEquals(event.name, statistics.eventName)
        assertEquals(3L, statistics.totalRegistrations)
        assertEquals(2L, statistics.totalCheckIns)
        assertEquals(2L, statistics.uniqueCheckIns) // Assuming different registrations
        assertEquals(66.67, statistics.checkInRate, 0.1) // 2/3 * 100

        verify(eventRepository).findById(testEventId)
        verify(registrationRepository).findByEventId(testEventId)
        verify(checkInRepository).findByEventIdOrderByCheckedInAtDesc(testEventId)
    }

    @Test
    fun `getCheckInsForRegistration should return registration check-ins`() {
        // Arrange
        val checkIns = listOf(createTestCheckIn(), createTestCheckIn().copy(id = UUID.randomUUID()))

        whenever(checkInRepository.findByRegistrationIdOrderByCheckedInAtDesc(testRegistrationId))
            .thenReturn(checkIns)

        // Act
        val result = checkInService.getCheckInsForRegistration(testRegistrationId)

        // Assert
        assertEquals(2, result.size)
        verify(checkInRepository).findByRegistrationIdOrderByCheckedInAtDesc(testRegistrationId)
    }

    @Test
    fun `getCheckInsForSession should return session check-ins`() {
        // Arrange
        val checkIns = listOf(createTestCheckIn())

        whenever(checkInRepository.findBySessionIdOrderByCheckedInAtDesc(testSessionId))
            .thenReturn(checkIns)

        // Act
        val result = checkInService.getCheckInsForSession(testSessionId)

        // Assert
        assertEquals(1, result.size)
        verify(checkInRepository).findBySessionIdOrderByCheckedInAtDesc(testSessionId)
    }

    @Test
    fun `getCheckInById should return check-in DTO when found`() {
        // Arrange
        val checkIn = createTestCheckIn()

        whenever(checkInRepository.findById(testCheckInId))
            .thenReturn(Optional.of(checkIn))

        // Act
        val result = checkInService.getCheckInById(testCheckInId)

        // Assert
        assertNotNull(result)
        assertEquals(testCheckInId, result?.id)
        verify(checkInRepository).findById(testCheckInId)
    }

    // Test helper methods

    private fun createTestRegistration(): Registration {
        return Registration(
            id = testRegistrationId,
            userEmail = "user@example.com",
            userName = "Test User",
            status = RegistrationStatus.REGISTERED,
            eventInstance = createTestEventInstance()
        )
    }

    private fun createTestEventInstance(): EventInstance {
        return EventInstance(
            id = UUID.randomUUID(),
            event = createTestEvent()
        )
    }

    private fun createTestEvent(): Event {
        return Event(
            id = testEventId,
            name = "Test Event",
            description = "Test event description"
        )
    }

    private fun createTestSession(): Session {
        return Session(
            id = testSessionId,
            title = "Test Session",
            event = createTestEvent()
        )
    }

    private fun createTestCheckIn(): CheckIn {
        return CheckIn(
            id = testCheckInId,
            registration = createTestRegistration(),
            session = createTestSession(),
            type = CheckInType.SESSION,
            method = CheckInMethod.MANUAL,
            checkedInAt = LocalDateTime.now()
        )
    }
}