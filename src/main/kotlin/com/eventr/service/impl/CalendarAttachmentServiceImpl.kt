package com.eventr.service.impl

import com.eventr.model.*
import com.eventr.service.CalendarAttachmentService
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * Implementation of CalendarAttachmentService focused on calendar file generation.
 * 
 * Responsibilities:
 * - ICS calendar file creation
 * - Event data conversion for calendar formats
 * - Calendar file validation and formatting
 * - Multi-event calendar generation
 */
@Service
class CalendarAttachmentServiceImpl : CalendarAttachmentService {
    
    override fun createIcsFile(eventInstance: EventInstance): ByteArray {
        validateEventForCalendar(eventInstance)
        
        val calendar = Calendar()
        calendar.add(ProdId("-//EventR//EventR Calendar//EN"))
        calendar.add(Version.VERSION_2_0)
        calendar.add(CalScale.GREGORIAN)
        
        val event = eventInstance.event
        val startDateTime = eventInstance.startDateTime ?: event.startDateTime
        val endDateTime = eventInstance.endDateTime ?: event.endDateTime ?: startDateTime?.plusHours(1)
        
        if (startDateTime != null && endDateTime != null) {
            // Convert to UTC for calendar
            val startUtc = startDateTime.atZone(ZoneId.systemDefault())
            val endUtc = endDateTime.atZone(ZoneId.systemDefault())
            
            val vEvent = VEvent(
                net.fortuna.ical4j.model.DateTime(Date.from(startUtc.toInstant())),
                net.fortuna.ical4j.model.DateTime(Date.from(endUtc.toInstant())),
                event.name ?: "Event"
            )
            
            // Add event properties
            vEvent.add(Uid(UUID.randomUUID().toString()))
            vEvent.add(DtStamp(net.fortuna.ical4j.model.DateTime()))
            
            // Add location if available
            val locationStr = formatCalendarLocation(event, eventInstance)
            if (locationStr.isNotBlank()) {
                vEvent.add(Location(locationStr))
            }
            
            // Add description
            val description = formatCalendarDescription(event, eventInstance)
            if (description.isNotBlank()) {
                vEvent.add(Description(description))
            }
            
            // Add organizer if available
            event.organizerEmail?.let { email ->
                try {
                    vEvent.add(Organizer("mailto:$email"))
                } catch (e: Exception) {
                    // Skip organizer if invalid email format
                }
            }
            
            // Add categories
            event.category?.let { category ->
                vEvent.add(Categories(category))
            }
            
            // Add status
            when (event.status) {
                EventStatus.PUBLISHED -> vEvent.add(Status.VEVENT_CONFIRMED)
                EventStatus.CANCELLED -> vEvent.add(Status.VEVENT_CANCELLED)
                else -> vEvent.add(Status.VEVENT_TENTATIVE)
            }
            
            calendar.add(vEvent)
        }
        
        // Convert to byte array
        val outputStream = ByteArrayOutputStream()
        val outputter = CalendarOutputter()
        outputter.output(calendar, outputStream)
        
        return outputStream.toByteArray()
    }
    
    override fun createMultiEventIcsFile(eventInstances: List<EventInstance>, calendarName: String): ByteArray {
        val calendar = Calendar()
        calendar.add(ProdId("-//EventR//EventR Calendar//EN"))
        calendar.add(Version.VERSION_2_0)
        calendar.add(CalScale.GREGORIAN)
        
        // Add calendar name
        calendar.add(XProperty("X-WR-CALNAME", calendarName))
        
        eventInstances.forEach { eventInstance ->
            try {
                validateEventForCalendar(eventInstance)
                
                val event = eventInstance.event
                val startDateTime = eventInstance.startDateTime ?: event.startDateTime
                val endDateTime = eventInstance.endDateTime ?: event.endDateTime ?: startDateTime?.plusHours(1)
                
                if (startDateTime != null && endDateTime != null) {
                    val startUtc = startDateTime.atZone(ZoneId.systemDefault())
                    val endUtc = endDateTime.atZone(ZoneId.systemDefault())
                    
                    val vEvent = VEvent(
                        net.fortuna.ical4j.model.DateTime(Date.from(startUtc.toInstant())),
                        net.fortuna.ical4j.model.DateTime(Date.from(endUtc.toInstant())),
                        event.name ?: "Event"
                    )
                    
                    vEvent.add(Uid("${event.id}-${eventInstance.id}"))
                    vEvent.add(DtStamp(net.fortuna.ical4j.model.DateTime()))
                    
                    val locationStr = formatCalendarLocation(event, eventInstance)
                    if (locationStr.isNotBlank()) {
                        vEvent.add(Location(locationStr))
                    }
                    
                    val description = formatCalendarDescription(event, eventInstance)
                    if (description.isNotBlank()) {
                        vEvent.add(Description(description))
                    }
                    
                    calendar.add(vEvent)
                }
            } catch (e: Exception) {
                // Skip invalid events but continue processing others
            }
        }
        
        val outputStream = ByteArrayOutputStream()
        val outputter = CalendarOutputter()
        outputter.output(calendar, outputStream)
        
        return outputStream.toByteArray()
    }
    
    override fun generateCalendarFilename(event: Event): String {
        val eventName = event.name ?: "Event"
        val sanitized = eventName
            .replace("[^a-zA-Z0-9\\s-_]".toRegex(), "")
            .replace("\\s+".toRegex(), "_")
            .take(50) // Limit length
        
        return "${sanitized}.ics"
    }
    
    override fun formatCalendarDescription(event: Event, eventInstance: EventInstance): String {
        val parts = mutableListOf<String>()
        
        // Add event description
        event.description?.let { description ->
            if (description.isNotBlank()) {
                parts.add(description.trim())
            }
        }
        
        // Add meeting URL for virtual events
        if (event.eventType == EventType.VIRTUAL && !event.meetingUrl.isNullOrBlank()) {
            parts.add("Join online: ${event.meetingUrl}")
        }
        
        // Add registration info if available
        if (event.requiresRegistration) {
            parts.add("Registration required")
            
            event.maxCapacity?.let { capacity ->
                if (capacity > 0) {
                    parts.add("Capacity: $capacity attendees")
                }
            }
        }
        
        // Add event instance specific details
        eventInstance.notes?.let { notes ->
            if (notes.isNotBlank()) {
                parts.add("Notes: $notes")
            }
        }
        
        return parts.joinToString("\n\n")
    }
    
    override fun formatCalendarLocation(event: Event, eventInstance: EventInstance): String {
        return when (event.eventType) {
            EventType.IN_PERSON -> {
                val locationParts = mutableListOf<String>()
                
                event.location?.let { location ->
                    if (location.isNotBlank()) {
                        locationParts.add(location)
                    }
                }
                
                event.address?.let { address ->
                    if (address.isNotBlank() && address != event.location) {
                        locationParts.add(address)
                    }
                }
                
                locationParts.joinToString(", ")
            }
            
            EventType.VIRTUAL -> {
                event.meetingUrl ?: "Virtual Event"
            }
            
            EventType.HYBRID -> {
                val parts = mutableListOf<String>()
                
                event.location?.let { location ->
                    if (location.isNotBlank()) {
                        parts.add(location)
                    }
                }
                
                event.meetingUrl?.let { url ->
                    parts.add("Online: $url")
                }
                
                parts.joinToString(" / ")
            }
        }
    }
    
    override fun validateEventForCalendar(eventInstance: EventInstance) {
        val event = eventInstance.event
        
        if (event.name.isNullOrBlank()) {
            throw IllegalArgumentException("Event name is required for calendar generation")
        }
        
        val startDateTime = eventInstance.startDateTime ?: event.startDateTime
        if (startDateTime == null) {
            throw IllegalArgumentException("Event start date/time is required for calendar generation")
        }
        
        val endDateTime = eventInstance.endDateTime ?: event.endDateTime
        if (endDateTime != null && endDateTime.isBefore(startDateTime)) {
            throw IllegalArgumentException("Event end date/time cannot be before start date/time")
        }
        
        // Validate location for in-person events
        if (event.eventType == EventType.IN_PERSON && 
            event.location.isNullOrBlank() && 
            event.address.isNullOrBlank()) {
            throw IllegalArgumentException("Location or address is required for in-person events")
        }
        
        // Validate meeting URL for virtual events
        if (event.eventType == EventType.VIRTUAL && event.meetingUrl.isNullOrBlank()) {
            throw IllegalArgumentException("Meeting URL is required for virtual events")
        }
    }
}