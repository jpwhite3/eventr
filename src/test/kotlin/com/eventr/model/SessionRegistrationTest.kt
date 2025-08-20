package com.eventr.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class SessionRegistrationTest {

    @Test
    fun `should create session registration with default values`() {
        val sessionRegistration = SessionRegistration()
        
        assertEquals(SessionRegistrationStatus.REGISTERED, sessionRegistration.status)
        assertNotNull(sessionRegistration.registeredAt)
        assertFalse(sessionRegistration.attendanceVerified)
        assertNotNull(sessionRegistration.updatedAt)
        assertNull(sessionRegistration.checkedInAt)
        assertNull(sessionRegistration.cancelledAt)
    }

    @Test
    fun `should create waitlist registration`() {
        val sessionRegistration = SessionRegistration(
            status = SessionRegistrationStatus.WAITLIST,
            waitlistPosition = 5,
            waitlistRegisteredAt = LocalDateTime.now()
        )
        
        assertEquals(SessionRegistrationStatus.WAITLIST, sessionRegistration.status)
        assertEquals(5, sessionRegistration.waitlistPosition)
        assertNotNull(sessionRegistration.waitlistRegisteredAt)
    }

    @Test
    fun `should handle attendance verification`() {
        val sessionRegistration = SessionRegistration()
        
        sessionRegistration.status = SessionRegistrationStatus.ATTENDED
        sessionRegistration.checkedInAt = LocalDateTime.now()
        sessionRegistration.attendanceVerified = true
        sessionRegistration.verificationMethod = "QR_CODE"
        
        assertEquals(SessionRegistrationStatus.ATTENDED, sessionRegistration.status)
        assertNotNull(sessionRegistration.checkedInAt)
        assertTrue(sessionRegistration.attendanceVerified)
        assertEquals("QR_CODE", sessionRegistration.verificationMethod)
    }

    @Test
    fun `should handle session feedback`() {
        val sessionRegistration = SessionRegistration()
        
        sessionRegistration.rating = 5
        sessionRegistration.feedback = "Excellent session, very informative!"
        
        assertEquals(5, sessionRegistration.rating)
        assertEquals("Excellent session, very informative!", sessionRegistration.feedback)
    }
}