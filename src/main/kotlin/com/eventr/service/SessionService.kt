package com.eventr.service
import com.eventr.model.Session
import com.eventr.model.SessionType
import com.eventr.repository.SessionRepository
import com.eventr.repository.EventRepository
import com.eventr.repository.SessionRegistrationRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class SessionService(
    private val sessionRepository: SessionRepository,
    private val eventRepository: EventRepository,
    private val sessionRegistrationRepository: SessionRegistrationRepository
) {


    fun getSessionsByEvent(eventId: UUID): List<SessionDto> {
        return sessionRepository.findByEventIdAndIsActiveTrue(eventId).map { it.toDto() }
    }

    fun getSessionById(id: UUID): SessionDto? {
        return sessionRepository.findById(id).orElse(null)?.toDto()
    }

    fun createSession(createDto: CreateSessionDto): SessionDto {
        val event = eventRepository.findById(createDto.eventId).orElseThrow {
            IllegalArgumentException("Event not found")
        }
        
        val session = Session(
            event = event,
            title = createDto.title,
            description = createDto.description,
            type = SessionType.valueOf(createDto.sessionType?.uppercase() ?: "PRESENTATION"),
            startTime = LocalDateTime.parse(createDto.startDateTime),
            endTime = LocalDateTime.parse(createDto.endDateTime),
            location = createDto.location,
            presenter = createDto.speakerName,
            presenterBio = createDto.speakerBio,
            capacity = createDto.capacity,
            prerequisites = createDto.requirements?.joinToString("; "),
            materialUrl = createDto.materials?.joinToString("; ")
        )
        
        return sessionRepository.save(session).toDto()
    }

    fun updateSession(id: UUID, updateDto: UpdateSessionDto): SessionDto? {
        val session = sessionRepository.findById(id).orElse(null) ?: return null
        
        updateDto.title?.let { session.title = it }
        updateDto.description?.let { session.description = it }
        updateDto.startDateTime?.let { session.startTime = LocalDateTime.parse(it) }
        updateDto.endDateTime?.let { session.endTime = LocalDateTime.parse(it) }
        updateDto.location?.let { session.location = it }
        updateDto.speakerName?.let { session.presenter = it }
        updateDto.speakerBio?.let { session.presenterBio = it }
        updateDto.capacity?.let { session.capacity = it }
        updateDto.sessionType?.let { session.type = SessionType.valueOf(it.uppercase()) }
        updateDto.isActive?.let { session.isActive = it }
        updateDto.requirements?.let { session.prerequisites = it.joinToString("; ") }
        updateDto.materials?.let { session.materialUrl = it.joinToString("; ") }
        session.updatedAt = LocalDateTime.now()
        
        return sessionRepository.save(session).toDto()
    }

    fun deleteSession(id: UUID): Boolean {
        val session = sessionRepository.findById(id).orElse(null) ?: return false
        session.isActive = false
        session.updatedAt = LocalDateTime.now()
        sessionRepository.save(session)
        return true
    }

    fun getSessionAttendees(sessionId: UUID): List<AttendeeDto> {
        val sessionRegistrations = sessionRegistrationRepository.findBySessionIdOrderByRegisteredAtAsc(sessionId)
        return sessionRegistrations.map { sessionReg ->
            AttendeeDto(
                id = sessionReg.id!!,
                firstName = sessionReg.registration?.userName?.split(" ")?.firstOrNull() ?: "Unknown",
                lastName = sessionReg.registration?.userName?.split(" ")?.drop(1)?.joinToString(" ") ?: "Unknown", 
                email = sessionReg.registration?.userEmail ?: "Unknown",
                registrationDate = sessionReg.registeredAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                checkInStatus = sessionReg.status.name,
                sessionId = sessionId
            )
        }
    }
    
    private fun Session.toDto(): SessionDto {
        val attendeeCount = sessionRegistrationRepository.countBySessionIdAndStatus(this.id!!, com.eventr.model.SessionRegistrationStatus.REGISTERED)
        
        return SessionDto(
            id = this.id!!,
            eventId = this.event?.id!!,
            title = this.title,
            description = this.description,
            startDateTime = this.startTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "",
            endDateTime = this.endTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "",
            location = this.location,
            speakerName = this.presenter,
            speakerBio = this.presenterBio,
            capacity = this.capacity,
            attendeeCount = attendeeCount.toInt(),
            sessionType = this.type.name,
            isActive = this.isActive,
            requirements = this.prerequisites?.split("; ") ?: emptyList(),
            materials = this.materialUrl?.split("; ") ?: emptyList(),
            createdAt = this.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            updatedAt = this.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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