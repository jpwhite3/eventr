package com.eventr.controller

import com.eventr.service.CalendarService
import com.eventr.repository.EventRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/api/calendar")
class CalendarController(
    private val calendarService: CalendarService,
    private val eventRepository: EventRepository
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
    fun getEventCalendarInfo(@PathVariable eventId: UUID, request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val event = eventRepository.findById(eventId).orElseThrow { 
                IllegalArgumentException("Event not found") 
            }
            
            val baseUrl = getBaseUrl(request)
            val downloadUrl = "$baseUrl/api/calendar/event/$eventId.ics"
            
            val calendarUrls = mapOf(
                "download" to downloadUrl,
                "google" to generateGoogleCalendarUrl(event),
                "outlook" to generateOutlookCalendarUrl(event)
            )
            
            val eventInfo = mapOf(
                "urls" to calendarUrls,
                "event" to mapOf(
                    "name" to (event.name ?: "Event"),
                    "startDateTime" to event.startDateTime?.toString(),
                    "endDateTime" to event.endDateTime?.toString(),
                    "location" to (event.address ?: event.venueName ?: ""),
                    "description" to (event.description ?: "")
                )
            )
            
            ResponseEntity.ok(eventInfo)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/user/{userId}/events.ics")
    fun downloadUserEventsCalendar(@PathVariable userId: UUID): ResponseEntity<String> {
        return try {
            val icsContent = calendarService.generateUserEventsCalendar(userId)
            
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"my-events.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(icsContent)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/user/{userId}/registrations.ics")
    fun downloadUserRegistrationsCalendar(@PathVariable userId: UUID): ResponseEntity<String> {
        return try {
            val icsContent = calendarService.generateUserRegistrationsCalendar(userId)
            
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"my-registered-events.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(icsContent)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/user/{userId}/subscription")
    fun getCalendarSubscription(@PathVariable userId: UUID): ResponseEntity<Map<String, Any>> {
        return try {
            val subscription = calendarService.getCalendarSubscription(userId)
                ?: return ResponseEntity.notFound().build()
            
            val response: Map<String, Any> = mapOf(
                "url" to subscription.url,
                "token" to subscription.token,
                "expiresAt" to (subscription.expiresAt?.toString() ?: ""),
                "createdAt" to subscription.createdAt.toString()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/user/{userId}/subscription")
    fun createCalendarSubscription(@PathVariable userId: UUID, request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val baseUrl = getBaseUrl(request)
            val subscription = calendarService.createCalendarSubscription(userId, baseUrl)
            
            val response: Map<String, Any> = mapOf(
                "url" to subscription.url,
                "token" to subscription.token,
                "expiresAt" to (subscription.expiresAt?.toString() ?: ""),
                "createdAt" to subscription.createdAt.toString()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/feed/{token}.ics")
    fun getCalendarFeed(@PathVariable token: String): ResponseEntity<String> {
        return try {
            val icsContent = calendarService.generateCalendarFeed(token)
            
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/calendar; charset=utf-8")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600") // Cache for 1 hour
                .body(icsContent)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
    
    private fun getBaseUrl(request: HttpServletRequest): String {
        val scheme = request.scheme
        val serverName = request.serverName
        val serverPort = request.serverPort
        val contextPath = request.contextPath
        
        val portString = if ((scheme == "http" && serverPort != 80) || 
                           (scheme == "https" && serverPort != 443)) {
            ":$serverPort"
        } else {
            ""
        }
        
        return "$scheme://$serverName$portString$contextPath"
    }
    
    private fun generateGoogleCalendarUrl(event: com.eventr.model.Event): String {
        if (event.startDateTime == null || event.endDateTime == null) return "#"
        
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        val startDate = event.startDateTime!!.atZone(java.time.ZoneId.systemDefault()).format(formatter)
        val endDate = event.endDateTime!!.atZone(java.time.ZoneId.systemDefault()).format(formatter)
        
        val params = mapOf(
            "action" to "TEMPLATE",
            "text" to (event.name ?: "Event"),
            "dates" to "$startDate/$endDate",
            "details" to (event.description ?: "Event: ${event.name}"),
            "location" to buildEventLocation(event),
            "trp" to "false"
        )
        
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }
        
        return "https://calendar.google.com/calendar/render?$queryString"
    }
    
    private fun generateOutlookCalendarUrl(event: com.eventr.model.Event): String {
        if (event.startDateTime == null || event.endDateTime == null) return "#"
        
        val params = mapOf(
            "subject" to (event.name ?: "Event"),
            "startdt" to event.startDateTime!!.toString(),
            "enddt" to event.endDateTime!!.toString(),
            "body" to (event.description ?: "Event: ${event.name}"),
            "location" to buildEventLocation(event)
        )
        
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }
        
        return "https://outlook.live.com/calendar/0/deeplink/compose?$queryString"
    }
    
    private fun buildEventLocation(event: com.eventr.model.Event): String {
        val parts = mutableListOf<String>()
        
        event.venueName?.let { parts.add(it) }
        event.address?.let { parts.add(it) }
        
        val cityStateZip = listOfNotNull(
            event.city,
            event.state,
            event.zipCode
        ).joinToString(" ")
        
        if (cityStateZip.isNotEmpty()) {
            parts.add(cityStateZip)
        }
        
        event.country?.let { parts.add(it) }
        
        return parts.joinToString(", ")
    }
}