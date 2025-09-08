package com.eventr.service

import com.eventr.model.Event
import com.eventr.model.EventType
import com.eventr.model.Registration
import com.eventr.model.CalendarSubscription
import com.eventr.repository.EventRepository
import com.eventr.repository.RegistrationRepository
import com.eventr.repository.CalendarSubscriptionRepository
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.LocalDateTime
import java.util.*
import java.security.SecureRandom

@Service
class CalendarService(
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository,
    private val calendarSubscriptionRepository: CalendarSubscriptionRepository
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
            event.id?.let { eventId ->
                append("\\nEvent Details: ${getEventUrl(eventId)}")
            }
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
    
    fun generateUserEventsCalendar(userId: UUID): String {
        val registrations = registrationRepository.findByUserId(userId)
        val events = registrations.mapNotNull { registration ->
            registration.eventInstance?.event
        }
        
        return generateMultiEventIcsContent(events, "My Events")
    }
    
    fun generateUserRegistrationsCalendar(userId: UUID): String {
        val registrations = registrationRepository.findByUserId(userId)
        val eventsWithRegistrations = registrations.mapNotNull { registration ->
            registration.eventInstance?.event?.let { event -> Pair(event, registration) }
        }
        
        return generateMultiEventIcsContentWithRegistrations(eventsWithRegistrations, "My Registered Events")
    }
    
    fun createCalendarSubscription(userId: UUID, baseUrl: String): CalendarSubscription {
        // Check if user already has an active subscription
        val existingSubscription = calendarSubscriptionRepository.findByUserIdAndIsActive(userId, true)
        if (existingSubscription != null) {
            return existingSubscription
        }
        
        val token = generateSecureToken()
        val subscriptionUrl = "$baseUrl/api/calendar/feed/$token.ics"
        
        val subscription = CalendarSubscription(
            userId = userId,
            token = token,
            url = subscriptionUrl,
            expiresAt = LocalDateTime.now().plusYears(1) // Expires in 1 year
        )
        
        return calendarSubscriptionRepository.save(subscription)
    }
    
    fun getCalendarSubscription(userId: UUID): CalendarSubscription? {
        return calendarSubscriptionRepository.findByUserIdAndIsActive(userId, true)
    }
    
    fun generateCalendarFeed(token: String): String {
        val subscription = calendarSubscriptionRepository.findByToken(token)
            ?: throw IllegalArgumentException("Invalid calendar feed token")
        
        if (!subscription.isActive) {
            throw IllegalArgumentException("Calendar subscription is not active")
        }
        
        if (subscription.expiresAt != null && subscription.expiresAt.isBefore(LocalDateTime.now())) {
            throw IllegalArgumentException("Calendar subscription has expired")
        }
        
        return generateUserRegistrationsCalendar(subscription.userId)
    }
    
    private fun generateMultiEventIcsContent(events: List<Event>, calendarName: String): String {
        val now = ZonedDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        
        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Eventr//Event Management//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")
            appendLine("X-WR-CALNAME:$calendarName")
            appendLine("X-WR-CALDESC:Personal calendar feed from Eventr")
            
            events.forEach { event ->
                appendLine("BEGIN:VEVENT")
                val uid = "event-${event.id}-${UUID.randomUUID()}"
                val summary = escapeIcsText(event.name ?: "Event")
                val description = buildDescription(event, null)
                val location = buildLocation(event)
                
                val startDateTime = event.startDateTime?.atZone(ZoneId.systemDefault()) ?: now.plusDays(1)
                val endDateTime = event.endDateTime?.atZone(ZoneId.systemDefault()) ?: startDateTime.plusHours(2)
                
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
                
                if (!event.organizerEmail.isNullOrBlank()) {
                    val organizerName = event.organizerName ?: "Event Organizer"
                    appendLine("ORGANIZER;CN=$organizerName:MAILTO:${event.organizerEmail}")
                }
                
                // Add alarm/reminder (30 minutes before)
                appendLine("BEGIN:VALARM")
                appendLine("TRIGGER:-PT30M")
                appendLine("DESCRIPTION:Event reminder")
                appendLine("ACTION:DISPLAY")
                appendLine("END:VALARM")
                
                appendLine("END:VEVENT")
            }
            
            appendLine("END:VCALENDAR")
        }
    }
    
    private fun generateMultiEventIcsContentWithRegistrations(eventsWithRegistrations: List<Pair<Event, Registration>>, calendarName: String): String {
        val now = ZonedDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        
        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Eventr//Event Management//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")
            appendLine("X-WR-CALNAME:$calendarName")
            appendLine("X-WR-CALDESC:Registered events calendar feed from Eventr")
            
            eventsWithRegistrations.forEach { (event, registration) ->
                appendLine("BEGIN:VEVENT")
                val uid = "event-${event.id}-reg-${registration.id}"
                val summary = escapeIcsText(event.name ?: "Event")
                val description = buildDescription(event, registration)
                val location = buildLocation(event)
                
                val startDateTime = event.startDateTime?.atZone(ZoneId.systemDefault()) ?: now.plusDays(1)
                val endDateTime = event.endDateTime?.atZone(ZoneId.systemDefault()) ?: startDateTime.plusHours(2)
                
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
                
                if (!event.organizerEmail.isNullOrBlank()) {
                    val organizerName = event.organizerName ?: "Event Organizer"
                    appendLine("ORGANIZER;CN=$organizerName:MAILTO:${event.organizerEmail}")
                }
                
                // Add attendee information
                appendLine("ATTENDEE;CN=${registration.userName};RSVP=TRUE:MAILTO:${registration.userEmail}")
                
                // Add alarm/reminder (30 minutes before)
                appendLine("BEGIN:VALARM")
                appendLine("TRIGGER:-PT30M")
                appendLine("DESCRIPTION:Event reminder")
                appendLine("ACTION:DISPLAY")
                appendLine("END:VALARM")
                
                appendLine("END:VEVENT")
            }
            
            appendLine("END:VCALENDAR")
        }
    }
    
    private fun generateSecureToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..32)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}