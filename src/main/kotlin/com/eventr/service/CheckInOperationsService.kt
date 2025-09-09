package com.eventr.service

import com.eventr.dto.*
import java.util.*

/**
 * Service interface for check-in operations.
 * 
 * Focuses exclusively on check-in business logic:
 * - Manual check-in processing
 * - Bulk check-in operations
 * - Event and session check-in workflows
 * - Check-in validation and duplicate prevention
 * 
 * Follows Single Responsibility Principle by handling only check-in operations.
 */
interface CheckInOperationsService {

    /**
     * Perform manual check-in for a registration.
     * 
     * @param createDto Check-in creation data
     * @return Created check-in record
     * @throws IllegalArgumentException if registration not found or already checked in
     */
    fun manualCheckIn(createDto: CheckInCreateDto): CheckInDto

    /**
     * Perform bulk check-in for multiple registrations.
     * 
     * @param bulkDto Bulk check-in data
     * @return List of created check-in records (successful ones only)
     */
    fun bulkCheckIn(bulkDto: BulkCheckInDto): List<CheckInDto>

    /**
     * Check in user to specific event.
     * 
     * @param eventId Event ID
     * @param userId User ID
     * @param checkInDetails Check-in details
     * @return Check-in record
     * @throws IllegalArgumentException if user not registered or already checked in
     */
    fun checkInToEvent(eventId: UUID, userId: String, checkInDetails: CheckInDetailsDto): CheckInDto

    /**
     * Check in user to specific session.
     * 
     * @param sessionId Session ID
     * @param userId User ID
     * @param checkInDetails Check-in details
     * @return Check-in record
     * @throws IllegalArgumentException if user not registered or already checked in
     */
    fun checkInToSession(sessionId: UUID, userId: String, checkInDetails: CheckInDetailsDto): CheckInDto

    /**
     * Validate if user can check in to event or session.
     * 
     * @param registrationId Registration ID
     * @param sessionId Optional session ID
     * @param type Check-in type
     * @return true if check-in is allowed
     */
    fun canCheckIn(registrationId: UUID, sessionId: UUID?, type: CheckInType): Boolean

    /**
     * Update registration status after check-in.
     * 
     * @param checkIn Check-in record
     */
    fun updateRegistrationStatus(checkIn: CheckInDto)
}

/**
 * Check-in details for operations.
 */
data class CheckInDetailsDto(
    val method: CheckInMethod,
    val checkedInBy: String? = null,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val verificationCode: String? = null,
    val notes: String? = null,
    val metadata: Map<String, Any>? = null
)