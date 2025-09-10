package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.model.CheckInMethod
import com.eventr.model.CheckInType
import com.eventr.repository.*
import com.eventr.service.CheckInOperationsService
import com.eventr.service.*
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Implementation of CheckInOperationsService focused on check-in business logic.
 * 
 * Responsibilities:
 * - Manual and bulk check-in processing
 * - Event and session check-in workflows
 * - Check-in validation and duplicate prevention
 * - Registration status management
 */
@Service
@Transactional
class CheckInOperationsServiceImpl(
    private val checkInRepository: CheckInRepository,
    private val registrationRepository: RegistrationRepository,
    private val sessionRepository: SessionRepository,
    private val eventRepository: EventRepository
) : CheckInOperationsService {

    override fun manualCheckIn(createDto: CheckInCreateDto): CheckInDto {
        val registration = registrationRepository.findById(createDto.registrationId)
            .orElseThrow { IllegalArgumentException("Registration not found") }
        
        // Check for duplicates
        if (!canCheckIn(createDto.registrationId, createDto.sessionId, createDto.type)) {
            throw IllegalArgumentException("Already checked in")
        }
        
        val checkIn = CheckIn().apply {
            this.registration = registration
            this.session = createDto.sessionId?.let { sessionRepository.findById(it).orElse(null) }
            this.type = createDto.type
            this.method = createDto.method
            this.checkedInBy = createDto.checkedInBy
            this.deviceId = createDto.deviceId
            this.deviceName = createDto.deviceName
            this.ipAddress = createDto.ipAddress
            this.userAgent = createDto.userAgent
            this.location = createDto.location
            this.verificationCode = createDto.verificationCode
            this.notes = createDto.notes
            this.metadata = createDto.metadata
        }
        
        val saved = checkInRepository.save(checkIn)
        
        // Update registration status
        updateRegistrationStatus(convertToDto(saved))
        
        return convertToDto(saved)
    }

    override fun bulkCheckIn(bulkDto: BulkCheckInDto): List<CheckInDto> {
        val results = mutableListOf<CheckInDto>()
        
        for (registrationId in bulkDto.registrationIds) {
            try {
                val createDto = CheckInCreateDto(
                    registrationId = registrationId,
                    sessionId = bulkDto.sessionId,
                    type = bulkDto.type,
                    method = CheckInMethod.BULK,
                    checkedInBy = bulkDto.checkedInBy,
                    location = bulkDto.location,
                    notes = bulkDto.notes
                )
                
                val checkIn = manualCheckIn(createDto)
                results.add(checkIn)
                
            } catch (e: Exception) {
                // Continue processing other registrations even if one fails
                // Log error but don't fail entire bulk operation
            }
        }
        
        return results
    }

    override fun checkInToEvent(eventId: UUID, userId: String, checkInDetails: CheckInDetailsDto): CheckInDto {
        val registration = findRegistrationByUserIdAndEventId(userId, eventId)
            ?: throw IllegalArgumentException("Registration not found for user $userId and event $eventId")
        
        // Check if already checked in to event
        if (!canCheckIn(registration.id!!, null, CheckInType.EVENT)) {
            throw IllegalArgumentException("User already checked in to event")
        }
        
        val checkIn = CheckIn().apply {
            this.registration = registration
            this.session = null
            this.type = CheckInType.EVENT
            this.method = checkInDetails.method
            this.checkedInBy = checkInDetails.checkedInBy
            this.deviceId = checkInDetails.deviceId
            this.deviceName = checkInDetails.deviceName
            this.ipAddress = checkInDetails.ipAddress
            this.userAgent = checkInDetails.userAgent
            this.location = checkInDetails.location
            this.verificationCode = checkInDetails.verificationCode
            this.notes = checkInDetails.notes
            this.metadata = checkInDetails.metadata?.toString()
        }
        
        val saved = checkInRepository.save(checkIn)
        
        // Update registration status
        updateRegistrationStatus(convertToDto(saved))
        
        return convertToDto(saved)
    }

    override fun checkInToSession(sessionId: UUID, userId: String, checkInDetails: CheckInDetailsDto): CheckInDto {
        val registration = findRegistrationByUserIdAndSessionId(userId, sessionId)
            ?: throw IllegalArgumentException("Registration not found for user $userId and session $sessionId")
        
        // Check if already checked in to session
        if (!canCheckIn(registration.id!!, sessionId, CheckInType.SESSION)) {
            throw IllegalArgumentException("User already checked in to session")
        }
        
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found: $sessionId") }
        
        val checkIn = CheckIn().apply {
            this.registration = registration
            this.session = session
            this.type = CheckInType.SESSION
            this.method = checkInDetails.method
            this.checkedInBy = checkInDetails.checkedInBy
            this.deviceId = checkInDetails.deviceId
            this.deviceName = checkInDetails.deviceName
            this.ipAddress = checkInDetails.ipAddress
            this.userAgent = checkInDetails.userAgent
            this.location = checkInDetails.location
            this.verificationCode = checkInDetails.verificationCode
            this.notes = checkInDetails.notes
            this.metadata = checkInDetails.metadata?.toString()
        }
        
        val saved = checkInRepository.save(checkIn)
        
        // Update registration status
        updateRegistrationStatus(convertToDto(saved))
        
        return convertToDto(saved)
    }

    override fun canCheckIn(registrationId: UUID, sessionId: UUID?, type: CheckInType): Boolean {
        val existingCheckIn = if (sessionId != null) {
            checkInRepository.findByRegistrationIdAndSessionId(registrationId, sessionId)
        } else {
            checkInRepository.findByRegistrationIdAndType(registrationId, type)
        }
        
        return existingCheckIn == null
    }

    override fun updateRegistrationStatus(checkIn: CheckInDto) {
        checkIn.registrationId?.let { registrationId ->
            val registration = registrationRepository.findById(registrationId)
                .orElse(null) ?: return
            
            // Update registration status to checked in if it was registered
            if (registration.status == RegistrationStatus.REGISTERED) {
                registration.status = RegistrationStatus.CHECKED_IN
                registrationRepository.save(registration)
            }
            
            // Update session registration status if applicable
            checkIn.sessionId?.let { sessionId ->
                updateSessionRegistrationStatus(registration, sessionId)
            }
        }
    }

    private fun updateSessionRegistrationStatus(registration: Registration, sessionId: UUID) {
        // Find and update session registration status
        // This would typically involve a SessionRegistration entity
        // For now, we'll just ensure the main registration status is updated
        if (registration.status == RegistrationStatus.REGISTERED) {
            registration.status = RegistrationStatus.CHECKED_IN
            registrationRepository.save(registration)
        }
    }

    private fun findRegistrationByUserIdAndEventId(userId: String, eventId: UUID): Registration? {
        return registrationRepository.findByUserEmailAndEventId(userId, eventId)
    }

    private fun findRegistrationByUserIdAndSessionId(userId: String, sessionId: UUID): Registration? {
        // Find registration through session's event
        val session = sessionRepository.findById(sessionId).orElse(null) ?: return null
        val eventId = session.event?.id ?: return null
        return findRegistrationByUserIdAndEventId(userId, eventId)
    }

    private fun convertToDto(checkIn: CheckIn): CheckInDto {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        return CheckInDto().apply {
            BeanUtils.copyProperties(checkIn, this)
            
            // Set registration ID
            this.registrationId = checkIn.registration?.id
            
            // Convert session if present
            checkIn.session?.let { sess ->
                this.sessionId = sess.id
                this.sessionTitle = sess.title
            }
            
            // Format timestamp
            this.checkedInAt = checkIn.checkedInAt
        }
    }
}