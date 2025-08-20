package com.eventr.controller

import com.eventr.dto.*
import com.eventr.service.ConflictDetectionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/conflicts")
@CrossOrigin(origins = ["http://localhost:3000"])
class ConflictDetectionController(
    private val conflictDetectionService: ConflictDetectionService
) {

    @PostMapping("/events/{eventId}/detect")
    fun detectConflicts(@PathVariable eventId: UUID): ResponseEntity<List<ScheduleConflictDto>> {
        val conflicts = conflictDetectionService.detectAllConflicts(eventId)
        return ResponseEntity.ok(conflicts)
    }

    @PostMapping("/events/{eventId}/detect/time-overlaps")
    fun detectTimeOverlapConflicts(@PathVariable eventId: UUID): ResponseEntity<List<ScheduleConflictDto>> {
        val conflicts = conflictDetectionService.detectTimeOverlapConflicts(eventId)
        return ResponseEntity.ok(conflicts)
    }

    @PostMapping("/events/{eventId}/detect/resources")
    fun detectResourceConflicts(@PathVariable eventId: UUID): ResponseEntity<List<ScheduleConflictDto>> {
        val conflicts = conflictDetectionService.detectResourceConflicts(eventId)
        return ResponseEntity.ok(conflicts)
    }

    @PostMapping("/events/{eventId}/detect/capacity")
    fun detectCapacityConflicts(@PathVariable eventId: UUID): ResponseEntity<List<ScheduleConflictDto>> {
        val conflicts = conflictDetectionService.detectCapacityConflicts(eventId)
        return ResponseEntity.ok(conflicts)
    }

    @PostMapping("/events/{eventId}/detect/users")
    fun detectUserConflicts(@PathVariable eventId: UUID): ResponseEntity<List<ScheduleConflictDto>> {
        val conflicts = conflictDetectionService.detectUserConflicts(eventId)
        return ResponseEntity.ok(conflicts)
    }

    @PostMapping("/{conflictId}/resolve")
    fun resolveConflict(
        @PathVariable conflictId: UUID,
        @RequestBody resolutionDto: ConflictResolutionDto
    ): ResponseEntity<ScheduleConflictDto> {
        return try {
            val resolved = conflictDetectionService.resolveConflict(conflictId, resolutionDto)
            ResponseEntity.ok(resolved)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/auto-resolve")
    fun autoResolveConflicts(): ResponseEntity<List<ScheduleConflictDto>> {
        val resolved = conflictDetectionService.autoResolveConflicts()
        return ResponseEntity.ok(resolved)
    }

    @GetMapping("/events/{eventId}/summary")
    fun getConflictSummary(@PathVariable eventId: UUID): ResponseEntity<ConflictSummaryDto> {
        val summary = conflictDetectionService.getConflictSummary(eventId)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/events/{eventId}/analytics")
    fun getConflictAnalytics(@PathVariable eventId: UUID): ResponseEntity<ConflictAnalyticsDto> {
        val analytics = conflictDetectionService.generateConflictAnalytics(eventId)
        return ResponseEntity.ok(analytics)
    }
}