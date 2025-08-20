package com.eventr.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EventTest {

    @Test
    fun shouldCreateEventWithDefaults() {
        val event = Event()
        
        assertNull(event.id)
        assertNull(event.name)
        assertEquals(EventType.IN_PERSON, event.eventType)
        assertFalse(event.requiresApproval)
        assertEquals("UTC", event.timezone)
        assertNotNull(event.tags)
        assertNotNull(event.instances)
    }

    @Test
    fun shouldSetEventProperties() {
        val event = Event().apply {
            name = "Test Event"
            description = "Test Description"
            eventType = EventType.VIRTUAL
            category = EventCategory.TECHNOLOGY
            requiresApproval = true
            maxRegistrations = 100
            organizerName = "Test Organizer"
            venueName = "Test Venue"
            city = "Test City"
            virtualUrl = "https://zoom.us/test"
        }
        
        assertEquals("Test Event", event.name)
        assertEquals("Test Description", event.description)
        assertEquals(EventType.VIRTUAL, event.eventType)
        assertEquals(EventCategory.TECHNOLOGY, event.category)
        assertTrue(event.requiresApproval)
        assertEquals(100, event.maxRegistrations)
        assertEquals("Test Organizer", event.organizerName)
        assertEquals("Test Venue", event.venueName)
        assertEquals("Test City", event.city)
        assertEquals("https://zoom.us/test", event.virtualUrl)
    }

    @Test
    fun shouldHandleInPersonEventFields() {
        val event = Event().apply {
            eventType = EventType.IN_PERSON
            venueName = "Conference Center"
            address = "123 Main St"
            city = "New York"
            state = "NY"
            zipCode = "10001"
            country = "USA"
        }
        
        assertEquals(EventType.IN_PERSON, event.eventType)
        assertEquals("Conference Center", event.venueName)
        assertEquals("123 Main St", event.address)
        assertEquals("New York", event.city)
        assertEquals("NY", event.state)
        assertEquals("10001", event.zipCode)
        assertEquals("USA", event.country)
    }

    @Test
    fun shouldHandleVirtualEventFields() {
        val event = Event().apply {
            eventType = EventType.VIRTUAL
            virtualUrl = "https://meet.google.com/abc-def-ghi"
            dialInNumber = "+1-234-567-8900"
            accessCode = "123456"
        }
        
        assertEquals(EventType.VIRTUAL, event.eventType)
        assertEquals("https://meet.google.com/abc-def-ghi", event.virtualUrl)
        assertEquals("+1-234-567-8900", event.dialInNumber)
        assertEquals("123456", event.accessCode)
    }

    @Test
    fun shouldHandleHybridEventFields() {
        val event = Event().apply {
            eventType = EventType.HYBRID
            venueName = "Hybrid Venue"
            address = "456 Tech Ave"
            virtualUrl = "https://teams.microsoft.com/meeting"
            dialInNumber = "+1-555-123-4567"
        }
        
        assertEquals(EventType.HYBRID, event.eventType)
        assertEquals("Hybrid Venue", event.venueName)
        assertEquals("456 Tech Ave", event.address)
        assertEquals("https://teams.microsoft.com/meeting", event.virtualUrl)
        assertEquals("+1-555-123-4567", event.dialInNumber)
    }

    @Test
    fun shouldHandleRegistrationSettings() {
        val event = Event().apply {
            requiresApproval = true
            maxRegistrations = 50
            capacity = 100
            waitlistEnabled = true
        }
        
        assertTrue(event.requiresApproval)
        assertEquals(50, event.maxRegistrations)
        assertEquals(100, event.capacity)
        assertTrue(event.waitlistEnabled ?: false)
    }

    @Test
    fun shouldHandleOrganizerInformation() {
        val event = Event().apply {
            organizerName = "John Doe"
            organizerEmail = "john@example.com"
            organizerPhone = "+1-555-0123"
            organizerWebsite = "https://example.com"
        }
        
        assertEquals("John Doe", event.organizerName)
        assertEquals("john@example.com", event.organizerEmail)
        assertEquals("+1-555-0123", event.organizerPhone)
        assertEquals("https://example.com", event.organizerWebsite)
    }

    @Test
    fun shouldHandleEventCategories() {
        val businessEvent = Event().apply { category = EventCategory.BUSINESS }
        val techEvent = Event().apply { category = EventCategory.TECHNOLOGY }
        val eduEvent = Event().apply { category = EventCategory.EDUCATION }
        
        assertEquals(EventCategory.BUSINESS, businessEvent.category)
        assertEquals(EventCategory.TECHNOLOGY, techEvent.category)
        assertEquals(EventCategory.EDUCATION, eduEvent.category)
    }

    @Test
    fun shouldHandleEventTags() {
        val event = Event()
        event.tags?.add("networking")
        event.tags?.add("professional")
        event.tags?.add("workshop")
        
        assertEquals(3, event.tags?.size)
        assertTrue(event.tags?.contains("networking") ?: false)
        assertTrue(event.tags?.contains("professional") ?: false)
        assertTrue(event.tags?.contains("workshop") ?: false)
    }
}