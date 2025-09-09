package com.eventr.service

import com.eventr.dto.*
import java.util.*

/**
 * Service interface for QR code processing operations.
 * 
 * Focuses exclusively on QR code generation and validation:
 * - QR code generation for events and sessions
 * - QR code parsing and validation
 * - Staff and attendee badge generation
 * 
 * Follows Single Responsibility Principle by handling only QR code concerns.
 */
interface QRCodeProcessingService {

    /**
     * Process QR code check-in request.
     * 
     * @param qrCheckInDto QR check-in data including QR content
     * @return Check-in result
     * @throws IllegalArgumentException if QR code is invalid or expired
     */
    fun processQRCheckIn(qrCheckInDto: QRCheckInDto): CheckInDto

    /**
     * Generate QR code for event check-in.
     * 
     * @param eventId Event ID
     * @param userId User ID for personalized QR code
     * @return QR code response with encoded data
     */
    fun generateEventQRCode(eventId: UUID, userId: String): QRCodeResponseDto

    /**
     * Generate QR code for session check-in.
     * 
     * @param sessionId Session ID
     * @param userId User ID for personalized QR code
     * @return QR code response with encoded data
     */
    fun generateSessionQRCode(sessionId: UUID, userId: String): QRCodeResponseDto

    /**
     * Generate staff QR code for event or session management.
     * 
     * @param eventId Event ID
     * @param sessionId Optional session ID for session-specific staff code
     * @return Staff QR code response
     */
    fun generateStaffQRCode(eventId: UUID, sessionId: UUID? = null): QRCodeResponseDto

    /**
     * Generate attendee badge with QR code.
     * 
     * @param eventId Event ID
     * @param userId User ID
     * @param userName User display name
     * @return Badge QR code response
     */
    fun generateAttendeeBadge(eventId: UUID, userId: String, userName: String): QRCodeResponseDto

    /**
     * Parse and validate QR code content.
     * 
     * @param qrContent Raw QR code content
     * @return Parsed QR data
     * @throws IllegalArgumentException if QR format is invalid
     */
    fun parseQRCode(qrContent: String): QRData

    /**
     * Validate QR code signature and timestamp.
     * 
     * @param qrData Parsed QR data
     * @return true if QR code is valid and not expired
     */
    fun validateQRCode(qrData: QRData): Boolean
}

/**
 * QR code data structure for parsed QR content.
 */
data class QRData(
    val type: String,           // "event", "session", "staff"
    val identifier: UUID,       // Event or session ID
    val userId: String,         // User ID
    val timestamp: Long,        // Generation timestamp
    val signature: String       // Security signature
)