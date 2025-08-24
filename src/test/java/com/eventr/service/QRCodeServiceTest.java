package com.eventr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QRCode Service Tests")
public class QRCodeServiceTest {

    private QRCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QRCodeService();
        // Set test values using reflection
        ReflectionTestUtils.setField(qrCodeService, "baseUrl", "https://test.com");
        ReflectionTestUtils.setField(qrCodeService, "qrSecret", "test-secret-key");
    }

    @Test
    @DisplayName("Should generate event check-in QR code with valid content")
    void shouldGenerateEventCheckInQRCode() {
        // Given
        String eventId = "event-123";
        String userId = "user-456";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        // When
        QRCodeService.QRCodeData qrCodeData = qrCodeService.generateEventCheckInQR(eventId, userId, expiresAt);

        // Then
        assertNotNull(qrCodeData);
        assertTrue(qrCodeData.getContent().contains("/checkin/event/" + eventId));
        assertTrue(qrCodeData.getContent().contains("user=" + userId));
        assertTrue(qrCodeData.getContent().contains("t="));
        assertTrue(qrCodeData.getContent().contains("sig="));
        assertTrue(qrCodeData.getImageBytes().length > 0);
        assertEquals(expiresAt, qrCodeData.getExpiresAt());
    }

    @Test
    @DisplayName("Should generate session check-in QR code with valid content")
    void shouldGenerateSessionCheckInQRCode() {
        // Given
        String sessionId = "session-789";
        String userId = "user-456";

        // When
        QRCodeService.QRCodeData qrCodeData = qrCodeService.generateSessionCheckInQR(sessionId, userId, null);

        // Then
        assertNotNull(qrCodeData);
        assertTrue(qrCodeData.getContent().contains("/checkin/session/" + sessionId));
        assertTrue(qrCodeData.getContent().contains("user=" + userId));
        assertTrue(qrCodeData.getContent().contains("t="));
        assertTrue(qrCodeData.getContent().contains("sig="));
        assertTrue(qrCodeData.getImageBytes().length > 0);
        assertNull(qrCodeData.getExpiresAt());
    }

    @Test
    @DisplayName("Should generate staff check-in QR code for event")
    void shouldGenerateStaffCheckInQRCodeForEvent() {
        // Given
        String eventId = "event-123";

        // When
        QRCodeService.QRCodeData qrCodeData = qrCodeService.generateStaffCheckInQR(eventId, null);

        // Then
        assertNotNull(qrCodeData);
        assertTrue(qrCodeData.getContent().contains("/staff/checkin/event/" + eventId));
        assertTrue(qrCodeData.getContent().contains("t="));
        assertTrue(qrCodeData.getContent().contains("sig="));
        assertTrue(qrCodeData.getImageBytes().length > 0);
        assertNull(qrCodeData.getExpiresAt());
    }

    @Test
    @DisplayName("Should generate staff check-in QR code for session")
    void shouldGenerateStaffCheckInQRCodeForSession() {
        // Given
        String eventId = "event-123";
        String sessionId = "session-789";

        // When
        QRCodeService.QRCodeData qrCodeData = qrCodeService.generateStaffCheckInQR(eventId, sessionId);

        // Then
        assertNotNull(qrCodeData);
        assertTrue(qrCodeData.getContent().contains("/staff/checkin/session/" + sessionId));
        assertTrue(qrCodeData.getContent().contains("t="));
        assertTrue(qrCodeData.getContent().contains("sig="));
        assertTrue(qrCodeData.getImageBytes().length > 0);
        assertNull(qrCodeData.getExpiresAt());
    }

    @Test
    @DisplayName("Should generate attendee badge QR code")
    void shouldGenerateAttendeeBadgeQRCode() {
        // Given
        String eventId = "event-123";
        String userId = "user-456";
        String userName = "John Doe";

        // When
        QRCodeService.QRCodeData qrCodeData = qrCodeService.generateAttendeeBadge(eventId, userId, userName);

        // Then
        assertNotNull(qrCodeData);
        assertTrue(qrCodeData.getContent().contains("/badge/" + eventId + "/" + userId));
        assertTrue(qrCodeData.getContent().contains("t="));
        assertTrue(qrCodeData.getContent().contains("sig="));
        assertTrue(qrCodeData.getImageBytes().length > 0);
        assertNull(qrCodeData.getExpiresAt());
    }

    @Test
    @DisplayName("Should validate QR signature correctly")
    void shouldValidateQRSignatureCorrectly() {
        // Given - First generate a QR code to get a valid signature
        String eventId = "event-123";
        String userId = "user-456";
        QRCodeService.QRCodeData qrCodeData = qrCodeService.generateEventCheckInQR(eventId, userId, null);
        
        // Extract components from the generated URL
        String url = qrCodeData.getContent();
        Pattern timestampPattern = Pattern.compile("t=(\\d+)");
        Pattern signaturePattern = Pattern.compile("sig=([^&]+)");
        
        Matcher timestampMatcher = timestampPattern.matcher(url);
        Matcher signatureMatcher = signaturePattern.matcher(url);
        
        assertTrue(timestampMatcher.find());
        assertTrue(signatureMatcher.find());
        
        String timestamp = timestampMatcher.group(1);
        String signature = signatureMatcher.group(1);

        // When
        boolean isValid = qrCodeService.validateQRSignature("event", eventId, userId, timestamp, signature);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject invalid QR signature")
    void shouldRejectInvalidQRSignature() {
        // Given
        String eventId = "event-123";
        String userId = "user-456";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String invalidSignature = "invalid-signature";

        // When
        boolean isValid = qrCodeService.validateQRSignature("event", eventId, userId, timestamp, invalidSignature);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate bulk attendee QR codes")
    void shouldGenerateBulkAttendeeQRCodes() {
        // Given
        String eventId = "event-123";
        List<kotlin.Pair<String, String>> attendees = List.of(
                new kotlin.Pair<>("user-1", "Alice Smith"),
                new kotlin.Pair<>("user-2", "Bob Johnson")
        );

        // When
        Map<String, QRCodeService.QRCodeData> qrCodes = qrCodeService.generateBulkAttendeeQRCodes(eventId, attendees);

        // Then
        assertEquals(2, qrCodes.size());
        assertTrue(qrCodes.containsKey("user-1"));
        assertTrue(qrCodes.containsKey("user-2"));
        
        qrCodes.values().forEach(qrCodeData -> {
            assertTrue(qrCodeData.getContent().contains("/badge/" + eventId + "/"));
            assertTrue(qrCodeData.getImageBytes().length > 0);
        });
    }

    @Test
    @DisplayName("Should handle empty bulk attendees list")
    void shouldHandleEmptyBulkAttendeesList() {
        // Given
        String eventId = "event-123";
        List<kotlin.Pair<String, String>> attendees = List.of();

        // When
        Map<String, QRCodeService.QRCodeData> qrCodes = qrCodeService.generateBulkAttendeeQRCodes(eventId, attendees);

        // Then
        assertTrue(qrCodes.isEmpty());
    }

    @Test
    @DisplayName("Should generate analytics QR code")
    void shouldGenerateAnalyticsQRCode() {
        // Given
        String eventId = "event-123";

        // When
        QRCodeService.QRCodeData qrCodeData = qrCodeService.generateAnalyticsQR(eventId);

        // Then
        assertNotNull(qrCodeData);
        assertTrue(qrCodeData.getContent().contains("/analytics/event/" + eventId));
        assertTrue(qrCodeData.getContent().contains("t="));
        assertTrue(qrCodeData.getContent().contains("sig="));
        assertTrue(qrCodeData.getImageBytes().length > 0);
        assertNull(qrCodeData.getExpiresAt());
    }

    @Test
    @DisplayName("Should generate session summary QR code")
    void shouldGenerateSessionSummaryQRCode() {
        // Given
        String sessionId = "session-789";

        // When
        QRCodeService.QRCodeData qrCodeData = qrCodeService.generateSessionSummaryQR(sessionId);

        // Then
        assertNotNull(qrCodeData);
        assertTrue(qrCodeData.getContent().contains("/session/" + sessionId + "/summary"));
        assertTrue(qrCodeData.getContent().contains("t="));
        assertTrue(qrCodeData.getContent().contains("sig="));
        assertTrue(qrCodeData.getImageBytes().length > 0);
        assertNull(qrCodeData.getExpiresAt());
    }

    @Test
    @DisplayName("Should generate different signatures for different payloads")
    void shouldGenerateDifferentSignaturesForDifferentPayloads() {
        // Given
        String eventId1 = "event-123";
        String eventId2 = "event-456";
        String userId = "user-789";

        // When
        QRCodeService.QRCodeData qr1 = qrCodeService.generateEventCheckInQR(eventId1, userId, null);
        QRCodeService.QRCodeData qr2 = qrCodeService.generateEventCheckInQR(eventId2, userId, null);

        // Then
        assertNotEquals(qr1.getContent(), qr2.getContent());
        
        // Extract signatures
        Pattern signaturePattern = Pattern.compile("sig=([^&]+)");
        Matcher matcher1 = signaturePattern.matcher(qr1.getContent());
        Matcher matcher2 = signaturePattern.matcher(qr2.getContent());
        
        assertTrue(matcher1.find());
        assertTrue(matcher2.find());
        
        String sig1 = matcher1.group(1);
        String sig2 = matcher2.group(1);
        
        assertNotEquals(sig1, sig2);
    }
}