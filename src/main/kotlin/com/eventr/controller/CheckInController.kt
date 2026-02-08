package com.eventr.controller

import com.eventr.dto.*
import com.eventr.service.interfaces.CheckInServiceInterface
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Check-in controller for MVP.
 * 
 * TODO: Re-add QR code generation when QRCodeService is created
 */
@RestController
@RequestMapping("/api/checkin")
class CheckInController(
    private val checkInService: CheckInServiceInterface
) {

    @PostMapping("/qr")
    fun checkInWithQR(@RequestBody qrCheckInDto: QRCheckInDto): ResponseEntity<CheckInDto> {
        return try {
            val result = checkInService.checkInWithQR(qrCheckInDto)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/manual")
    fun manualCheckIn(@RequestBody createDto: CheckInCreateDto): ResponseEntity<CheckInDto> {
        return try {
            val result = checkInService.manualCheckIn(createDto)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/bulk")
    fun bulkCheckIn(
        @RequestParam registrationIds: List<UUID>,
        @RequestParam(required = false) sessionId: UUID?
    ): ResponseEntity<List<CheckInDto>> {
        val results = checkInService.bulkCheckIn(registrationIds, sessionId)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/event/{eventId}/stats")
    fun getEventStats(@PathVariable eventId: UUID): ResponseEntity<Map<String, Any>> {
        val stats = checkInService.getCheckInStatistics(eventId)
        return ResponseEntity.ok(mapOf(
            "eventId" to stats.eventId,
            "eventName" to stats.eventName,
            "totalRegistrations" to stats.totalRegistrations,
            "totalCheckIns" to stats.totalCheckIns,
            "uniqueCheckIns" to stats.uniqueCheckIns,
            "checkInRate" to stats.checkInRate
        ))
    }

    @GetMapping("/{checkInId}")
    fun getCheckIn(@PathVariable checkInId: UUID): ResponseEntity<CheckInDto> {
        val checkIn = checkInService.getCheckInById(checkInId)
        return if (checkIn != null) {
            ResponseEntity.ok(checkIn)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/registration/{registrationId}")
    fun getCheckInsForRegistration(@PathVariable registrationId: UUID): ResponseEntity<List<CheckInDto>> {
        val checkIns = checkInService.getCheckInsForRegistration(registrationId)
        return ResponseEntity.ok(checkIns)
    }

    @GetMapping("/session/{sessionId}")
    fun getCheckInsForSession(@PathVariable sessionId: UUID): ResponseEntity<List<CheckInDto>> {
        val checkIns = checkInService.getCheckInsForSession(sessionId)
        return ResponseEntity.ok(checkIns)
    }
    
    // TODO: Add QR code generation endpoints when QRCodeService is created
}
