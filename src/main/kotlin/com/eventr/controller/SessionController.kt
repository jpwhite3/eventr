package com.eventr.controller

import com.eventr.dto.*
import com.eventr.service.SessionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/sessions")
class SessionController(
    private val sessionService: SessionService
) {

    @PostMapping
    fun createSession(@RequestBody createDto: SessionCreateDto): ResponseEntity<SessionDto> {
        return try {
            val session = sessionService.createSession(createDto)
            ResponseEntity.ok(session)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{sessionId}")
    fun updateSession(
        @PathVariable sessionId: UUID,
        @RequestBody updateDto: SessionUpdateDto
    ): ResponseEntity<SessionDto> {
        return try {
            val session = sessionService.updateSession(sessionId, updateDto)
            ResponseEntity.ok(session)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{sessionId}")
    fun getSession(@PathVariable sessionId: UUID): ResponseEntity<SessionDto> {
        val session = sessionService.getSessionById(sessionId)
        return if (session != null) {
            ResponseEntity.ok(session)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{sessionId}")
    fun deleteSession(@PathVariable sessionId: UUID): ResponseEntity<Void> {
        return try {
            sessionService.deleteSession(sessionId)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/event/{eventId}")
    fun getEventSessions(@PathVariable eventId: UUID): ResponseEntity<List<SessionDto>> {
        val sessions = sessionService.getSessionsByEventId(eventId)
        return ResponseEntity.ok(sessions)
    }

    @PostMapping("/register")
    fun registerForSession(@RequestBody createDto: SessionRegistrationCreateDto): ResponseEntity<SessionRegistrationDto> {
        return try {
            val registration = sessionService.registerForSession(createDto)
            ResponseEntity.ok(registration)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/register/bulk")
    fun registerForMultipleSessions(@RequestBody bulkDto: BulkSessionRegistrationDto): ResponseEntity<List<SessionRegistrationDto>> {
        return try {
            val registrations = bulkDto.sessionIds.map { sessionId ->
                sessionService.registerForSession(
                    SessionRegistrationCreateDto(
                        sessionId = sessionId,
                        registrationId = bulkDto.registrationId,
                        notes = bulkDto.notes
                    )
                )
            }
            ResponseEntity.ok(registrations)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/registration/{sessionRegistrationId}/cancel")
    fun cancelSessionRegistration(@PathVariable sessionRegistrationId: UUID): ResponseEntity<SessionRegistrationDto> {
        return try {
            val registration = sessionService.cancelSessionRegistration(sessionRegistrationId)
            ResponseEntity.ok(registration)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/checkin")
    fun checkInToSession(@RequestBody attendanceDto: SessionAttendanceDto): ResponseEntity<SessionRegistrationDto> {
        return try {
            val registration = sessionService.checkInToSession(attendanceDto)
            ResponseEntity.ok(registration)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{sessionId}/registrations")
    fun getSessionRegistrations(@PathVariable sessionId: UUID): ResponseEntity<List<SessionRegistrationDto>> {
        val registrations = sessionService.getSessionRegistrations(sessionId)
        return ResponseEntity.ok(registrations)
    }

    @GetMapping("/registration/{registrationId}/sessions")
    fun getUserSessionRegistrations(@PathVariable registrationId: UUID): ResponseEntity<List<SessionRegistrationDto>> {
        val registrations = sessionService.getUserSessionRegistrations(registrationId)
        return ResponseEntity.ok(registrations)
    }
}