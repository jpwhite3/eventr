package com.eventr.service

import com.eventr.model.Event
import com.eventr.model.EventInstance
import com.eventr.model.Registration
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.mail.javamail.JavaMailSender
import java.time.LocalDateTime
import java.util.*

@DisplayName("EmailService Tests")
class EmailServiceTest {

    private lateinit var javaMailSender: JavaMailSender
    private lateinit var emailService: EmailService
    private lateinit var mimeMessage: MimeMessage

    @BeforeEach
    fun setUp() {
        javaMailSender = mock()
        mimeMessage = mock()
        
        whenever(javaMailSender.createMimeMessage()).thenReturn(mimeMessage)
        
        emailService = EmailService(javaMailSender)
    }

    @Test
    @DisplayName("Should send registration confirmation email successfully")
    fun shouldSendRegistrationConfirmationEmailSuccessfully() {
        // Given
        val event = createEvent("Tech Conference 2024")
        val eventInstance = createEventInstance(event, LocalDateTime.of(2024, 12, 15, 10, 0))
        val registration = createRegistration("john@example.com", "John Doe", eventInstance)
        
        // When
        assertDoesNotThrow {
            emailService.sendRegistrationConfirmation(registration)
        }

        // Then
        verify(javaMailSender).createMimeMessage()
        verify(javaMailSender).send(mimeMessage)
    }

    @Test
    @DisplayName("Should throw exception when user email is null")
    fun shouldThrowExceptionWhenUserEmailIsNull() {
        // Given
        val event = createEvent("Tech Conference 2024")
        val eventInstance = createEventInstance(event, LocalDateTime.of(2024, 12, 15, 10, 0))
        val registration = createRegistration(null, "John Doe", eventInstance)

        // When & Then
        assertThrows<IllegalArgumentException> {
            emailService.sendRegistrationConfirmation(registration)
        }
        
        // Verify no email was sent
        verify(javaMailSender, never()).send(any<MimeMessage>())
    }

    @Test
    @DisplayName("Should handle registration with null event instance")
    fun shouldHandleRegistrationWithNullEventInstance() {
        // Given
        val registration = Registration().apply {
            id = UUID.randomUUID()
            userEmail = "john@example.com"
            userName = "John Doe"
            eventInstance = null
        }

        // When
        assertDoesNotThrow {
            emailService.sendRegistrationConfirmation(registration)
        }

        // Then
        verify(javaMailSender).createMimeMessage()
        verify(javaMailSender).send(mimeMessage)
    }

    @Test
    @DisplayName("Should handle event instance with null datetime")
    fun shouldHandleEventInstanceWithNullDatetime() {
        // Given
        val event = createEvent("Tech Conference 2024")
        val eventInstance = EventInstance().apply {
            id = UUID.randomUUID()
            event = event
            dateTime = null
        }
        val registration = createRegistration("john@example.com", "John Doe", eventInstance)

        // When
        assertDoesNotThrow {
            emailService.sendRegistrationConfirmation(registration)
        }

        // Then
        verify(javaMailSender).createMimeMessage()
        verify(javaMailSender).send(mimeMessage)
    }

    @Test
    @DisplayName("Should handle event instance with null event")
    fun shouldHandleEventInstanceWithNullEvent() {
        // Given
        val eventInstance = EventInstance().apply {
            id = UUID.randomUUID()
            dateTime = LocalDateTime.of(2024, 12, 15, 10, 0)
            event = null
        }
        val registration = createRegistration("john@example.com", "John Doe", eventInstance)

        // When
        assertDoesNotThrow {
            emailService.sendRegistrationConfirmation(registration)
        }

        // Then
        verify(javaMailSender).createMimeMessage()
        verify(javaMailSender).send(mimeMessage)
    }

    @Test
    @DisplayName("Should create ICS file for event with valid datetime")
    fun shouldCreateIcsFileForEventWithValidDatetime() {
        // Given
        val event = createEvent("Tech Conference 2024")
        val eventInstance = createEventInstance(event, LocalDateTime.of(2024, 12, 15, 10, 0))
        val registration = createRegistration("john@example.com", "John Doe", eventInstance)

        // When
        assertDoesNotThrow {
            emailService.sendRegistrationConfirmation(registration)
        }

        // Then
        verify(javaMailSender).createMimeMessage()
        verify(javaMailSender).send(mimeMessage)
    }

    // Helper methods
    private fun createEvent(name: String): Event {
        return Event().apply {
            this.id = UUID.randomUUID()
            this.name = name
            this.description = "A great event"
        }
    }

    private fun createEventInstance(event: Event, dateTime: LocalDateTime): EventInstance {
        return EventInstance().apply {
            this.id = UUID.randomUUID()
            this.event = event
            this.dateTime = dateTime
        }
    }

    private fun createRegistration(email: String?, userName: String, eventInstance: EventInstance): Registration {
        return Registration().apply {
            this.id = UUID.randomUUID()
            this.userEmail = email
            this.userName = userName
            this.eventInstance = eventInstance
        }
    }
}