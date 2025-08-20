package com.eventr.dto

import com.eventr.model.EventCategory
import com.eventr.model.EventStatus
import com.eventr.model.EventType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.util.*

class EventDtoTest {

    @Test
    fun shouldCreateEventDto() {
        val eventDto = EventDto().apply {
            id = UUID.randomUUID()
            name = "Corporate Training"
            description = "Annual training event"
            eventType = EventType.IN_PERSON
            category = EventCategory.BUSINESS
            status = EventStatus.PUBLISHED
            venueName = "Main Conference Room"
            city = "San Francisco"
            organizerName = "HR Department"
            requiresApproval = true
            maxRegistrations = 25
            startDateTime = LocalDateTime.of(2024, 12, 15, 9, 0)
            endDateTime = LocalDateTime.of(2024, 12, 15, 17, 0)
        }

        assertNotNull(eventDto.id)
        assertEquals("Corporate Training", eventDto.name)
        assertEquals("Annual training event", eventDto.description)
        assertEquals(EventType.IN_PERSON, eventDto.eventType)
        assertEquals(EventCategory.BUSINESS, eventDto.category)
        assertEquals(EventStatus.PUBLISHED, eventDto.status)
        assertEquals("Main Conference Room", eventDto.venueName)
        assertEquals("San Francisco", eventDto.city)
        assertEquals("HR Department", eventDto.organizerName)
        assertTrue(eventDto.requiresApproval)
        assertEquals(25, eventDto.maxRegistrations)
        assertNotNull(eventDto.startDateTime)
        assertNotNull(eventDto.endDateTime)
    }

    @Test
    fun shouldCreateEventCreateDto() {
        val createDto = EventCreateDto().apply {
            name = "New Event"
            description = "Event description"
            eventType = EventType.VIRTUAL
            category = EventCategory.TECHNOLOGY
            virtualUrl = "https://zoom.us/meeting"
            requiresApproval = false
            maxRegistrations = 100
            organizerName = "Tech Team"
            tags = listOf("tech", "virtual", "training")
        }

        assertEquals("New Event", createDto.name)
        assertEquals("Event description", createDto.description)
        assertEquals(EventType.VIRTUAL, createDto.eventType)
        assertEquals(EventCategory.TECHNOLOGY, createDto.category)
        assertEquals("https://zoom.us/meeting", createDto.virtualUrl)
        assertFalse(createDto.requiresApproval ?: true)
        assertEquals(100, createDto.maxRegistrations)
        assertEquals("Tech Team", createDto.organizerName)
        assertEquals(3, createDto.tags?.size)
        assertTrue(createDto.tags?.contains("tech") ?: false)
    }

    @Test
    fun shouldCreateEventUpdateDto() {
        val updateDto = EventUpdateDto().apply {
            name = "Updated Event Name"
            description = "Updated description"
            eventType = EventType.HYBRID
            category = EventCategory.EDUCATION
            venueName = "Updated Venue"
            virtualUrl = "https://updated-meeting-url.com"
            requiresApproval = true
            maxRegistrations = 75
        }

        assertEquals("Updated Event Name", updateDto.name)
        assertEquals("Updated description", updateDto.description)
        assertEquals(EventType.HYBRID, updateDto.eventType)
        assertEquals(EventCategory.EDUCATION, updateDto.category)
        assertEquals("Updated Venue", updateDto.venueName)
        assertEquals("https://updated-meeting-url.com", updateDto.virtualUrl)
        assertTrue(updateDto.requiresApproval ?: false)
        assertEquals(75, updateDto.maxRegistrations)
    }

    @Test
    fun shouldHandleNullableFields() {
        val eventDto = EventDto()
        
        assertNull(eventDto.id)
        assertNull(eventDto.name)
        assertNull(eventDto.description)
        assertNull(eventDto.eventType)
        assertNull(eventDto.category)
        assertNull(eventDto.venueName)
        assertNull(eventDto.virtualUrl)
        assertNull(eventDto.organizerName)
        assertFalse(eventDto.requiresApproval ?: true) // default value
    }

    @Test
    fun shouldHandleVirtualEventFields() {
        val eventDto = EventDto().apply {
            eventType = EventType.VIRTUAL
            virtualUrl = "https://meet.google.com/abc-def-ghi"
            dialInNumber = "+1-800-123-4567"
            accessCode = "987654"
        }

        assertEquals(EventType.VIRTUAL, eventDto.eventType)
        assertEquals("https://meet.google.com/abc-def-ghi", eventDto.virtualUrl)
        assertEquals("+1-800-123-4567", eventDto.dialInNumber)
        assertEquals("987654", eventDto.accessCode)
    }

    @Test
    fun shouldHandleInPersonEventFields() {
        val eventDto = EventDto().apply {
            eventType = EventType.IN_PERSON
            venueName = "Corporate Headquarters"
            address = "123 Business Ave"
            city = "New York"
            state = "NY"
            zipCode = "10001"
            country = "USA"
        }

        assertEquals(EventType.IN_PERSON, eventDto.eventType)
        assertEquals("Corporate Headquarters", eventDto.venueName)
        assertEquals("123 Business Ave", eventDto.address)
        assertEquals("New York", eventDto.city)
        assertEquals("NY", eventDto.state)
        assertEquals("10001", eventDto.zipCode)
        assertEquals("USA", eventDto.country)
    }
}