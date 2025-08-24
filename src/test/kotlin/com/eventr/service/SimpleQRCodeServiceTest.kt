package com.eventr.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

@DisplayName("QRCode Service Tests")
class SimpleQRCodeServiceTest {

    private lateinit var qrCodeService: QRCodeService

    @BeforeEach
    fun setup() {
        qrCodeService = QRCodeService()
        // Set test values using reflection
        ReflectionTestUtils.setField(qrCodeService, "baseUrl", "https://test.com")
        ReflectionTestUtils.setField(qrCodeService, "qrSecret", "test-secret-key")
    }

    @Test
    @DisplayName("Should generate event check-in QR code with valid content")
    fun shouldGenerateEventCheckInQRCode() {
        // Given
        val eventId = "event-123"
        val userId = "user-456"
        val expiresAt = LocalDateTime.now().plusHours(24)

        // When
        val qrCodeData = qrCodeService.generateEventCheckInQR(eventId, userId, expiresAt)

        // Then
        assertNotNull(qrCodeData)
        assertTrue(qrCodeData.content.contains("/checkin/event/$eventId"))
        assertTrue(qrCodeData.content.contains("user=$userId"))
        assertTrue(qrCodeData.content.contains("t="))
        assertTrue(qrCodeData.content.contains("sig="))
        assertTrue(qrCodeData.imageBytes.isNotEmpty())
        assertEquals(expiresAt, qrCodeData.expiresAt)
    }

    @Test
    @DisplayName("Should generate session check-in QR code with valid content")
    fun shouldGenerateSessionCheckInQRCode() {
        // Given
        val sessionId = "session-789"
        val userId = "user-456"

        // When
        val qrCodeData = qrCodeService.generateSessionCheckInQR(sessionId, userId)

        // Then
        assertNotNull(qrCodeData)
        assertTrue(qrCodeData.content.contains("/checkin/session/$sessionId"))
        assertTrue(qrCodeData.content.contains("user=$userId"))
        assertTrue(qrCodeData.content.contains("t="))
        assertTrue(qrCodeData.content.contains("sig="))
        assertTrue(qrCodeData.imageBytes.isNotEmpty())
        assertNull(qrCodeData.expiresAt)
    }

    @Test
    @DisplayName("Should generate staff check-in QR code for event")
    fun shouldGenerateStaffCheckInQRCodeForEvent() {
        // Given
        val eventId = "event-123"

        // When
        val qrCodeData = qrCodeService.generateStaffCheckInQR(eventId)

        // Then
        assertNotNull(qrCodeData)
        assertTrue(qrCodeData.content.contains("/staff/checkin/event/$eventId"))
        assertTrue(qrCodeData.content.contains("t="))
        assertTrue(qrCodeData.content.contains("sig="))
        assertTrue(qrCodeData.imageBytes.isNotEmpty())
        assertNull(qrCodeData.expiresAt)
    }

    @Test
    @DisplayName("Should generate attendee badge QR code")
    fun shouldGenerateAttendeeBadgeQRCode() {
        // Given
        val eventId = "event-123"
        val userId = "user-456"
        val userName = "John Doe"

        // When
        val qrCodeData = qrCodeService.generateAttendeeBadge(eventId, userId, userName)

        // Then
        assertNotNull(qrCodeData)
        assertTrue(qrCodeData.content.contains("/badge/$eventId/$userId"))
        assertTrue(qrCodeData.content.contains("t="))
        assertTrue(qrCodeData.content.contains("sig="))
        assertTrue(qrCodeData.imageBytes.isNotEmpty())
        assertNull(qrCodeData.expiresAt)
    }

    @Test
    @DisplayName("Should validate QR signature correctly")
    fun shouldValidateQRSignatureCorrectly() {
        // Given - First generate a QR code to get a valid signature
        val eventId = "event-123"
        val userId = "user-456"
        val qrCodeData = qrCodeService.generateEventCheckInQR(eventId, userId)
        
        // Extract components from the generated URL
        val url = qrCodeData.content
        val timestampMatch = Regex("t=(\\d+)").find(url)
        val signatureMatch = Regex("sig=([^&]+)").find(url)
        
        assertNotNull(timestampMatch)
        assertNotNull(signatureMatch)
        
        val timestamp = timestampMatch!!.groupValues[1]
        val signature = signatureMatch!!.groupValues[1]

        // When
        val isValid = qrCodeService.validateQRSignature("event", eventId, userId, timestamp, signature)

        // Then
        assertTrue(isValid)
    }

    @Test
    @DisplayName("Should reject invalid QR signature")
    fun shouldRejectInvalidQRSignature() {
        // Given
        val eventId = "event-123"
        val userId = "user-456"
        val timestamp = System.currentTimeMillis().toString()
        val invalidSignature = "invalid-signature"

        // When
        val isValid = qrCodeService.validateQRSignature("event", eventId, userId, timestamp, invalidSignature)

        // Then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("Should generate bulk attendee QR codes")
    fun shouldGenerateBulkAttendeeQRCodes() {
        // Given
        val eventId = "event-123"
        val attendees = listOf(
            "user-1" to "Alice Smith",
            "user-2" to "Bob Johnson"
        )

        // When
        val qrCodes = qrCodeService.generateBulkAttendeeQRCodes(eventId, attendees)

        // Then
        assertEquals(2, qrCodes.size)
        assertTrue(qrCodes.containsKey("user-1"))
        assertTrue(qrCodes.containsKey("user-2"))
        
        qrCodes.values.forEach { qrCodeData ->
            assertTrue(qrCodeData.content.contains("/badge/$eventId/"))
            assertTrue(qrCodeData.imageBytes.isNotEmpty())
        }
    }

    @Test
    @DisplayName("Should handle empty bulk attendees list")
    fun shouldHandleEmptyBulkAttendeesList() {
        // Given
        val eventId = "event-123"
        val attendees = emptyList<Pair<String, String>>()

        // When
        val qrCodes = qrCodeService.generateBulkAttendeeQRCodes(eventId, attendees)

        // Then
        assertTrue(qrCodes.isEmpty())
    }

    @Test
    @DisplayName("Should generate different signatures for different payloads")
    fun shouldGenerateDifferentSignaturesForDifferentPayloads() {
        // Given
        val eventId1 = "event-123"
        val eventId2 = "event-456"
        val userId = "user-789"

        // When
        val qr1 = qrCodeService.generateEventCheckInQR(eventId1, userId)
        val qr2 = qrCodeService.generateEventCheckInQR(eventId2, userId)

        // Then
        assertNotEquals(qr1.content, qr2.content)
        
        // Extract signatures
        val sig1 = Regex("sig=([^&]+)").find(qr1.content)?.groupValues?.get(1)
        val sig2 = Regex("sig=([^&]+)").find(qr2.content)?.groupValues?.get(1)
        
        assertNotNull(sig1)
        assertNotNull(sig2)
        assertNotEquals(sig1, sig2)
    }
}