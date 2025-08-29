package com.eventr.controller

import com.eventr.service.SessionService
import com.eventr.service.SessionDto
import com.eventr.service.CreateSessionDto
import com.eventr.service.UpdateSessionDto
import com.eventr.service.AttendeeDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/sessions")
class SessionController(
    private val sessionService: SessionService
) {

    @GetMapping("/event/{eventId}")
    fun getSessionsByEvent(@PathVariable eventId: UUID): ResponseEntity<List<SessionDto>> {
        val sessions = sessionService.getSessionsByEvent(eventId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/{id}")
    fun getSessionById(@PathVariable id: UUID): ResponseEntity<SessionDto> {
        val session = sessionService.getSessionById(id)
        return if (session != null) {
            ResponseEntity.ok(session)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createSession(@RequestBody createDto: CreateSessionDto): ResponseEntity<SessionDto> {
        return try {
            val session = sessionService.createSession(createDto)
            ResponseEntity.ok(session)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    fun updateSession(
        @PathVariable id: UUID,
        @RequestBody updateDto: UpdateSessionDto
    ): ResponseEntity<SessionDto> {
        return try {
            val session = sessionService.updateSession(id, updateDto)
            if (session != null) {
                ResponseEntity.ok(session)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteSession(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {
        return if (sessionService.deleteSession(id)) {
            ResponseEntity.ok(mapOf("message" to "Session deleted successfully"))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{id}/attendees")
    fun getSessionAttendees(@PathVariable id: UUID): ResponseEntity<List<AttendeeDto>> {
        val attendees = sessionService.getSessionAttendees(id)
        return ResponseEntity.ok(attendees)
    }
}