package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.events.*
import com.eventr.model.*
import com.eventr.repository.*
import com.eventr.service.QRCodeService
import com.eventr.service.interfaces.CheckInServiceInterface
import com.eventr.service.interfaces.CheckInStatistics
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Event-driven implementation of CheckInService following SOLID principles
 * - Single Responsibility: Handles only check-in operations
 * - Open/Closed: Extensible via interfaces
 * - Liskov Substitution: Implements CheckInServiceInterface
 * - Interface Segregation: Uses focused interfaces
 * - Dependency Inversion: Depends on abstractions (EventPublisher, repositories)
 */
@Service("eventDrivenCheckInService")
@Transactional
class EventDrivenCheckInService(
    private val checkInRepository: CheckInRepository,
    private val registrationRepository: RegistrationRepository,
    private val sessionRepository: SessionRepository,
    private val eventRepository: EventRepository,
    private val qrCodeService: QRCodeService,
    private val eventPublisher: EventPublisher
) : CheckInServiceInterface {
    
    private val logger = LoggerFactory.getLogger(EventDrivenCheckInService::class.java)
    
    override fun checkInWithQR(qrCheckInDto: QRCheckInDto): CheckInDto {
        logger.info("Processing QR check-in")
        
        val qrContent = URLDecoder.decode(qrCheckInDto.qrCode, "UTF-8")
        val qrData = parseQRCode(qrContent)
        
        // Validate QR code signature
        if (!qrCodeService.validateQRSignature(
                qrData.type, 
                qrData.identifier, 
                qrData.userId, 
                qrData.timestamp, 
                qrData.signature
            )) {
            throw IllegalArgumentException("Invalid or expired QR code")
        }
        
        val checkInDto = when (qrData.type) {
            "event" -> checkInToEvent(qrData, qrCheckInDto)
            "session" -> checkInToSession(qrData, qrCheckInDto)
            else -> throw IllegalArgumentException("Invalid QR code type: ${qrData.type}")
        }
        
        // Publish domain event after successful check-in
        publishCheckInEvent(checkInDto, CheckInMethod.QR_CODE)
        
        logger.info("Successfully processed QR check-in")
        return checkInDto
    }
    
    override fun manualCheckIn(createDto: CheckInCreateDto): CheckInDto {
        logger.info("Processing manual check-in for registration: {}", createDto.registrationId)
        
        val registration = registrationRepository.findById(createDto.registrationId)
            .orElseThrow { IllegalArgumentException("Registration not found: ${createDto.registrationId}") }
        
        // Validate no duplicate check-in
        validateNoDuplicateCheckIn(createDto)
        
        val checkIn = createCheckIn(registration, createDto)
        val saved = checkInRepository.save(checkIn)
        val checkInDto = saved.toDto()
        
        // Publish domain event
        publishCheckInEvent(checkInDto, createDto.method)
        
        logger.info("Successfully processed manual check-in for registration: {}", createDto.registrationId)
        return checkInDto
    }
    
    override fun bulkCheckIn(registrationIds: List<UUID>, sessionId: UUID?): List<CheckInDto> {
        logger.info("Processing bulk check-in for {} registrations", registrationIds.size)
        
        val results = mutableListOf<CheckInDto>()
        @Suppress("UNUSED_VARIABLE")
        val session = sessionId?.let { sessionRepository.findById(it).orElse(null) }
        
        registrationIds.forEach { registrationId ->
            try {
                val registration = registrationRepository.findById(registrationId)
                    .orElseThrow { IllegalArgumentException("Registration not found: $registrationId") }
                
                val createDto = CheckInCreateDto(
                    registrationId = registrationId,
                    sessionId = sessionId,
                    type = if (sessionId != null) CheckInType.SESSION else CheckInType.EVENT,
                    method = CheckInMethod.BULK,
                    checkedInBy = "SYSTEM"
                )
                
                // Check for duplicates
                val existingCheckIn = findExistingCheckIn(createDto)
                if (existingCheckIn != null) {
                    logger.warn("Skipping duplicate check-in for registration: {}", registrationId)
                    return@forEach
                }
                
                val checkIn = createCheckIn(registration, createDto)
                val saved = checkInRepository.save(checkIn)
                val checkInDto = saved.toDto()
                
                results.add(checkInDto)
                
                // Publish domain event
                publishCheckInEvent(checkInDto, CheckInMethod.BULK)
                
            } catch (e: Exception) {
                logger.error("Failed to check in registration: {}", registrationId, e)
                // Continue with other registrations
            }
        }
        
        logger.info("Successfully processed bulk check-in: {}/{} registrations", 
            results.size, registrationIds.size)
        return results
    }
    
    @Transactional(readOnly = true)
    override fun getCheckInById(checkInId: UUID): CheckInDto? {
        return checkInRepository.findById(checkInId)
            .map { it.toDto() }
            .orElse(null)
    }
    
    @Transactional(readOnly = true)
    override fun getCheckInsForRegistration(registrationId: UUID): List<CheckInDto> {
        return checkInRepository.findByRegistrationIdOrderByCheckedInAtDesc(registrationId)
            .map { it.toDto() }
    }
    
    @Transactional(readOnly = true)
    override fun getCheckInsForSession(sessionId: UUID): List<CheckInDto> {
        return checkInRepository.findBySessionIdOrderByCheckedInAtDesc(sessionId)
            .map { it.toDto() }
    }
    
    @Transactional(readOnly = true)
    override fun getCheckInStatistics(eventId: UUID): CheckInStatistics {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found: $eventId") }
        
        // Get all registrations for the event
        val registrations = registrationRepository.findByEventId(eventId)
        val totalRegistrations = registrations.size
        
        // Get all check-ins for the event
        val checkIns = checkInRepository.findByEventIdOrderByCheckedInAtDesc(eventId)
        val totalCheckIns = checkIns.size
        val uniqueCheckIns = checkIns.map { it.registration?.id }.distinct().size
        
        // Calculate check-in rate
        val checkInRate = if (totalRegistrations > 0) {
            (uniqueCheckIns.toDouble() / totalRegistrations.toDouble()) * 100
        } else {
            0.0
        }
        
        return CheckInStatistics(
            eventId = eventId,
            eventName = event.name ?: "",
            totalRegistrations = totalRegistrations.toLong(),
            totalCheckIns = totalCheckIns.toLong(),
            uniqueCheckIns = uniqueCheckIns.toLong(),
            checkInRate = checkInRate,
            averageCheckInTime = calculateAverageCheckInTime(checkIns)
        )
    }
    
    // Private helper methods
    
    private fun validateNoDuplicateCheckIn(createDto: CheckInCreateDto) {
        val existingCheckIn = findExistingCheckIn(createDto)
        if (existingCheckIn != null) {
            throw IllegalArgumentException("Already checked in")
        }
    }
    
    private fun findExistingCheckIn(createDto: CheckInCreateDto): CheckIn? {
        return if (createDto.sessionId != null) {
            checkInRepository.findByRegistrationIdAndSessionId(createDto.registrationId, createDto.sessionId)
        } else {
            checkInRepository.findByRegistrationIdAndType(createDto.registrationId, createDto.type)
        }
    }
    
    private fun createCheckIn(registration: Registration, createDto: CheckInCreateDto): CheckIn {
        return CheckIn().apply {
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
            this.checkedInAt = LocalDateTime.now()
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }
    }
    
    private fun publishCheckInEvent(checkInDto: CheckInDto, method: CheckInMethod) {
        try {
            val event = UserCheckedInEvent(
                aggregateId = checkInDto.id!!,
                registrationId = checkInDto.registrationId!!,
                relatedEventId = getEventIdFromRegistration(checkInDto.registrationId!!),
                sessionId = checkInDto.sessionId,
                userEmail = checkInDto.userEmail ?: "",
                checkInMethod = method.toString(),
                location = checkInDto.location
            )
            
            eventPublisher.publish(event)
            logger.debug("Published UserCheckedInEvent for check-in: {}", checkInDto.id)
            
        } catch (e: Exception) {
            logger.error("Failed to publish check-in event for: {}", checkInDto.id, e)
            // Don't fail the check-in if event publishing fails
        }
    }
    
    private fun getEventIdFromRegistration(registrationId: UUID): UUID {
        val registration = registrationRepository.findById(registrationId).orElse(null)
        return registration?.eventInstance?.event?.id 
            ?: throw IllegalStateException("Cannot determine event ID for registration: $registrationId")
    }
    
    private fun calculateAverageCheckInTime(checkIns: List<CheckIn>): Double? {
        if (checkIns.isEmpty()) return null
        
        val totalMinutes = checkIns.mapNotNull { checkIn ->
            checkIn.createdAt.until(checkIn.checkedInAt, ChronoUnit.MINUTES)
        }.sum()
        
        return totalMinutes.toDouble() / checkIns.size
    }
    
    // Placeholder methods for QR code processing - these would need to be implemented
    // based on the existing CheckInService implementation
    
    private fun parseQRCode(@Suppress("UNUSED_PARAMETER") qrContent: String): QRData {
        // This is a placeholder - implement based on existing logic
        return QRData("event", UUID.randomUUID().toString(), "", "", "")
    }
    
    private fun checkInToEvent(@Suppress("UNUSED_PARAMETER") qrData: QRData, @Suppress("UNUSED_PARAMETER") qrCheckInDto: QRCheckInDto): CheckInDto {
        // Placeholder - implement based on existing logic
        throw NotImplementedError("QR check-in implementation needed")
    }
    
    private fun checkInToSession(@Suppress("UNUSED_PARAMETER") qrData: QRData, @Suppress("UNUSED_PARAMETER") qrCheckInDto: QRCheckInDto): CheckInDto {
        // Placeholder - implement based on existing logic  
        throw NotImplementedError("QR check-in implementation needed")
    }
}

// Extension function to convert CheckIn entity to DTO
private fun CheckIn.toDto(): CheckInDto {
    val dto = CheckInDto()
    BeanUtils.copyProperties(this, dto)
    dto.registrationId = this.registration?.id
    dto.sessionId = this.session?.id
    dto.userEmail = this.registration?.userEmail
    dto.userName = this.registration?.userName
    dto.eventName = this.registration?.eventInstance?.event?.name
    dto.sessionTitle = this.session?.title
    return dto
}

// Data classes
data class QRData(
    val type: String,
    val identifier: String,
    val userId: String,
    val timestamp: String,
    val signature: String
)

