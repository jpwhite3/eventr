package com.eventr.controller

import com.eventr.service.CalendarService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003"])
@RequestMapping("/api/calendar")
class CalendarController(
    private val calendarService: CalendarService
) {
    
    @GetMapping("/event/{eventId}.ics")
    fun downloadEventCalendar(@PathVariable eventId: UUID): ResponseEntity<String> {
        return try {
            val icsContent = calendarService.generateEventCalendar(eventId)
            
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"event-$eventId.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(icsContent)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/event/{eventId}/registration/{registrationId}.ics")
    fun downloadPersonalizedCalendar(
        @PathVariable eventId: UUID,
        @PathVariable registrationId: UUID
    ): ResponseEntity<String> {
        return try {
            val icsContent = calendarService.generatePersonalizedCalendar(eventId, registrationId)
            
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"my-event-$eventId.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(icsContent)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/event/{eventId}/info")
    fun getEventCalendarInfo(@PathVariable eventId: UUID): ResponseEntity<Map<String, String>> {
        return try {
            // Generate URLs for different calendar providers
            val baseUrl = "http://localhost:8080/api/calendar/event/$eventId.ics"
            
            val calendarUrls = mapOf(
                "download" to baseUrl,
                "google" to "https://calendar.google.com/calendar/render?action=TEMPLATE&cid=$baseUrl",
                "outlook" to "https://outlook.live.com/calendar/0/deeplink/compose?rru=addevent&path=%2Fcalendar%2Faction%2Fcompose&rru=addevent&startdt=${getEventStartDate(eventId)}&subject=${getEventName(eventId)}"
            )
            
            ResponseEntity.ok(calendarUrls)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
    
    // Helper methods for calendar provider integration
    private fun getEventStartDate(eventId: UUID): String {
        // This would need to fetch the actual event start date
        // For now, return a placeholder
        return "2024-01-01T10:00:00Z"
    }
    
    private fun getEventName(eventId: UUID): String {
        // This would need to fetch the actual event name
        // For now, return a placeholder
        return "Event"
    }
}