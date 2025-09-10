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
        calendar.properties.add(ProdId("-//EventR//EventR Calendar//EN"))
        calendar.properties.add(Version.VERSION_2_0)
        calendar.properties.add(CalScale.GREGORIAN)
        
        val event = eventInstance.event ?: throw IllegalArgumentException("EventInstance must have an associated Event")
        val startDateTime = eventInstance.dateTime ?: event.startDateTime
        val endDateTime = event.endDateTime ?: startDateTime?.plusHours(1)
        
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
            vEvent.properties.add(Uid(UUID.randomUUID().toString()))
            vEvent.properties.add(DtStamp(net.fortuna.ical4j.model.DateTime()))
            
            // Add location if available
            val locationStr = formatCalendarLocation(event, eventInstance)
            if (locationStr.isNotBlank()) {
                vEvent.properties.add(Location(locationStr))
            }
            
            // Add description
            val description = formatCalendarDescription(event, eventInstance)
            if (description.isNotBlank()) {
                vEvent.properties.add(Description(description))
            }
            
            // Add organizer if available
            event.organizerEmail?.let { email ->
                try {
                    vEvent.properties.add(Organizer("mailto:$email"))
                } catch (e: Exception) {
                    // Skip organizer if invalid email format
                }
            }
            
            // Add categories
            event.category?.let { category ->
                vEvent.properties.add(Categories(category.toString()))
            }
            
            // Add status
            when (event.status) {
                EventStatus.PUBLISHED -> vEvent.properties.add(Status.VEVENT_CONFIRMED)
                EventStatus.DRAFT -> vEvent.properties.add(Status.VEVENT_TENTATIVE)
                else -> vEvent.properties.add(Status.VEVENT_TENTATIVE)
            }
            
            calendar.components.add(vEvent)
        }
        
        // Convert to byte array
        val outputStream = ByteArrayOutputStream()
        val outputter = CalendarOutputter()
        outputter.output(calendar, outputStream)
        
        return outputStream.toByteArray()
    }
    
    override fun createMultiEventIcsFile(eventInstances: List<EventInstance>, calendarName: String): ByteArray {
        val calendar = Calendar()
        calendar.properties.add(ProdId("-//EventR//EventR Calendar//EN"))
        calendar.properties.add(Version.VERSION_2_0)
        calendar.properties.add(CalScale.GREGORIAN)
        
        // Add calendar name
        calendar.properties.add(XProperty("X-WR-CALNAME", calendarName))
        
        eventInstances.forEach { eventInstance ->
            try {
                validateEventForCalendar(eventInstance)
                
                val event = eventInstance.event ?: return@forEach
                val startDateTime = eventInstance.dateTime ?: event.startDateTime
                val endDateTime = event.endDateTime ?: startDateTime?.plusHours(1)
                
                if (startDateTime != null && endDateTime != null) {
                    val startUtc = startDateTime.atZone(ZoneId.systemDefault())
                    val endUtc = endDateTime.atZone(ZoneId.systemDefault())
                    
                    val vEvent = VEvent(
                        net.fortuna.ical4j.model.DateTime(Date.from(startUtc.toInstant())),
                        net.fortuna.ical4j.model.DateTime(Date.from(endUtc.toInstant())),
                        event.name ?: "Event"
                    )
                    
                    vEvent.properties.add(Uid("${event.id}-${eventInstance.id}"))
                    vEvent.properties.add(DtStamp(net.fortuna.ical4j.model.DateTime()))
                    
                    val locationStr = formatCalendarLocation(event, eventInstance)
                    if (locationStr.isNotBlank()) {
                        vEvent.properties.add(Location(locationStr))
                    }
                    
                    val description = formatCalendarDescription(event, eventInstance)
                    if (description.isNotBlank()) {
                        vEvent.properties.add(Description(description))
                    }
                    
                    calendar.components.add(vEvent)
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
        if (event.eventType == EventType.VIRTUAL && !event.virtualUrl.isNullOrBlank()) {
            parts.add("Join online: ${event.virtualUrl}")
        }
        
        // Add registration info if available
        if (event.maxRegistrations != null) {
            parts.add("Registration required")
            
            event.maxRegistrations?.let { capacity ->
                if (capacity > 0) {
                    parts.add("Capacity: $capacity attendees")
                }
            }
        }
        
        // Add event instance specific details
        // EventInstance doesn't have notes property
        // Additional instance-specific info could be added here if needed
        
        return parts.joinToString("\n\n")
    }
    
    override fun formatCalendarLocation(event: Event, eventInstance: EventInstance): String {
        return when (event.eventType) {
            EventType.IN_PERSON -> {
                val locationParts = mutableListOf<String>()
                
                event.venueName?.let { venue ->
                    if (venue.isNotBlank()) {
                        locationParts.add(venue)
                    }
                }
                
                event.address?.let { address ->
                    if (address.isNotBlank() && address != event.venueName) {
                        locationParts.add(address)
                    }
                }
                
                locationParts.joinToString(", ")
            }
            
            EventType.VIRTUAL -> {
                event.virtualUrl ?: "Virtual Event"
            }
            
            EventType.HYBRID -> {
                val parts = mutableListOf<String>()
                
                event.venueName?.let { venue ->
                    if (venue.isNotBlank()) {
                        parts.add(venue)
                    }
                }
                
                event.virtualUrl?.let { url ->
                    parts.add("Online: $url")
                }
                
                parts.joinToString(" / ")
            }
            
            null -> "Event Location"
        }
    }
    
    override fun validateEventForCalendar(eventInstance: EventInstance) {
        val event = eventInstance.event ?: throw IllegalArgumentException("EventInstance must have an associated Event")
        
        if (event.name.isNullOrBlank()) {
            throw IllegalArgumentException("Event name is required for calendar generation")
        }
        
        val startDateTime = eventInstance.dateTime ?: event.startDateTime
        if (startDateTime == null) {
            throw IllegalArgumentException("Event start date/time is required for calendar generation")
        }
        
        val endDateTime = event.endDateTime
        if (endDateTime != null && endDateTime.isBefore(startDateTime)) {
            throw IllegalArgumentException("Event end date/time cannot be before start date/time")
        }
        
        // Validate location for in-person events
        if (event.eventType == EventType.IN_PERSON && 
            event.venueName.isNullOrBlank() && 
            event.address.isNullOrBlank()) {
            throw IllegalArgumentException("Location or address is required for in-person events")
        }
        
        // Validate meeting URL for virtual events
        if (event.eventType == EventType.VIRTUAL && event.virtualUrl.isNullOrBlank()) {
            throw IllegalArgumentException("Meeting URL is required for virtual events")
        }
    }
}