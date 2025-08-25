package com.eventr.controller

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.service.CapacityManagementService
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
import java.util.*

@WebMvcTest(CapacityManagementController::class)
@DisplayName("CapacityManagementController Tests")
class CapacityManagementControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var capacityManagementService: CapacityManagementService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var sessionId: UUID
    private lateinit var eventId: UUID

    @BeforeEach
    fun setUp() {
        sessionId = UUID.randomUUID()
        eventId = UUID.randomUUID()
    }

    @Test
    @DisplayName("Should create session capacity successfully")
    fun shouldCreateSessionCapacitySuccessfully() {
        // Given
        val capacityDto = SessionCapacityDto(
            sessionId = sessionId,
            maximumCapacity = 100,
            minimumCapacity = 20,
            enableWaitlist = true
        )
        
        whenever(capacityManagementService.createSessionCapacity(sessionId, capacityDto)).thenReturn(capacityDto)

        // When & Then
        mockMvc.perform(
            post("/api/capacity/sessions/{sessionId}", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(capacityDto))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
        .andExpect(jsonPath("$.maximumCapacity").value(100))
        .andExpect(jsonPath("$.minimumCapacity").value(20))
        .andExpect(jsonPath("$.enableWaitlist").value(true))
    }

    @Test
    @DisplayName("Should return bad request when creating session capacity fails")
    fun shouldReturnBadRequestWhenCreatingSessionCapacityFails() {
        // Given
        val capacityDto = SessionCapacityDto(sessionId = sessionId)
        
        whenever(capacityManagementService.createSessionCapacity(sessionId, capacityDto))
            .thenThrow(IllegalArgumentException("Session not found"))

        // When & Then
        mockMvc.perform(
            post("/api/capacity/sessions/{sessionId}", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(capacityDto))
        )
        .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should update session capacity successfully")
    fun shouldUpdateSessionCapacitySuccessfully() {
        // Given
        val updateDto = CapacityUpdateDto(
            sessionId = sessionId,
            maximumCapacity = 150,
            minimumCapacity = 30,
            reason = "Venue change"
        )
        
        val updatedCapacity = SessionCapacityDto(
            sessionId = sessionId,
            maximumCapacity = 150,
            minimumCapacity = 30
        )
        
        whenever(capacityManagementService.updateSessionCapacity(sessionId, updateDto)).thenReturn(updatedCapacity)

        // When & Then
        mockMvc.perform(
            put("/api/capacity/sessions/{sessionId}", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
        .andExpect(jsonPath("$.maximumCapacity").value(150))
        .andExpect(jsonPath("$.minimumCapacity").value(30))
    }

    @Test
    @DisplayName("Should return not found when updating non-existent session capacity")
    fun shouldReturnNotFoundWhenUpdatingNonExistentSessionCapacity() {
        // Given
        val updateDto = CapacityUpdateDto(sessionId = sessionId, maximumCapacity = 150)
        
        whenever(capacityManagementService.updateSessionCapacity(sessionId, updateDto))
            .thenThrow(IllegalArgumentException("Session capacity not found"))

        // When & Then
        mockMvc.perform(
            put("/api/capacity/sessions/{sessionId}", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
        .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("Should get session capacity successfully")
    fun shouldGetSessionCapacitySuccessfully() {
        // Given
        val capacityDto = SessionCapacityDto(
            sessionId = sessionId,
            sessionTitle = "Test Session",
            maximumCapacity = 100,
            currentRegistrations = 50,
            availableSpots = 50,
            utilizationPercentage = 50.0
        )
        
        whenever(capacityManagementService.getSessionCapacity(sessionId)).thenReturn(capacityDto)

        // When & Then
        mockMvc.perform(
            get("/api/capacity/sessions/{sessionId}", sessionId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
        .andExpect(jsonPath("$.sessionTitle").value("Test Session"))
        .andExpect(jsonPath("$.maximumCapacity").value(100))
        .andExpect(jsonPath("$.currentRegistrations").value(50))
        .andExpect(jsonPath("$.utilizationPercentage").value(50.0))
    }

    @Test
    @DisplayName("Should return not found when session capacity does not exist")
    fun shouldReturnNotFoundWhenSessionCapacityDoesNotExist() {
        // Given
        whenever(capacityManagementService.getSessionCapacity(sessionId))
            .thenThrow(IllegalArgumentException("Session capacity not found"))

        // When & Then
        mockMvc.perform(
            get("/api/capacity/sessions/{sessionId}", sessionId)
        )
        .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("Should get event capacity analytics successfully")
    fun shouldGetEventCapacityAnalyticsSuccessfully() {
        // Given
        val analyticsDto = CapacityAnalyticsDto(
            eventId = eventId,
            eventName = "Test Event",
            totalSessions = 5,
            averageUtilization = 75.0,
            fullSessionsCount = 2,
            underCapacitySessionsCount = 1,
            totalWaitlistCount = 20
        )
        
        whenever(capacityManagementService.getEventCapacityAnalytics(eventId)).thenReturn(analyticsDto)

        // When & Then
        mockMvc.perform(
            get("/api/capacity/events/{eventId}/analytics", eventId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.eventId").value(eventId.toString()))
        .andExpect(jsonPath("$.eventName").value("Test Event"))
        .andExpect(jsonPath("$.totalSessions").value(5))
        .andExpect(jsonPath("$.averageUtilization").value(75.0))
        .andExpect(jsonPath("$.fullSessionsCount").value(2))
        .andExpect(jsonPath("$.totalWaitlistCount").value(20))
    }

    @Test
    @DisplayName("Should promote from waitlist successfully")
    fun shouldPromoteFromWaitlistSuccessfully() {
        // Given
        val registrationId = UUID.randomUUID()
        val promotionDto = WaitlistPromotionDto(
            sessionId = sessionId,
            registrationIds = listOf(registrationId),
            promotionReason = "Space available",
            notifyAttendees = true
        )
        
        val promotedRegistration = SessionRegistrationDto().apply {
            this.sessionId = sessionId
            this.registrationId = registrationId
            this.status = SessionRegistrationStatus.REGISTERED
        }
        
        whenever(capacityManagementService.promoteFromWaitlist(promotionDto))
            .thenReturn(listOf(promotedRegistration))

        // When & Then
        mockMvc.perform(
            post("/api/capacity/waitlist/promote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(promotionDto))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$[0].sessionId").value(sessionId.toString()))
        .andExpect(jsonPath("$[0].registrationId").value(registrationId.toString()))
    }

    @Test
    @DisplayName("Should return bad request when waitlist promotion fails")
    fun shouldReturnBadRequestWhenWaitlistPromotionFails() {
        // Given
        val promotionDto = WaitlistPromotionDto(
            sessionId = sessionId,
            registrationIds = listOf(UUID.randomUUID())
        )
        
        whenever(capacityManagementService.promoteFromWaitlist(promotionDto))
            .thenThrow(IllegalArgumentException("No waitlist entries to promote"))

        // When & Then
        mockMvc.perform(
            post("/api/capacity/waitlist/promote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(promotionDto))
        )
        .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should auto-promote waitlisted users successfully")
    fun shouldAutoPromoteWaitlistedUsersSuccessfully() {
        // Given
        val updatedCapacities = listOf(
            SessionCapacityDto(sessionId = sessionId, maximumCapacity = 100)
        )
        
        whenever(capacityManagementService.autoPromoteWaitlistedUsers()).thenReturn(updatedCapacities)

        // When & Then
        mockMvc.perform(
            post("/api/capacity/waitlist/auto-promote")
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$[0].sessionId").value(sessionId.toString()))
    }

    @Test
    @DisplayName("Should get capacity optimization suggestions successfully")
    fun shouldGetCapacityOptimizationSuggestionsSuccessfully() {
        // Given
        val suggestions = listOf(
            CapacityOptimizationSuggestionDto(
                sessionId = sessionId,
                sessionTitle = "Test Session",
                currentCapacity = 50,
                currentRegistrations = 50,
                suggestedCapacity = 70,
                optimizationType = "INCREASE",
                reason = "High waitlist demand",
                potentialImpact = "20 more attendees can be accommodated",
                priority = "HIGH"
            )
        )
        
        whenever(capacityManagementService.getCapacityOptimizationSuggestions(eventId)).thenReturn(suggestions)

        // When & Then
        mockMvc.perform(
            get("/api/capacity/events/{eventId}/optimization-suggestions", eventId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$[0].sessionId").value(sessionId.toString()))
        .andExpect(jsonPath("$[0].optimizationType").value("INCREASE"))
        .andExpect(jsonPath("$[0].priority").value("HIGH"))
    }
}