package com.eventr.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class RegistrationTest {

    @Test
    fun shouldCreateRegistration() {
        val event = Event().apply {
            name = "Test Event"
            eventType = EventType.IN_PERSON
            category = EventCategory.BUSINESS
        }
        
        val eventInstance = EventInstance().apply {
            this.event = event
            dateTime = LocalDateTime.now().plusDays(7)
            location = "Conference Room A"
        }
        
        val registration = Registration().apply {
            this.eventInstance = eventInstance
            userName = "John Doe"
            userEmail = "john@example.com"
            status = RegistrationStatus.REGISTERED
            formData = """{"department": "Engineering", "dietary": "Vegetarian"}"""
        }
        
        assertEquals("John Doe", registration.userName)
        assertEquals("john@example.com", registration.userEmail)
        assertEquals(RegistrationStatus.REGISTERED, registration.status)
        assertEquals(eventInstance, registration.eventInstance)
        assertNotNull(registration.formData)
    }

    @Test
    fun shouldHandleRegistrationStatus() {
        val registration = Registration()
        
        // Test all registration statuses (only REGISTERED and CANCELLED available)
        registration.status = RegistrationStatus.REGISTERED
        assertEquals(RegistrationStatus.REGISTERED, registration.status)
        
        registration.status = RegistrationStatus.CANCELLED
        assertEquals(RegistrationStatus.CANCELLED, registration.status)
    }

    @Test
    fun shouldHandleEventWithRegistrationSettings() {
        val event = Event().apply {
            name = "Corporate Training"
            eventType = EventType.IN_PERSON
            category = EventCategory.BUSINESS
            requiresApproval = true
            maxRegistrations = 25
            capacity = 50
            waitlistEnabled = true
        }
        
        assertTrue(event.requiresApproval)
        assertEquals(25, event.maxRegistrations)
        assertEquals(50, event.capacity)
        assertTrue(event.waitlistEnabled ?: false)
    }

    @Test
    fun shouldHandleApprovalWorkflow() {
        val event = Event().apply {
            name = "Executive Meeting"
            requiresApproval = true
            maxRegistrations = 10
        }
        
        val registration = Registration().apply {
            userName = "Jane Smith"
            userEmail = "jane@example.com"
            status = RegistrationStatus.REGISTERED // Registration approved
        }
        
        assertEquals(RegistrationStatus.REGISTERED, registration.status)
        assertTrue(event.requiresApproval)
        
        // Can be cancelled if needed
        registration.status = RegistrationStatus.CANCELLED
        assertEquals(RegistrationStatus.CANCELLED, registration.status)
    }

    @Test
    fun shouldHandleCapacityLimits() {
        val event = Event().apply {
            name = "Workshop"
            maxRegistrations = 20
            capacity = 25
            waitlistEnabled = true
        }
        
        assertEquals(20, event.maxRegistrations)
        assertEquals(25, event.capacity)
        assertTrue(event.waitlistEnabled ?: false)
        
        // Simulate registration
        val registration = Registration().apply {
            userName = "John Overflow"
            userEmail = "overflow@example.com"
            status = RegistrationStatus.REGISTERED
        }
        
        assertEquals(RegistrationStatus.REGISTERED, registration.status)
    }

    @Test
    fun shouldHandleRegistrationData() {
        val registration = Registration().apply {
            userName = "Alice Johnson"
            userEmail = "alice@example.com"
            formData = """
                {
                  "department": "Marketing",
                  "role": "Manager",
                  "dietary_restrictions": ["Gluten-free"],
                  "emergency_contact": "+1-555-0123",
                  "t_shirt_size": "M"
                }
            """.trimIndent()
        }
        
        assertNotNull(registration.formData)
        assertTrue(registration.formData?.contains("Marketing") ?: false)
        assertTrue(registration.formData?.contains("Gluten-free") ?: false)
    }

    @Test
    fun shouldHandleEventInstanceRelationship() {
        val event = Event().apply {
            name = "Multi-session Workshop"
        }
        
        val session1 = EventInstance().apply {
            this.event = event
            dateTime = LocalDateTime.of(2024, 12, 1, 9, 0)
            location = "Room A"
        }
        
        val session2 = EventInstance().apply {
            this.event = event
            dateTime = LocalDateTime.of(2024, 12, 8, 9, 0)
            location = "Room B"
        }
        
        event.instances?.add(session1)
        event.instances?.add(session2)
        
        assertEquals(2, event.instances?.size)
        assertEquals(event, session1.event)
        assertEquals(event, session2.event)
    }
}