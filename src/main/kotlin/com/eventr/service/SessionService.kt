package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.EventRepository
import com.eventr.repository.SessionRepository
import com.eventr.repository.SessionRegistrationRepository
import com.eventr.repository.RegistrationRepository
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class SessionService(
    private val sessionRepository: SessionRepository,
    private val sessionRegistrationRepository: SessionRegistrationRepository,
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository
) {

    fun createSession(createDto: SessionCreateDto): SessionDto {
        val event = eventRepository.findById(createDto.eventId)
            .orElseThrow { IllegalArgumentException("Event not found") }
        
        // Check for room conflicts
        createDto.room?.let { room ->
            val conflicts = sessionRepository.findConflictingSessions(
                createDto.eventId, room, createDto.startTime, createDto.endTime
            )
            if (conflicts.isNotEmpty()) {
                throw IllegalArgumentException("Room '$room' is already booked for this time slot")
            }
        }
        
        // Check for presenter conflicts
        createDto.presenter?.let { presenter ->
            val conflicts = sessionRepository.findSessionsByPresenterAndTimeConflict(
                presenter, createDto.startTime, createDto.endTime
            )
            if (conflicts.isNotEmpty()) {
                throw IllegalArgumentException("Presenter '$presenter' has a conflict at this time")
            }
        }
        
        val session = Session().apply {
            BeanUtils.copyProperties(createDto, this)
            this.event = event
            this.tags = createDto.tags?.toMutableList() ?: mutableListOf()
        }
        
        val savedSession = sessionRepository.save(session)
        
        // Mark event as multi-session if it has more than one session
        if (!event.isMultiSession && sessionRepository.findByEventIdAndIsActiveTrue(createDto.eventId).size > 1) {
            event.isMultiSession = true
            eventRepository.save(event)
        }
        
        return convertToDto(savedSession)
    }

    fun updateSession(sessionId: UUID, updateDto: SessionUpdateDto): SessionDto {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        // Check for conflicts if time or room changed
        if ((updateDto.startTime != null || updateDto.endTime != null || updateDto.room != null)) {
            val startTime = updateDto.startTime ?: session.startTime!!
            val endTime = updateDto.endTime ?: session.endTime!!
            val room = updateDto.room ?: session.room
            
            room?.let {
                val conflicts = sessionRepository.findConflictingSessions(
                    session.event!!.id!!, it, startTime, endTime
                ).filter { conflict -> conflict.id != sessionId }
                
                if (conflicts.isNotEmpty()) {
                    throw IllegalArgumentException("Room conflict detected")
                }
            }
        }
        
        updateDto.title?.let { session.title = it }
        updateDto.description?.let { session.description = it }
        updateDto.type?.let { session.type = it }
        updateDto.startTime?.let { session.startTime = it }
        updateDto.endTime?.let { session.endTime = it }
        updateDto.location?.let { session.location = it }
        updateDto.room?.let { session.room = it }
        updateDto.building?.let { session.building = it }
        updateDto.capacity?.let { session.capacity = it }
        updateDto.isRegistrationRequired?.let { session.isRegistrationRequired = it }
        updateDto.isWaitlistEnabled?.let { session.isWaitlistEnabled = it }
        updateDto.presenter?.let { session.presenter = it }
        updateDto.presenterTitle?.let { session.presenterTitle = it }
        updateDto.presenterBio?.let { session.presenterBio = it }
        updateDto.presenterEmail?.let { session.presenterEmail = it }
        updateDto.materialUrl?.let { session.materialUrl = it }
        updateDto.recordingUrl?.let { session.recordingUrl = it }
        updateDto.slidesUrl?.let { session.slidesUrl = it }
        updateDto.prerequisites?.let { session.prerequisites = it }
        updateDto.targetAudience?.let { session.targetAudience = it }
        updateDto.difficultyLevel?.let { session.difficultyLevel = it }
        updateDto.tags?.let { session.tags = it.toMutableList() }
        
        session.updatedAt = LocalDateTime.now()
        
        return convertToDto(sessionRepository.save(session))
    }

    fun getSessionById(sessionId: UUID): SessionDto? {
        return sessionRepository.findById(sessionId)
            .map { convertToDto(it) }
            .orElse(null)
    }

    fun getSessionsByEventId(eventId: UUID): List<SessionDto> {
        return sessionRepository.findByEventIdOrderByStartTime(eventId)
            .map { convertToDto(it) }
    }

    fun deleteSession(sessionId: UUID) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        // Cancel all registrations first
        session.sessionRegistrations?.forEach { registration ->
            registration.status = SessionRegistrationStatus.CANCELLED
            registration.cancelledAt = LocalDateTime.now()
        }
        
        session.isActive = false
        sessionRepository.save(session)
    }

    fun registerForSession(createDto: SessionRegistrationCreateDto): SessionRegistrationDto {
        val session = sessionRepository.findById(createDto.sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val registration = registrationRepository.findById(createDto.registrationId)
            .orElseThrow { IllegalArgumentException("Registration not found") }
        
        // Check if already registered
        sessionRegistrationRepository.findBySessionIdAndRegistrationId(
            createDto.sessionId, createDto.registrationId
        )?.let {
            throw IllegalArgumentException("Already registered for this session")
        }
        
        // Check for time conflicts
        session.startTime?.let { startTime ->
            session.endTime?.let { endTime ->
                val conflicts = sessionRegistrationRepository.findConflictingUserSessions(
                    createDto.registrationId, startTime, endTime
                )
                if (conflicts.isNotEmpty()) {
                    throw IllegalArgumentException("Time conflict with another registered session")
                }
            }
        }
        
        val confirmedCount = sessionRegistrationRepository.countConfirmedBySessionId(createDto.sessionId)
        val isWaitlist = session.capacity?.let { capacity -> confirmedCount >= capacity } ?: false
        
        val sessionRegistration = SessionRegistration().apply {
            this.session = session
            this.registration = registration
            this.status = if (isWaitlist) SessionRegistrationStatus.WAITLIST else SessionRegistrationStatus.REGISTERED
            this.notes = createDto.notes
            
            if (isWaitlist) {
                val waitlistCount = sessionRegistrationRepository.countWaitlistBySessionId(createDto.sessionId)
                this.waitlistPosition = (waitlistCount + 1).toInt()
                this.waitlistRegisteredAt = LocalDateTime.now()
            }
        }
        
        val saved = sessionRegistrationRepository.save(sessionRegistration)
        return convertToDto(saved)
    }

    fun cancelSessionRegistration(sessionRegistrationId: UUID): SessionRegistrationDto {
        val sessionRegistration = sessionRegistrationRepository.findById(sessionRegistrationId)
            .orElseThrow { IllegalArgumentException("Session registration not found") }
        
        sessionRegistration.status = SessionRegistrationStatus.CANCELLED
        sessionRegistration.cancelledAt = LocalDateTime.now()
        sessionRegistration.updatedAt = LocalDateTime.now()
        
        val saved = sessionRegistrationRepository.save(sessionRegistration)
        
        // Move waitlist up if this was a confirmed registration
        if (sessionRegistration.status == SessionRegistrationStatus.REGISTERED) {
            promoteFromWaitlist(sessionRegistration.session!!.id!!)
        }
        
        return convertToDto(saved)
    }

    fun checkInToSession(attendanceDto: SessionAttendanceDto): SessionRegistrationDto {
        val sessionRegistration = sessionRegistrationRepository.findById(attendanceDto.sessionRegistrationId)
            .orElseThrow { IllegalArgumentException("Session registration not found") }
        
        if (sessionRegistration.status != SessionRegistrationStatus.REGISTERED) {
            throw IllegalArgumentException("Cannot check in - registration is not confirmed")
        }
        
        sessionRegistration.status = SessionRegistrationStatus.ATTENDED
        sessionRegistration.checkedInAt = LocalDateTime.now()
        sessionRegistration.attendanceVerified = true
        sessionRegistration.verificationMethod = attendanceDto.verificationMethod
        sessionRegistration.notes = attendanceDto.notes
        sessionRegistration.updatedAt = LocalDateTime.now()
        
        return convertToDto(sessionRegistrationRepository.save(sessionRegistration))
    }

    fun getSessionRegistrations(sessionId: UUID): List<SessionRegistrationDto> {
        return sessionRegistrationRepository.findBySessionIdOrderByRegisteredAtAsc(sessionId)
            .map { convertToDto(it) }
    }

    fun getUserSessionRegistrations(registrationId: UUID): List<SessionRegistrationDto> {
        return sessionRegistrationRepository.findByRegistrationIdOrderBySessionStartTime(registrationId)
            .map { convertToDto(it) }
    }

    private fun promoteFromWaitlist(sessionId: UUID) {
        val waitlist = sessionRegistrationRepository.findWaitlistBySessionIdOrderByPosition(sessionId)
        if (waitlist.isNotEmpty()) {
            val firstInWaitlist = waitlist.first()
            firstInWaitlist.status = SessionRegistrationStatus.REGISTERED
            firstInWaitlist.waitlistPosition = null
            firstInWaitlist.waitlistRegisteredAt = null
            firstInWaitlist.updatedAt = LocalDateTime.now()
            sessionRegistrationRepository.save(firstInWaitlist)
            
            // Update positions for remaining waitlist
            waitlist.drop(1).forEachIndexed { index, reg ->
                reg.waitlistPosition = index + 1
                sessionRegistrationRepository.save(reg)
            }
        }
    }

    private fun convertToDto(session: Session): SessionDto {
        val dto = SessionDto()
        BeanUtils.copyProperties(session, dto)
        dto.eventId = session.event?.id
        dto.tags = session.tags?.toList()
        
        // Calculate statistics
        dto.registeredCount = sessionRegistrationRepository.countConfirmedBySessionId(session.id!!).toInt()
        dto.waitlistCount = sessionRegistrationRepository.countWaitlistBySessionId(session.id!!).toInt()
        dto.attendedCount = sessionRegistrationRepository.findAttendedBySessionId(session.id!!).size
        dto.availableSpots = session.capacity?.minus(dto.registeredCount) ?: 0
        
        return dto
    }

    private fun convertToDto(sessionRegistration: SessionRegistration): SessionRegistrationDto {
        val dto = SessionRegistrationDto()
        BeanUtils.copyProperties(sessionRegistration, dto)
        dto.sessionId = sessionRegistration.session?.id
        dto.registrationId = sessionRegistration.registration?.id
        dto.sessionTitle = sessionRegistration.session?.title
        dto.sessionStartTime = sessionRegistration.session?.startTime
        dto.sessionEndTime = sessionRegistration.session?.endTime
        dto.sessionLocation = sessionRegistration.session?.location
        dto.sessionRoom = sessionRegistration.session?.room
        dto.userName = sessionRegistration.registration?.userName
        dto.userEmail = sessionRegistration.registration?.userEmail
        return dto
    }
}