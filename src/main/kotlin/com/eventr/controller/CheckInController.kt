package com.eventr.controller

import com.eventr.dto.*
import com.eventr.service.CheckInService
import com.eventr.service.WebSocketEventService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003"])
@RequestMapping("/api/checkin")
class CheckInController(
    private val checkInService: CheckInService,
    private val webSocketEventService: WebSocketEventService
) {

    @PostMapping("/qr")
    fun checkInWithQR(@RequestBody qrCheckInDto: QRCheckInDto): ResponseEntity<CheckInDto> {
        return try {
            val result = checkInService.checkInWithQR(qrCheckInDto)
            
            // Broadcast real-time check-in update
            qrCheckInDto.eventId?.let { eventIdValue ->
                webSocketEventService.broadcastCheckInUpdate(
                    eventIdValue,
                    result.userName ?: "Unknown",
                    result.userEmail ?: "",
                    "CHECKED_IN"
                )
            }
            
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
    fun bulkCheckIn(@RequestBody bulkDto: BulkCheckInDto): ResponseEntity<List<CheckInDto>> {
        val results = checkInService.bulkCheckIn(bulkDto)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/event/{eventId}/stats")
    fun getEventStats(@PathVariable eventId: UUID): ResponseEntity<CheckInStatsDto> {
        val stats = checkInService.getEventCheckInStats(eventId)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/session/{sessionId}/attendance")
    fun getSessionAttendance(@PathVariable sessionId: UUID): ResponseEntity<List<CheckInDto>> {
        val attendance = checkInService.getSessionAttendance(sessionId)
        return ResponseEntity.ok(attendance)
    }

    @GetMapping("/event/{eventId}/report")
    fun getAttendanceReport(@PathVariable eventId: UUID): ResponseEntity<AttendanceReportDto> {
        val report = checkInService.getAttendanceReport(eventId)
        return ResponseEntity.ok(report)
    }

    @PostMapping("/sync")
    fun syncOfflineCheckIns(@RequestBody offlineCheckIns: List<OfflineCheckInDto>): ResponseEntity<List<CheckInDto>> {
        val results = checkInService.syncOfflineCheckIns(offlineCheckIns)
        return ResponseEntity.ok(results)
    }

    // QR Code Generation Endpoints
    
    @GetMapping("/qr/event/{eventId}/user/{userId}")
    fun generateEventQR(
        @PathVariable eventId: UUID,
        @PathVariable userId: String
    ): ResponseEntity<QRCodeResponseDto> {
        val qrCode = checkInService.generateEventQRCode(eventId, userId)
        return ResponseEntity.ok(qrCode)
    }

    @GetMapping("/qr/session/{sessionId}/user/{userId}")
    fun generateSessionQR(
        @PathVariable sessionId: UUID,
        @PathVariable userId: String
    ): ResponseEntity<QRCodeResponseDto> {
        val qrCode = checkInService.generateSessionQRCode(sessionId, userId)
        return ResponseEntity.ok(qrCode)
    }

    @GetMapping("/qr/staff/event/{eventId}")
    fun generateStaffEventQR(@PathVariable eventId: UUID): ResponseEntity<QRCodeResponseDto> {
        val qrCode = checkInService.generateStaffQRCode(eventId)
        return ResponseEntity.ok(qrCode)
    }

    @GetMapping("/qr/staff/session/{sessionId}")
    fun generateStaffSessionQR(
        @PathVariable sessionId: UUID,
        @RequestParam eventId: UUID
    ): ResponseEntity<QRCodeResponseDto> {
        val qrCode = checkInService.generateStaffQRCode(eventId, sessionId)
        return ResponseEntity.ok(qrCode)
    }

    @GetMapping("/qr/badge/event/{eventId}/user/{userId}")
    fun generateAttendeeBadge(
        @PathVariable eventId: UUID,
        @PathVariable userId: String,
        @RequestParam userName: String
    ): ResponseEntity<QRCodeResponseDto> {
        val badge = checkInService.generateAttendeeBadge(eventId, userId, userName)
        return ResponseEntity.ok(badge)
    }

    // Direct QR Code Image Download (for printing badges)
    
    @GetMapping("/qr/badge/event/{eventId}/user/{userId}/image")
    fun downloadBadgeImage(
        @PathVariable eventId: UUID,
        @PathVariable userId: String,
        @RequestParam userName: String
    ): ResponseEntity<ByteArray> {
        val badge = checkInService.generateAttendeeBadge(eventId, userId, userName)
        val imageBytes = Base64.getDecoder().decode(badge.qrCodeBase64)
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"badge-$userId.png\"")
            .contentType(MediaType.IMAGE_PNG)
            .body(imageBytes)
    }

    @GetMapping("/qr/staff/event/{eventId}/image")
    fun downloadStaffQRImage(@PathVariable eventId: UUID): ResponseEntity<ByteArray> {
        val qrCode = checkInService.generateStaffQRCode(eventId)
        val imageBytes = Base64.getDecoder().decode(qrCode.qrCodeBase64)
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"staff-checkin-$eventId.png\"")
            .contentType(MediaType.IMAGE_PNG)
            .body(imageBytes)
    }

    @GetMapping("/qr/staff/session/{sessionId}/image")
    fun downloadStaffSessionQRImage(
        @PathVariable sessionId: UUID,
        @RequestParam eventId: UUID
    ): ResponseEntity<ByteArray> {
        val qrCode = checkInService.generateStaffQRCode(eventId, sessionId)
        val imageBytes = Base64.getDecoder().decode(qrCode.qrCodeBase64)
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"session-checkin-$sessionId.png\"")
            .contentType(MediaType.IMAGE_PNG)
            .body(imageBytes)
    }
}