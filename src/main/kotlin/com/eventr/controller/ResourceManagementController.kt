package com.eventr.controller

import com.eventr.dto.*
import com.eventr.service.ResourceManagementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/resources")
class ResourceManagementController(
    private val resourceManagementService: ResourceManagementService
) {

    @GetMapping
    fun getAllResources(): ResponseEntity<List<ResourceDto>> {
        val resources = resourceManagementService.getAllResources()
        return ResponseEntity.ok(resources)
    }

    @GetMapping("/{resourceId}")
    fun getResource(@PathVariable resourceId: UUID): ResponseEntity<ResourceDto> {
        return try {
            val resource = resourceManagementService.getResourceById(resourceId)
            ResponseEntity.ok(resource)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createResource(@RequestBody createDto: ResourceCreateDto): ResponseEntity<ResourceDto> {
        val created = resourceManagementService.createResource(createDto)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{resourceId}")
    fun updateResource(
        @PathVariable resourceId: UUID,
        @RequestBody updateDto: ResourceCreateDto
    ): ResponseEntity<ResourceDto> {
        return try {
            val updated = resourceManagementService.updateResource(resourceId, updateDto)
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/sessions/{sessionId}/book")
    fun bookResourceForSession(
        @PathVariable sessionId: UUID,
        @RequestBody bookingDto: ResourceBookingDto
    ): ResponseEntity<SessionResourceDto> {
        return try {
            val booking = resourceManagementService.bookResourceForSession(sessionId, bookingDto)
            ResponseEntity.ok(booking)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/bookings/{bookingId}/approve")
    fun approveResourceBooking(
        @PathVariable bookingId: UUID,
        @RequestParam approverName: String
    ): ResponseEntity<SessionResourceDto> {
        return try {
            val approved = resourceManagementService.approveResourceBooking(bookingId, approverName)
            ResponseEntity.ok(approved)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/search")
    fun searchAvailableResources(@RequestBody searchDto: ResourceSearchDto): ResponseEntity<List<ResourceAvailabilityDto>> {
        val available = resourceManagementService.findAvailableResources(searchDto)
        return ResponseEntity.ok(available)
    }

    @GetMapping("/{resourceId}/utilization")
    fun getResourceUtilization(
        @PathVariable resourceId: UUID,
        @RequestParam startDate: LocalDateTime,
        @RequestParam endDate: LocalDateTime
    ): ResponseEntity<ResourceUtilizationDto> {
        return try {
            val utilization = resourceManagementService.getResourceUtilization(resourceId, startDate, endDate)
            ResponseEntity.ok(utilization)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/analytics")
    fun getResourceAnalytics(
        @RequestParam startDate: LocalDateTime,
        @RequestParam endDate: LocalDateTime
    ): ResponseEntity<ResourceAnalyticsDto> {
        val analytics = resourceManagementService.getResourceAnalytics(startDate, endDate)
        return ResponseEntity.ok(analytics)
    }
}