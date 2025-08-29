package com.eventr.controller

import com.eventr.dto.*
import com.eventr.service.CapacityManagementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/capacity")
class CapacityManagementController(
    private val capacityManagementService: CapacityManagementService
) {

    @PostMapping("/sessions/{sessionId}")
    fun createSessionCapacity(
        @PathVariable sessionId: UUID,
        @RequestBody capacityDto: SessionCapacityDto
    ): ResponseEntity<SessionCapacityDto> {
        return try {
            val created = capacityManagementService.createSessionCapacity(sessionId, capacityDto)
            ResponseEntity.ok(created)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/sessions/{sessionId}")
    fun updateSessionCapacity(
        @PathVariable sessionId: UUID,
        @RequestBody updateDto: CapacityUpdateDto
    ): ResponseEntity<SessionCapacityDto> {
        return try {
            val updated = capacityManagementService.updateSessionCapacity(sessionId, updateDto)
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/sessions/{sessionId}")
    fun getSessionCapacity(@PathVariable sessionId: UUID): ResponseEntity<SessionCapacityDto> {
        return try {
            val capacity = capacityManagementService.getSessionCapacity(sessionId)
            ResponseEntity.ok(capacity)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/events/{eventId}/analytics")
    fun getEventCapacityAnalytics(@PathVariable eventId: UUID): ResponseEntity<CapacityAnalyticsDto> {
        return try {
            val analytics = capacityManagementService.getEventCapacityAnalytics(eventId)
            ResponseEntity.ok(analytics)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/waitlist/promote")
    fun promoteFromWaitlist(@RequestBody promotionDto: WaitlistPromotionDto): ResponseEntity<List<SessionRegistrationDto>> {
        return try {
            val promoted = capacityManagementService.promoteFromWaitlist(promotionDto)
            ResponseEntity.ok(promoted)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/waitlist/auto-promote")
    fun autoPromoteWaitlistedUsers(): ResponseEntity<List<SessionCapacityDto>> {
        val updated = capacityManagementService.autoPromoteWaitlistedUsers()
        return ResponseEntity.ok(updated)
    }

    @GetMapping("/events/{eventId}/optimization-suggestions")
    fun getCapacityOptimizationSuggestions(@PathVariable eventId: UUID): ResponseEntity<List<CapacityOptimizationSuggestionDto>> {
        val suggestions = capacityManagementService.getCapacityOptimizationSuggestions(eventId)
        return ResponseEntity.ok(suggestions)
    }
}