package com.eventr.controller

import com.eventr.dto.*
import com.eventr.service.CheckInService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(CheckInController::class)
@DisplayName("CheckInController Tests")
class CheckInControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var checkInService: CheckInService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var eventId: UUID
    private lateinit var sessionId: UUID
    private lateinit var registrationId: UUID

    @BeforeEach
    fun setUp() {
        eventId = UUID.randomUUID()
        sessionId = UUID.randomUUID()
        registrationId = UUID.randomUUID()
    }

    @Test
    @DisplayName("Should check in with QR code successfully")
    fun shouldCheckInWithQRSuccessfully() {
        // Given
        val qrCheckInDto = QRCheckInDto(
            qrCode = "test-qr-code",
            scannerInfo = "test-scanner"
        )
        
        val checkInDto = CheckInDto(
            id = UUID.randomUUID(),
            registrationId = registrationId,
            eventId = eventId,
            sessionId = sessionId,
            checkedInAt = LocalDateTime.now(),
            type = CheckInType.SESSION
        )
        
        whenever(checkInService.checkInWithQR(qrCheckInDto)).thenReturn(checkInDto)

        // When & Then
        mockMvc.perform(
            post("/api/checkin/qr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(qrCheckInDto))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.registrationId").value(registrationId.toString()))
        .andExpect(jsonPath("$.eventId").value(eventId.toString()))
        .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
    }

    @Test
    @DisplayName("Should return bad request when QR check-in fails")
    fun shouldReturnBadRequestWhenQRCheckInFails() {
        // Given
        val qrCheckInDto = QRCheckInDto(qrCode = "invalid-qr")
        
        whenever(checkInService.checkInWithQR(qrCheckInDto))
            .thenThrow(IllegalArgumentException("Invalid QR code"))

        // When & Then
        mockMvc.perform(
            post("/api/checkin/qr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(qrCheckInDto))
        )
        .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should perform manual check-in successfully")
    fun shouldPerformManualCheckInSuccessfully() {
        // Given
        val checkInCreateDto = CheckInCreateDto(
            registrationId = registrationId,
            sessionId = sessionId,
            type = CheckInType.SESSION,
            notes = "Manual check-in"
        )
        
        val checkInDto = CheckInDto(
            id = UUID.randomUUID(),
            registrationId = registrationId,
            sessionId = sessionId,
            type = CheckInType.SESSION,
            checkedInAt = LocalDateTime.now()
        )
        
        whenever(checkInService.manualCheckIn(checkInCreateDto)).thenReturn(checkInDto)

        // When & Then
        mockMvc.perform(
            post("/api/checkin/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkInCreateDto))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.registrationId").value(registrationId.toString()))
        .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
    }

    @Test
    @DisplayName("Should perform bulk check-in successfully")
    fun shouldPerformBulkCheckInSuccessfully() {
        // Given
        val bulkCheckInDto = BulkCheckInDto(
            sessionId = sessionId,
            registrationIds = listOf(registrationId),
            type = CheckInType.SESSION
        )
        
        val checkInResults = listOf(
            CheckInDto(
                id = UUID.randomUUID(),
                registrationId = registrationId,
                sessionId = sessionId,
                type = CheckInType.SESSION,
                checkedInAt = LocalDateTime.now()
            )
        )
        
        whenever(checkInService.bulkCheckIn(bulkCheckInDto)).thenReturn(checkInResults)

        // When & Then
        mockMvc.perform(
            post("/api/checkin/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkCheckInDto))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$[0].registrationId").value(registrationId.toString()))
    }

    @Test
    @DisplayName("Should get event check-in stats successfully")
    fun shouldGetEventCheckInStatsSuccessfully() {
        // Given
        val statsDto = CheckInStatsDto(
            eventId = eventId,
            totalRegistrations = 100,
            totalCheckIns = 85,
            checkInRate = 85.0,
            sessionStats = emptyList()
        )
        
        whenever(checkInService.getEventCheckInStats(eventId)).thenReturn(statsDto)

        // When & Then
        mockMvc.perform(
            get("/api/checkin/event/{eventId}/stats", eventId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.eventId").value(eventId.toString()))
        .andExpect(jsonPath("$.totalRegistrations").value(100))
        .andExpect(jsonPath("$.totalCheckIns").value(85))
        .andExpect(jsonPath("$.checkInRate").value(85.0))
    }

    @Test
    @DisplayName("Should get session attendance successfully")
    fun shouldGetSessionAttendanceSuccessfully() {
        // Given
        val attendance = listOf(
            CheckInDto(
                id = UUID.randomUUID(),
                registrationId = registrationId,
                sessionId = sessionId,
                type = CheckInType.SESSION,
                checkedInAt = LocalDateTime.now()
            )
        )
        
        whenever(checkInService.getSessionAttendance(sessionId)).thenReturn(attendance)

        // When & Then
        mockMvc.perform(
            get("/api/checkin/session/{sessionId}/attendance", sessionId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$[0].sessionId").value(sessionId.toString()))
    }

    @Test
    @DisplayName("Should get attendance report successfully")
    fun shouldGetAttendanceReportSuccessfully() {
        // Given
        val reportDto = AttendanceReportDto(
            eventId = eventId,
            reportGeneratedAt = LocalDateTime.now(),
            summary = AttendanceSummaryDto(
                totalRegistrations = 100,
                totalAttendees = 85,
                overallAttendanceRate = 85.0
            ),
            sessionReports = emptyList()
        )
        
        whenever(checkInService.getAttendanceReport(eventId)).thenReturn(reportDto)

        // When & Then
        mockMvc.perform(
            get("/api/checkin/event/{eventId}/report", eventId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.eventId").value(eventId.toString()))
        .andExpect(jsonPath("$.summary.totalRegistrations").value(100))
        .andExpect(jsonPath("$.summary.overallAttendanceRate").value(85.0))
    }

    @Test
    @DisplayName("Should sync offline check-ins successfully")
    fun shouldSyncOfflineCheckInsSuccessfully() {
        // Given
        val offlineCheckIns = listOf(
            OfflineCheckInDto(
                tempId = "temp-1",
                registrationId = registrationId,
                sessionId = sessionId,
                type = CheckInType.SESSION,
                checkedInAt = LocalDateTime.now()
            )
        )
        
        val syncResults = listOf(
            CheckInDto(
                id = UUID.randomUUID(),
                registrationId = registrationId,
                sessionId = sessionId,
                type = CheckInType.SESSION,
                checkedInAt = LocalDateTime.now()
            )
        )
        
        whenever(checkInService.syncOfflineCheckIns(offlineCheckIns)).thenReturn(syncResults)

        // When & Then
        mockMvc.perform(
            post("/api/checkin/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(offlineCheckIns))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$[0].registrationId").value(registrationId.toString()))
    }

    @Test
    @DisplayName("Should generate event QR code successfully")
    fun shouldGenerateEventQRSuccessfully() {
        // Given
        val userId = "user123"
        val qrCodeResponse = QRCodeResponseDto(
            qrCode = "test-qr-code",
            qrCodeBase64 = "base64-encoded-image",
            expiresAt = LocalDateTime.now().plusHours(1)
        )
        
        whenever(checkInService.generateEventQRCode(eventId, userId)).thenReturn(qrCodeResponse)

        // When & Then
        mockMvc.perform(
            get("/api/checkin/qr/event/{eventId}/user/{userId}", eventId, userId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.qrCode").value("test-qr-code"))
        .andExpect(jsonPath("$.qrCodeBase64").value("base64-encoded-image"))
    }

    @Test
    @DisplayName("Should generate session QR code successfully")
    fun shouldGenerateSessionQRSuccessfully() {
        // Given
        val userId = "user123"
        val qrCodeResponse = QRCodeResponseDto(
            qrCode = "session-qr-code",
            qrCodeBase64 = "base64-session-image",
            expiresAt = LocalDateTime.now().plusHours(1)
        )
        
        whenever(checkInService.generateSessionQRCode(sessionId, userId)).thenReturn(qrCodeResponse)

        // When & Then
        mockMvc.perform(
            get("/api/checkin/qr/session/{sessionId}/user/{userId}", sessionId, userId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.qrCode").value("session-qr-code"))
        .andExpect(jsonPath("$.qrCodeBase64").value("base64-session-image"))
    }

    @Test
    @DisplayName("Should generate staff event QR code successfully")
    fun shouldGenerateStaffEventQRSuccessfully() {
        // Given
        val qrCodeResponse = QRCodeResponseDto(
            qrCode = "staff-event-qr",
            qrCodeBase64 = "base64-staff-image",
            expiresAt = LocalDateTime.now().plusHours(8)
        )
        
        whenever(checkInService.generateStaffQRCode(eventId)).thenReturn(qrCodeResponse)

        // When & Then
        mockMvc.perform(
            get("/api/checkin/qr/staff/event/{eventId}", eventId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.qrCode").value("staff-event-qr"))
        .andExpect(jsonPath("$.qrCodeBase64").value("base64-staff-image"))
    }

    @Test
    @DisplayName("Should generate attendee badge successfully")
    fun shouldGenerateAttendeeBadgeSuccessfully() {
        // Given
        val userId = "user123"
        val userName = "John Doe"
        val badgeResponse = QRCodeResponseDto(
            qrCode = "badge-qr-code",
            qrCodeBase64 = "base64-badge-image",
            additionalInfo = mapOf("userName" to userName)
        )
        
        whenever(checkInService.generateAttendeeBadge(eventId, userId, userName)).thenReturn(badgeResponse)

        // When & Then
        mockMvc.perform(
            get("/api/checkin/qr/badge/event/{eventId}/user/{userId}", eventId, userId)
                .param("userName", userName)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.qrCode").value("badge-qr-code"))
        .andExpect(jsonPath("$.qrCodeBase64").value("base64-badge-image"))
        .andExpect(jsonPath("$.additionalInfo.userName").value(userName))
    }

    @Test
    @DisplayName("Should download badge image successfully")
    fun shouldDownloadBadgeImageSuccessfully() {
        // Given
        val userId = "user123"
        val userName = "John Doe"
        val base64Image = Base64.getEncoder().encodeToString("fake-image-data".toByteArray())
        val badgeResponse = QRCodeResponseDto(
            qrCode = "badge-qr-code",
            qrCodeBase64 = base64Image
        )
        
        whenever(checkInService.generateAttendeeBadge(eventId, userId, userName)).thenReturn(badgeResponse)

        // When & Then
        mockMvc.perform(
            get("/api/checkin/qr/badge/event/{eventId}/user/{userId}/image", eventId, userId)
                .param("userName", userName)
        )
        .andExpect(status().isOk)
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"badge-$userId.png\""))
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
    }

    @Test
    @DisplayName("Should download staff QR image successfully")
    fun shouldDownloadStaffQRImageSuccessfully() {
        // Given
        val base64Image = Base64.getEncoder().encodeToString("fake-staff-qr-data".toByteArray())
        val qrCodeResponse = QRCodeResponseDto(
            qrCode = "staff-qr-code",
            qrCodeBase64 = base64Image
        )
        
        whenever(checkInService.generateStaffQRCode(eventId)).thenReturn(qrCodeResponse)

        // When & Then
        mockMvc.perform(
            get("/api/checkin/qr/staff/event/{eventId}/image", eventId)
        )
        .andExpect(status().isOk)
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"staff-checkin-$eventId.png\""))
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
    }
}