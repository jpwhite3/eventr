package com.eventr.service;

import com.eventr.model.Event;
import com.eventr.model.EventInstance;
import com.eventr.model.Registration;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Email Service Tests")
public class EmailServiceTest {

    @Mock
    private JavaMailSender mockMailSender;
    
    @Mock
    private MimeMessage mockMimeMessage;
    
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailService = new EmailService(mockMailSender);
        when(mockMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
    }

    @Test
    @DisplayName("Should send registration confirmation email successfully")
    void shouldSendRegistrationConfirmationEmailSuccessfully() {
        // Given
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Tech Conference 2024");
        
        EventInstance eventInstance = new EventInstance();
        eventInstance.setId(UUID.randomUUID());
        eventInstance.setEvent(event);
        eventInstance.setDateTime(LocalDateTime.of(2024, 12, 15, 9, 0));
        
        Registration registration = new Registration();
        registration.setId(UUID.randomUUID());
        registration.setUserEmail("test@example.com");
        registration.setEventInstance(eventInstance);

        // When
        assertDoesNotThrow(() -> emailService.sendRegistrationConfirmation(registration));

        // Then
        verify(mockMailSender).createMimeMessage();
        verify(mockMailSender).send(mockMimeMessage);
    }

    @Test
    @DisplayName("Should throw exception when user email is null")
    void shouldThrowExceptionWhenUserEmailIsNull() {
        // Given
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Tech Conference 2024");
        
        EventInstance eventInstance = new EventInstance();
        eventInstance.setId(UUID.randomUUID());
        eventInstance.setEvent(event);
        eventInstance.setDateTime(LocalDateTime.of(2024, 12, 15, 9, 0));
        
        Registration registration = new Registration();
        registration.setId(UUID.randomUUID());
        registration.setUserEmail(null);  // This should cause an exception
        registration.setEventInstance(eventInstance);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                emailService.sendRegistrationConfirmation(registration)
        );
        
        assertEquals("User email cannot be null", exception.getMessage());
        verify(mockMailSender).createMimeMessage();
        verify(mockMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle registration with null event instance")
    void shouldHandleRegistrationWithNullEventInstance() {
        // Given
        Registration registration = new Registration();
        registration.setId(UUID.randomUUID());
        registration.setUserEmail("test@example.com");
        registration.setEventInstance(null);

        // When
        assertDoesNotThrow(() -> emailService.sendRegistrationConfirmation(registration));

        // Then
        verify(mockMailSender).createMimeMessage();
        verify(mockMailSender).send(mockMimeMessage);
    }

    @Test
    @DisplayName("Should propagate MessagingException from mail sender")
    void shouldPropagateMessagingExceptionFromMailSender() {
        // Given
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Test Event");
        
        EventInstance eventInstance = new EventInstance();
        eventInstance.setId(UUID.randomUUID());
        eventInstance.setEvent(event);
        eventInstance.setDateTime(LocalDateTime.of(2024, 12, 15, 9, 0));
        
        Registration registration = new Registration();
        registration.setId(UUID.randomUUID());
        registration.setUserEmail("test@example.com");
        registration.setEventInstance(eventInstance);
        
        doThrow(new MessagingException("Mail server error")).when(mockMailSender).send(any(MimeMessage.class));

        // When & Then
        assertThrows(MessagingException.class, () ->
                emailService.sendRegistrationConfirmation(registration)
        );
        
        verify(mockMailSender).createMimeMessage();
    }

    @Test
    @DisplayName("Should handle event instance with null event")
    void shouldHandleEventInstanceWithNullEvent() {
        // Given
        EventInstance eventInstance = new EventInstance();
        eventInstance.setId(UUID.randomUUID());
        eventInstance.setEvent(null);
        eventInstance.setDateTime(LocalDateTime.of(2024, 12, 15, 9, 0));
        
        Registration registration = new Registration();
        registration.setId(UUID.randomUUID());
        registration.setUserEmail("test@example.com");
        registration.setEventInstance(eventInstance);

        // When
        assertDoesNotThrow(() -> emailService.sendRegistrationConfirmation(registration));

        // Then
        verify(mockMailSender).createMimeMessage();
        verify(mockMailSender).send(mockMimeMessage);
    }

    @Test
    @DisplayName("Should handle event instance with null dateTime")
    void shouldHandleEventInstanceWithNullDateTime() {
        // Given
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Test Event");
        
        EventInstance eventInstance = new EventInstance();
        eventInstance.setId(UUID.randomUUID());
        eventInstance.setEvent(event);
        eventInstance.setDateTime(null);
        
        Registration registration = new Registration();
        registration.setId(UUID.randomUUID());
        registration.setUserEmail("test@example.com");
        registration.setEventInstance(eventInstance);

        // When
        assertDoesNotThrow(() -> emailService.sendRegistrationConfirmation(registration));

        // Then
        verify(mockMailSender).createMimeMessage();
        verify(mockMailSender).send(mockMimeMessage);
    }
}