package com.eventr.service.interfaces

import com.eventr.dto.*
import java.util.UUID

/**
 * Interface for check-in operations following Interface Segregation Principle
 */
interface CheckInServiceInterface {
    
    /**
     * Check in using QR code
     */
    fun checkInWithQR(qrCheckInDto: QRCheckInDto): CheckInDto
    
    /**
     * Manual check-in
     */
    fun manualCheckIn(createDto: CheckInCreateDto): CheckInDto
    
    /**
     * Bulk check-in for multiple registrations
     */
    fun bulkCheckIn(registrationIds: List<UUID>, sessionId: UUID?): List<CheckInDto>
    
    /**
     * Get check-in by ID
     */
    fun getCheckInById(checkInId: UUID): CheckInDto?
    
    /**
     * Get all check-ins for a registration
     */
    fun getCheckInsForRegistration(registrationId: UUID): List<CheckInDto>
    
    /**
     * Get all check-ins for a session
     */
    fun getCheckInsForSession(sessionId: UUID): List<CheckInDto>
    
    /**
     * Get check-in statistics for an event
     */
    fun getCheckInStatistics(eventId: UUID): CheckInStatistics
}

/**
 * Data class for check-in statistics
 */
data class CheckInStatistics(
    val eventId: UUID,
    val eventName: String,
    val totalRegistrations: Long,
    val totalCheckIns: Long,
    val uniqueCheckIns: Long,
    val checkInRate: Double,
    val averageCheckInTime: Double?
)