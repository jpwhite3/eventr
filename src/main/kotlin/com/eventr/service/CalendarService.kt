package com.eventr.service

import com.eventr.model.Event
import com.eventr.model.EventType
import com.eventr.model.Registration
import com.eventr.repository.EventRepository
import com.eventr.repository.RegistrationRepository
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.time.ZoneId
import java.util.*

@Service
class CalendarService(
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository
) {
    
    fun generateEventCalendar(eventId: UUID): String {
        val event = eventRepository.findById(eventId).orElseThrow { 
            IllegalArgumentException("Event not found") 
        }
        
        return generateIcsContent(event, null)
    }
    
    fun generatePersonalizedCalendar(eventId: UUID, registrationId: UUID): String {
        val event = eventRepository.findById(eventId).orElseThrow { 
            IllegalArgumentException("Event not found") 
        }
        val registration = registrationRepository.findById(registrationId).orElseThrow { 
            IllegalArgumentException("Registration not found") 
        }
        
        return generateIcsContent(event, registration)
    }
    
    private fun generateIcsContent(event: Event, registration: Registration?): String {
        val now = ZonedDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        
        val startDateTime = event.startDateTime?.atZone(ZoneId.systemDefault()) ?: now.plusDays(1)
        val endDateTime = event.endDateTime?.atZone(ZoneId.systemDefault()) ?: startDateTime.plusHours(2)
        
        val uid = "event-${event.id}-${UUID.randomUUID()}"
        val summary = escapeIcsText(event.name ?: "Event")
        val description = buildDescription(event, registration)
        val location = buildLocation(event)
        
        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Eventr//Event Management//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")
            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            appendLine("DTSTAMP:${now.format(formatter)}")
            appendLine("DTSTART:${startDateTime.format(formatter)}")
            appendLine("DTEND:${endDateTime.format(formatter)}")
            appendLine("SUMMARY:$summary")
            appendLine("DESCRIPTION:$description")
            if (location.isNotEmpty()) {
                appendLine("LOCATION:$location")
            }
            appendLine("STATUS:CONFIRMED")
            appendLine("TRANSP:OPAQUE")
            
            // Add organizer information
            if (!event.organizerEmail.isNullOrBlank()) {
                val organizerName = event.organizerName ?: "Event Organizer"
                appendLine("ORGANIZER;CN=$organizerName:MAILTO:${event.organizerEmail}")
            }
            
            // Add attendee information if this is a personalized calendar
            registration?.let {
                appendLine("ATTENDEE;CN=${it.userName};RSVP=TRUE:MAILTO:${it.userEmail}")
            }
            
            // Add alarm/reminder (30 minutes before)
            appendLine("BEGIN:VALARM")
            appendLine("TRIGGER:-PT30M")
            appendLine("DESCRIPTION:Event reminder")
            appendLine("ACTION:DISPLAY")
            appendLine("END:VALARM")
            
            appendLine("END:VEVENT")
            appendLine("END:VCALENDAR")
        }
    }
    
    private fun buildDescription(event: Event, registration: Registration?): String {
        return buildString {
            event.description?.takeIf { it.isNotBlank() }?.let { desc ->
                append(escapeIcsText(desc))
                append("\\n\\n")
            }
            
            // Add event type information
            append("Event Type: ${event.eventType ?: "TBD"}\\n")
            
            // Add virtual meeting details if available
            if (event.eventType == EventType.VIRTUAL || event.eventType == EventType.HYBRID) {
                event.virtualUrl?.let { 
                    append("Meeting URL: $it\\n")
                }
                event.dialInNumber?.let { 
                    append("Dial-in: $it\\n")
                }
                event.accessCode?.let { 
                    append("Access Code: $it\\n")
                }
            }
            
            // Add registration information
            registration?.let {
                append("\\nRegistered as: ${it.userName}\\n")
                append("Registration ID: ${it.id}\\n")
            }
            
            // Add event website/details link
            append("\\nEvent Details: ${getEventUrl(event.id!!)}")
        }
    }
    
    private fun buildLocation(event: Event): String {
        return when (event.eventType) {
            EventType.VIRTUAL -> "Virtual Event"
            EventType.HYBRID -> {
                val physicalLocation = buildPhysicalLocation(event)
                if (physicalLocation.isNotEmpty()) {
                    "$physicalLocation (Hybrid - Virtual option available)"
                } else {
                    "Hybrid Event"
                }
            }
            else -> buildPhysicalLocation(event)
        }
    }
    
    private fun buildPhysicalLocation(event: Event): String {
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
    
    private fun escapeIcsText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
            .replace("\r", "")
    }
    
    private fun getEventUrl(eventId: UUID): String {
        // This should be configurable based on environment
        return "http://localhost:3002/events/$eventId"
    }
}