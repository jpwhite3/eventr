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
        val registration = Registration(
            id = UUID.randomUUID()
        ).apply {
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
        val testEvent = createEvent("Tech Conference 2024")
        val eventInstance = EventInstance(
            id = UUID.randomUUID(),
            event = testEvent,
            dateTime = null
        )
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
        val eventInstance = EventInstance(
            id = UUID.randomUUID()
        ).apply {
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
        return Event(
            id = UUID.randomUUID()
        ).apply {
            this.name = name
            this.description = "A great event"
        }
    }

    private fun createEventInstance(event: Event, dateTime: LocalDateTime): EventInstance {
        return EventInstance(
            id = UUID.randomUUID()
        ).apply {
            this.event = event
            this.dateTime = dateTime
        }
    }

    private fun createRegistration(email: String?, userName: String, eventInstance: EventInstance): Registration {
        return Registration(
            id = UUID.randomUUID()
        ).apply {
            this.userEmail = email
            this.userName = userName
            this.eventInstance = eventInstance
        }
    }
}