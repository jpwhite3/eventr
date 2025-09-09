package com.eventr.service

import com.eventr.model.*

/**
 * Service interface for calendar file generation and attachment handling.
 * 
 * Focuses exclusively on calendar integration:
 * - ICS calendar file generation
 * - Event data conversion for calendar formats
 * - Calendar attachment creation
 * 
 * Follows Single Responsibility Principle by handling only calendar generation logic.
 */
interface CalendarAttachmentService {

    /**
     * Create ICS calendar file for event instance.
     * 
     * @param eventInstance Event instance to create calendar for
     * @return ICS file content as byte array
     * @throws Exception if calendar generation fails
     */
    fun createIcsFile(eventInstance: EventInstance): ByteArray

    /**
     * Create ICS calendar file for multiple event instances.
     * 
     * @param eventInstances List of event instances
     * @param calendarName Name for the calendar
     * @return ICS file content as byte array
     * @throws Exception if calendar generation fails
     */
    fun createMultiEventIcsFile(eventInstances: List<EventInstance>, calendarName: String): ByteArray

    /**
     * Generate calendar filename for event.
     * 
     * @param event Event to generate filename for
     * @return Sanitized filename suitable for ICS attachment
     */
    fun generateCalendarFilename(event: Event): String

    /**
     * Create calendar event description from event data.
     * 
     * @param event Event information
     * @param eventInstance Specific event instance
     * @return Formatted description for calendar entry
     */
    fun formatCalendarDescription(event: Event, eventInstance: EventInstance): String

    /**
     * Create calendar event location from event data.
     * 
     * @param event Event information
     * @param eventInstance Specific event instance
     * @return Formatted location for calendar entry
     */
    fun formatCalendarLocation(event: Event, eventInstance: EventInstance): String

    /**
     * Validate event data for calendar generation.
     * 
     * @param eventInstance Event instance to validate
     * @throws IllegalArgumentException if event data is insufficient for calendar
     */
    fun validateEventForCalendar(eventInstance: EventInstance)
}