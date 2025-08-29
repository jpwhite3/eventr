package com.eventr.controller

import com.eventr.service.CheckInStatsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/checkin/stats")
class CheckInStatsController(
    private val checkInStatsService: CheckInStatsService
) {

    @GetMapping("/event/{eventId}")
    fun getEventCheckInStats(@PathVariable eventId: UUID): ResponseEntity<Map<String, Any>> {
        val stats = checkInStatsService.getEventCheckInStats(eventId)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/session/{sessionId}")
    fun getSessionCheckInStats(@PathVariable sessionId: UUID): ResponseEntity<Map<String, Any>> {
        val stats = checkInStatsService.getSessionCheckInStats(sessionId)
        return ResponseEntity.ok(stats)
    }

    @PostMapping("/event/{eventId}/checkin")
    fun checkInToEvent(
        @PathVariable eventId: UUID,
        @RequestBody checkInData: Map<String, String>
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val result = checkInStatsService.checkInToEvent(eventId, checkInData)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Check-in failed")))
        }
    }

    @PostMapping("/session/{sessionId}/checkin")
    fun checkInToSession(
        @PathVariable sessionId: UUID,
        @RequestBody checkInData: Map<String, String>
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val result = checkInStatsService.checkInToSession(sessionId, checkInData)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Check-in failed")))
        }
    }

    @GetMapping("/event/{eventId}/attendees")
    fun getEventAttendees(@PathVariable eventId: UUID): ResponseEntity<List<Map<String, Any>>> {
        val attendees = checkInStatsService.getEventAttendees(eventId)
        return ResponseEntity.ok(attendees)
    }
}