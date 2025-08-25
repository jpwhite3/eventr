package com.eventr.service
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class SessionService {

    // Mock data for development
    private val mockSessions = mutableListOf<SessionDto>()

    init {
        // Initialize with mock data
        mockSessions.addAll(
            listOf(
                SessionDto(
                    id = UUID.fromString("650e8400-e29b-41d4-a716-446655440001"),
                    eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                    title = "Opening Keynote",
                    description = "Welcome and overview of the conference",
                    startDateTime = "2024-03-15T09:00:00",
                    endDateTime = "2024-03-15T10:00:00",
                    location = "Main Auditorium",
                    speakerName = "Dr. Jane Smith",
                    speakerBio = "Leading expert in React development and software architecture",
                    capacity = 500,
                    attendeeCount = 387,
                    sessionType = "KEYNOTE",
                    isActive = true,
                    requirements = listOf("Conference badge required"),
                    materials = listOf("Presentation slides will be available after session"),
                    createdAt = "2024-02-01T10:00:00",
                    updatedAt = "2024-02-20T14:30:00"
                ),
                SessionDto(
                    id = UUID.fromString("650e8400-e29b-41d4-a716-446655440002"),
                    eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                    title = "Advanced React Patterns",
                    description = "Deep dive into advanced React patterns and best practices",
                    startDateTime = "2024-03-15T10:30:00",
                    endDateTime = "2024-03-15T12:00:00",
                    location = "Tech Lab 1",
                    speakerName = "Mike Johnson",
                    speakerBio = "Senior React Developer at Tech Corp",
                    capacity = 50,
                    attendeeCount = 45,
                    sessionType = "WORKSHOP",
                    isActive = true,
                    requirements = listOf("Laptop required", "Node.js installed"),
                    materials = listOf("Code repository", "Exercise files"),
                    createdAt = "2024-02-01T10:30:00",
                    updatedAt = "2024-02-15T09:15:00"
                ),
                SessionDto(
                    id = UUID.fromString("650e8400-e29b-41d4-a716-446655440003"),
                    eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
                    title = "Machine Learning Fundamentals",
                    description = "Introduction to ML concepts and practical applications",
                    startDateTime = "2024-03-20T10:00:00",
                    endDateTime = "2024-03-20T12:00:00",
                    location = "Workshop Room A",
                    speakerName = "Dr. Sarah Lee",
                    speakerBio = "Data Science Professor and ML researcher",
                    capacity = 30,
                    attendeeCount = 25,
                    sessionType = "WORKSHOP",
                    isActive = true,
                    requirements = listOf("Basic Python knowledge helpful"),
                    materials = listOf("Jupyter notebooks", "Dataset samples"),
                    createdAt = "2024-02-05T11:00:00",
                    updatedAt = "2024-02-18T16:20:00"
                )
            )
        )
    }

    fun getSessionsByEvent(eventId: UUID): List<SessionDto> {
        return mockSessions.filter { it.eventId == eventId }
    }

    fun getSessionById(id: UUID): SessionDto? {
        return mockSessions.find { it.id == id }
    }

    fun createSession(createDto: CreateSessionDto): SessionDto {
        val newSession = SessionDto(
            id = UUID.randomUUID(),
            eventId = createDto.eventId,
            title = createDto.title,
            description = createDto.description,
            startDateTime = createDto.startDateTime,
            endDateTime = createDto.endDateTime,
            location = createDto.location,
            speakerName = createDto.speakerName,
            speakerBio = createDto.speakerBio,
            capacity = createDto.capacity,
            attendeeCount = 0,
            sessionType = createDto.sessionType ?: "PRESENTATION",
            isActive = true,
            requirements = createDto.requirements ?: emptyList(),
            materials = createDto.materials ?: emptyList(),
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        mockSessions.add(newSession)
        return newSession
    }

    fun updateSession(id: UUID, updateDto: UpdateSessionDto): SessionDto? {
        val index = mockSessions.indexOfFirst { it.id == id }
        if (index == -1) return null

        val existingSession = mockSessions[index]
        val updatedSession = existingSession.copy(
            title = updateDto.title ?: existingSession.title,
            description = updateDto.description ?: existingSession.description,
            startDateTime = updateDto.startDateTime ?: existingSession.startDateTime,
            endDateTime = updateDto.endDateTime ?: existingSession.endDateTime,
            location = updateDto.location ?: existingSession.location,
            speakerName = updateDto.speakerName ?: existingSession.speakerName,
            speakerBio = updateDto.speakerBio ?: existingSession.speakerBio,
            capacity = updateDto.capacity ?: existingSession.capacity,
            sessionType = updateDto.sessionType ?: existingSession.sessionType,
            isActive = updateDto.isActive ?: existingSession.isActive,
            requirements = updateDto.requirements ?: existingSession.requirements,
            materials = updateDto.materials ?: existingSession.materials,
            updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )

        mockSessions[index] = updatedSession
        return updatedSession
    }

    fun deleteSession(id: UUID): Boolean {
        return mockSessions.removeIf { it.id == id }
    }

    fun getSessionAttendees(sessionId: UUID): List<AttendeeDto> {
        // Mock attendees data
        return listOf(
            AttendeeDto(
                id = UUID.randomUUID(),
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                registrationDate = "2024-02-20T10:30:00",
                checkInStatus = "CHECKED_IN",
                sessionId = sessionId
            ),
            AttendeeDto(
                id = UUID.randomUUID(),
                firstName = "Jane",
                lastName = "Smith",
                email = "jane.smith@example.com",
                registrationDate = "2024-02-21T14:15:00",
                checkInStatus = "REGISTERED",
                sessionId = sessionId
            )
        )
    }
}

// DTOs for session management
data class SessionDto(
    val id: UUID,
    val eventId: UUID,
    val title: String,
    val description: String?,
    val startDateTime: String,
    val endDateTime: String,
    val location: String?,
    val speakerName: String?,
    val speakerBio: String?,
    val capacity: Int?,
    val attendeeCount: Int,
    val sessionType: String,
    val isActive: Boolean,
    val requirements: List<String>,
    val materials: List<String>,
    val createdAt: String,
    val updatedAt: String
)

data class CreateSessionDto(
    val eventId: UUID,
    val title: String,
    val description: String?,
    val startDateTime: String,
    val endDateTime: String,
    val location: String?,
    val speakerName: String?,
    val speakerBio: String?,
    val capacity: Int?,
    val sessionType: String?,
    val requirements: List<String>?,
    val materials: List<String>?
)

data class UpdateSessionDto(
    val title: String?,
    val description: String?,
    val startDateTime: String?,
    val endDateTime: String?,
    val location: String?,
    val speakerName: String?,
    val speakerBio: String?,
    val capacity: Int?,
    val sessionType: String?,
    val isActive: Boolean?,
    val requirements: List<String>?,
    val materials: List<String>?
)

data class AttendeeDto(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registrationDate: String,
    val checkInStatus: String,
    val sessionId: UUID
)