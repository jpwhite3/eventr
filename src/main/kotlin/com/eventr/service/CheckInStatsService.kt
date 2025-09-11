package com.eventr.service

import com.eventr.repository.EventRepository
import com.eventr.repository.RegistrationRepository
import com.eventr.repository.SessionRegistrationRepository
import com.eventr.repository.SessionRepository
import com.eventr.model.RegistrationStatus
import com.eventr.model.SessionRegistrationStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class CheckInStatsService(
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository,
    private val sessionRepository: SessionRepository,
    private val sessionRegistrationRepository: SessionRegistrationRepository
) {


    fun getEventCheckInStats(eventId: UUID): Map<String, Any> {
        // Check if event exists
        if (!eventRepository.existsById(eventId)) {
            return mapOf(
                "eventId" to eventId.toString(),
                "totalRegistrations" to 0,
                "totalCheckedIn" to 0,
                "checkInRate" to 0.0,
                "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "sessions" to emptyList<Map<String, Any>>()
            )
        }
        
        val totalRegistrations = registrationRepository.countByEventId(eventId)
        val totalCheckedInCount = registrationRepository.findByEventId(eventId).count { it.checkedIn }
        val checkInRate = if (totalRegistrations > 0) (totalCheckedInCount.toDouble() / totalRegistrations.toDouble()) * 100 else 0.0
        
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId).map { session ->
            val sessionCheckedIn = sessionRegistrationRepository.countBySessionIdAndStatus(session.id!!, SessionRegistrationStatus.ATTENDED)
            
            mapOf(
                "sessionId" to session.id.toString(),
                "name" to session.title,
                "checkedIn" to sessionCheckedIn,
                "capacity" to (session.capacity ?: 0)
            )
        }
        
        return mapOf(
            "eventId" to eventId.toString(),
            "totalRegistrations" to totalRegistrations,
            "totalCheckedIn" to totalCheckedInCount,
            "checkInRate" to checkInRate,
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "sessions" to sessions
        )
    }

    fun getSessionCheckInStats(sessionId: UUID): Map<String, Any> {
        val session = sessionRepository.findById(sessionId).orElse(null)
            ?: return mapOf(
                "sessionId" to sessionId.toString(),
                "sessionName" to "Unknown Session",
                "totalRegistrations" to 0,
                "totalCheckedIn" to 0,
                "checkInRate" to 0.0,
                "capacity" to 0,
                "startTime" to "",
                "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        
        val totalRegistrations = sessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.REGISTERED)
        val totalCheckedIn = sessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.ATTENDED)
        val checkInRate = if (totalRegistrations > 0) (totalCheckedIn.toDouble() / totalRegistrations.toDouble()) * 100 else 0.0
        
        return mapOf(
            "sessionId" to sessionId.toString(),
            "sessionName" to session.title,
            "totalRegistrations" to totalRegistrations,
            "totalCheckedIn" to totalCheckedIn,
            "checkInRate" to checkInRate,
            "capacity" to (session.capacity ?: 0),
            "startTime" to (session.startTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""),
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    fun checkInToEvent(eventId: UUID, checkInData: Map<String, String>): Map<String, Any> {
        val email = checkInData["email"] ?: throw IllegalArgumentException("Email is required")
        val qrCode = checkInData["qrCode"]
        
        // Find the registration for this user and event
        val registration = registrationRepository.findByUserEmailAndEventId(email, eventId)
            ?: throw IllegalArgumentException("Registration not found for this email and event")
        
        if (registration.checkedIn) {
            throw IllegalArgumentException("User is already checked in")
        }
        
        // Update registration status
        registration.checkedIn = true
        registrationRepository.save(registration)
        
        return mapOf(
            "success" to true,
            "message" to "Successfully checked in to event",
            "checkInTime" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "attendeeInfo" to mapOf(
                "email" to email,
                "checkInMethod" to if (qrCode != null) "QR_CODE" else "MANUAL",
                "name" to (registration.userName ?: "")
            )
        )
    }

    fun checkInToSession(sessionId: UUID, checkInData: Map<String, String>): Map<String, Any> {
        val email = checkInData["email"] ?: throw IllegalArgumentException("Email is required")
        val qrCode = checkInData["qrCode"]
        
        val session = sessionRepository.findById(sessionId).orElse(null)
            ?: throw IllegalArgumentException("Session not found")
        
        // Find the user's main event registration first
        val eventRegistration = registrationRepository.findByUserEmailAndEventId(email, session.event?.id!!)
            ?: throw IllegalArgumentException("User is not registered for this event")
        
        // Find or create session registration
        val sessionRegistration = sessionRegistrationRepository.findByRegistrationIdAndSessionId(eventRegistration.id!!, sessionId)
            ?: throw IllegalArgumentException("User is not registered for this session")
        
        if (sessionRegistration.status == SessionRegistrationStatus.ATTENDED) {
            throw IllegalArgumentException("User is already checked in to this session")
        }
        
        // Update session registration
        sessionRegistration.status = SessionRegistrationStatus.ATTENDED
        sessionRegistration.checkedInAt = LocalDateTime.now()
        sessionRegistration.verificationMethod = if (qrCode != null) "QR_CODE" else "MANUAL"
        sessionRegistration.attendanceVerified = true
        sessionRegistrationRepository.save(sessionRegistration)
        
        return mapOf(
            "success" to true,
            "message" to "Successfully checked in to session",
            "checkInTime" to sessionRegistration.checkedInAt!!.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "sessionInfo" to mapOf(
                "sessionId" to sessionId.toString(),
                "sessionName" to session.title
            ),
            "attendeeInfo" to mapOf(
                "email" to email,
                "checkInMethod" to (sessionRegistration.verificationMethod ?: "MANUAL"),
                "name" to (eventRegistration.userName ?: "")
            )
        )
    }

    fun getEventAttendees(eventId: UUID): List<Map<String, Any>> {
        val registrations = registrationRepository.findByEventId(eventId)
        return registrations.map { registration ->
            mapOf(
                "id" to (registration.id?.toString() ?: ""),
                "firstName" to (registration.userName?.split(" ")?.firstOrNull() ?: ""),
                "lastName" to (registration.userName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""),
                "email" to (registration.userEmail ?: ""),
                "registrationDate" to "", // Registration date not available in current model
                "checkInStatus" to if (registration.checkedIn) "CHECKED_IN" else "REGISTERED",
                "checkInTime" to "" // Check-in time not available in current model
            )
        }
    }
}