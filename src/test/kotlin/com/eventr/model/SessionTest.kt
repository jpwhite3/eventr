package com.eventr.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class SessionTest {

    @Test
    fun `should create session with default values`() {
        val session = Session()
        
        assertEquals("", session.title)
        assertEquals(SessionType.PRESENTATION, session.type)
        assertTrue(session.isRegistrationRequired)
        assertTrue(session.isWaitlistEnabled)
        assertTrue(session.isActive)
        assertNotNull(session.createdAt)
        assertNotNull(session.updatedAt)
    }

    @Test
    fun `should create session with specified values`() {
        val startTime = LocalDateTime.now().plusDays(1)
        val endTime = startTime.plusHours(2)
        
        val session = Session(
            title = "Test Workshop",
            description = "A test workshop session",
            type = SessionType.WORKSHOP,
            startTime = startTime,
            endTime = endTime,
            location = "Main Building",
            room = "Room A",
            capacity = 25,
            presenter = "John Doe"
        )
        
        assertEquals("Test Workshop", session.title)
        assertEquals("A test workshop session", session.description)
        assertEquals(SessionType.WORKSHOP, session.type)
        assertEquals(startTime, session.startTime)
        assertEquals(endTime, session.endTime)
        assertEquals("Main Building", session.location)
        assertEquals("Room A", session.room)
        assertEquals(25, session.capacity)
        assertEquals("John Doe", session.presenter)
    }

    @Test
    fun `should handle tags collection`() {
        val session = Session()
        session.tags = mutableListOf("technical", "hands-on", "beginner")
        
        assertEquals(3, session.tags?.size)
        assertTrue(session.tags?.contains("technical") == true)
        assertTrue(session.tags?.contains("hands-on") == true)
        assertTrue(session.tags?.contains("beginner") == true)
    }
}