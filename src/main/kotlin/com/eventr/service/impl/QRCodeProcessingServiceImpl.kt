package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import com.eventr.service.QRCodeProcessingService
import com.eventr.service.QRCodeService
import com.eventr.service.CheckInOperationsService
import com.eventr.service.QRData
import org.springframework.stereotype.Service
import java.net.URLDecoder
import java.util.*

/**
 * Implementation of QRCodeProcessingService focused on QR code operations.
 * 
 * Responsibilities:
 * - QR code generation for events and sessions
 * - QR code parsing and validation
 * - Staff and attendee badge generation
 * - QR security validation
 */
@Service
class QRCodeProcessingServiceImpl(
    private val qrCodeService: QRCodeService,
    private val checkInOperationsService: CheckInOperationsService,
    private val eventRepository: EventRepository,
    private val sessionRepository: SessionRepository
) : QRCodeProcessingService {

    override fun processQRCheckIn(qrCheckInDto: QRCheckInDto): CheckInDto {
        val qrContent = URLDecoder.decode(qrCheckInDto.qrCode, "UTF-8")
        
        // Parse QR code content
        val qrData = parseQRCode(qrContent)
        
        // Validate QR code
        if (!validateQRCode(qrData)) {
            throw IllegalArgumentException("Invalid or expired QR code")
        }
        
        // Convert to check-in details
        val checkInDetails = CheckInDetailsDto(
            method = CheckInMethod.QR_CODE,
            checkedInBy = qrCheckInDto.checkedInBy,
            deviceId = qrCheckInDto.deviceId,
            deviceName = qrCheckInDto.deviceName,
            ipAddress = qrCheckInDto.ipAddress,
            userAgent = qrCheckInDto.userAgent,
            location = qrCheckInDto.location,
            notes = qrCheckInDto.notes
        )
        
        return when (qrData.type) {
            "event" -> checkInOperationsService.checkInToEvent(qrData.identifier, qrData.userId, checkInDetails)
            "session" -> checkInOperationsService.checkInToSession(qrData.identifier, qrData.userId, checkInDetails)
            else -> throw IllegalArgumentException("Invalid QR code type: ${qrData.type}")
        }
    }

    override fun generateEventQRCode(eventId: UUID, userId: String): QRCodeResponseDto {
        // Verify event exists
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found: $eventId") }
        
        // Generate QR code content
        val timestamp = System.currentTimeMillis()
        val signature = qrCodeService.generateSignature("event", eventId.toString(), userId, timestamp)
        
        val qrContent = buildQRContent(
            type = "event",
            identifier = eventId.toString(),
            userId = userId,
            timestamp = timestamp,
            signature = signature
        )
        
        val qrCodeImage = qrCodeService.generateQRCode(qrContent)
        
        return QRCodeResponseDto(
            qrCode = qrContent,
            qrCodeImage = qrCodeImage,
            eventId = eventId,
            eventName = event.name,
            userId = userId,
            validUntil = calculateExpirationTime(timestamp),
            type = "event"
        )
    }

    override fun generateSessionQRCode(sessionId: UUID, userId: String): QRCodeResponseDto {
        // Verify session exists
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found: $sessionId") }
        
        val event = session.event ?: throw IllegalArgumentException("Session has no associated event")
        
        // Generate QR code content
        val timestamp = System.currentTimeMillis()
        val signature = qrCodeService.generateSignature("session", sessionId.toString(), userId, timestamp)
        
        val qrContent = buildQRContent(
            type = "session",
            identifier = sessionId.toString(),
            userId = userId,
            timestamp = timestamp,
            signature = signature
        )
        
        val qrCodeImage = qrCodeService.generateQRCode(qrContent)
        
        return QRCodeResponseDto(
            qrCode = qrContent,
            qrCodeImage = qrCodeImage,
            eventId = event.id,
            eventName = event.name,
            sessionId = sessionId,
            sessionName = session.name,
            userId = userId,
            validUntil = calculateExpirationTime(timestamp),
            type = "session"
        )
    }

    override fun generateStaffQRCode(eventId: UUID, sessionId: UUID?): QRCodeResponseDto {
        // Verify event exists
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found: $eventId") }
        
        val timestamp = System.currentTimeMillis()
        val identifier = sessionId?.toString() ?: eventId.toString()
        val type = if (sessionId != null) "staff_session" else "staff_event"
        
        val signature = qrCodeService.generateSignature(type, identifier, "staff", timestamp)
        
        val qrContent = buildQRContent(
            type = type,
            identifier = identifier,
            userId = "staff",
            timestamp = timestamp,
            signature = signature
        )
        
        val qrCodeImage = qrCodeService.generateQRCode(qrContent)
        
        return QRCodeResponseDto(
            qrCode = qrContent,
            qrCodeImage = qrCodeImage,
            eventId = eventId,
            eventName = event.name,
            sessionId = sessionId,
            userId = "staff",
            validUntil = calculateExpirationTime(timestamp),
            type = type
        )
    }

    override fun generateAttendeeBadge(eventId: UUID, userId: String, userName: String): QRCodeResponseDto {
        // Verify event exists
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found: $eventId") }
        
        val timestamp = System.currentTimeMillis()
        val signature = qrCodeService.generateSignature("badge", eventId.toString(), userId, timestamp)
        
        val qrContent = buildQRContent(
            type = "badge",
            identifier = eventId.toString(),
            userId = userId,
            timestamp = timestamp,
            signature = signature,
            additionalData = mapOf("userName" to userName)
        )
        
        val qrCodeImage = qrCodeService.generateQRCode(qrContent)
        
        return QRCodeResponseDto(
            qrCode = qrContent,
            qrCodeImage = qrCodeImage,
            eventId = eventId,
            eventName = event.name,
            userId = userId,
            userName = userName,
            validUntil = calculateExpirationTime(timestamp),
            type = "badge"
        )
    }

    override fun parseQRCode(qrContent: String): QRData {
        try {
            // Expected format: type|identifier|userId|timestamp|signature
            val parts = qrContent.split("|")
            if (parts.size < 5) {
                throw IllegalArgumentException("Invalid QR code format")
            }
            
            return QRData(
                type = parts[0],
                identifier = UUID.fromString(parts[1]),
                userId = parts[2],
                timestamp = parts[3].toLong(),
                signature = parts[4]
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse QR code: ${e.message}")
        }
    }

    override fun validateQRCode(qrData: QRData): Boolean {
        try {
            // Validate signature
            val isSignatureValid = qrCodeService.validateQRSignature(
                qrData.type,
                qrData.identifier.toString(),
                qrData.userId,
                qrData.timestamp,
                qrData.signature
            )
            
            if (!isSignatureValid) {
                return false
            }
            
            // Check if QR code has expired (24 hours)
            val currentTime = System.currentTimeMillis()
            val expirationTime = qrData.timestamp + (24 * 60 * 60 * 1000) // 24 hours
            
            return currentTime <= expirationTime
            
        } catch (e: Exception) {
            return false
        }
    }

    private fun buildQRContent(
        type: String,
        identifier: String,
        userId: String,
        timestamp: Long,
        signature: String,
        additionalData: Map<String, String> = emptyMap()
    ): String {
        val baseContent = "$type|$identifier|$userId|$timestamp|$signature"
        
        return if (additionalData.isNotEmpty()) {
            val additional = additionalData.entries.joinToString(",") { "${it.key}:${it.value}" }
            "$baseContent|$additional"
        } else {
            baseContent
        }
    }

    private fun calculateExpirationTime(timestamp: Long): String {
        val expirationTime = timestamp + (24 * 60 * 60 * 1000) // 24 hours
        return java.time.Instant.ofEpochMilli(expirationTime).toString()
    }
}