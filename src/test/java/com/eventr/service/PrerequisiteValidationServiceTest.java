package com.eventr.service;

import com.eventr.model.*;
import com.eventr.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PrerequisiteValidation Service Tests")
public class PrerequisiteValidationServiceTest {

    @Mock
    private SessionPrerequisiteRepository mockSessionPrerequisiteRepository;
    
    @Mock
    private SessionRepository mockSessionRepository;
    
    @Mock
    private RegistrationRepository mockRegistrationRepository;
    
    @Mock
    private SessionRegistrationRepository mockSessionRegistrationRepository;
    
    @Mock
    private CheckInRepository mockCheckInRepository;
    
    private PrerequisiteValidationService prerequisiteValidationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        prerequisiteValidationService = new PrerequisiteValidationService(
            mockSessionPrerequisiteRepository,
            mockSessionRepository,
            mockRegistrationRepository,
            mockSessionRegistrationRepository,
            mockCheckInRepository
        );
    }

    @Test
    @DisplayName("Should validate prerequisites when all are met")
    void shouldValidatePrerequisitesWhenAllAreMet() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        
        Session session = new Session();
        session.setId(sessionId);
        session.setName("Advanced Workshop");
        
        Registration registration = new Registration();
        registration.setId(registrationId);
        registration.setUserEmail("test@example.com");
        
        when(mockSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mockRegistrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(mockSessionPrerequisiteRepository.findBySessionId(sessionId)).thenReturn(Collections.emptyList());

        // When
        var result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId);

        // Then
        assertTrue(result.isValid());
        assertTrue(result.getUnmetPrerequisites().isEmpty());
        verify(mockSessionRepository).findById(sessionId);
        verify(mockRegistrationRepository).findById(registrationId);
    }

    @Test
    @DisplayName("Should return invalid when session not found")
    void shouldReturnInvalidWhenSessionNotFound() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        
        when(mockSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // When
        var result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId);

        // Then
        assertFalse(result.isValid());
        verify(mockSessionRepository).findById(sessionId);
        verify(mockRegistrationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should return invalid when registration not found")
    void shouldReturnInvalidWhenRegistrationNotFound() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        
        Session session = new Session();
        session.setId(sessionId);
        
        when(mockSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mockRegistrationRepository.findById(registrationId)).thenReturn(Optional.empty());

        // When
        var result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId);

        // Then
        assertFalse(result.isValid());
        verify(mockSessionRepository).findById(sessionId);
        verify(mockRegistrationRepository).findById(registrationId);
    }

    @Test
    @DisplayName("Should detect unmet session prerequisites")
    void shouldDetectUnmetSessionPrerequisites() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        UUID prerequisiteSessionId = UUID.randomUUID();
        
        Session session = new Session();
        session.setId(sessionId);
        
        Session prerequisiteSession = new Session();
        prerequisiteSession.setId(prerequisiteSessionId);
        prerequisiteSession.setName("Basic Workshop");
        
        Registration registration = new Registration();
        registration.setId(registrationId);
        
        SessionPrerequisite prerequisite = new SessionPrerequisite();
        prerequisite.setSessionId(sessionId);
        prerequisite.setPrerequisiteType(PrerequisiteType.PREVIOUS_SESSION);
        prerequisite.setPrerequisiteSessionId(prerequisiteSessionId);
        
        when(mockSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mockRegistrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(mockSessionPrerequisiteRepository.findBySessionId(sessionId))
                .thenReturn(Arrays.asList(prerequisite));
        when(mockSessionRepository.findById(prerequisiteSessionId))
                .thenReturn(Optional.of(prerequisiteSession));
        when(mockSessionRegistrationRepository.findBySessionIdAndRegistrationId(prerequisiteSessionId, registrationId))
                .thenReturn(Optional.empty()); // No registration for prerequisite session

        // When
        var result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId);

        // Then
        assertFalse(result.isValid());
        assertEquals(1, result.getUnmetPrerequisites().size());
        assertTrue(result.getUnmetPrerequisites().get(0).getDescription()
                .contains("Basic Workshop"));
    }

    @Test
    @DisplayName("Should validate check-in prerequisites")
    void shouldValidateCheckInPrerequisites() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        UUID prerequisiteSessionId = UUID.randomUUID();
        
        Session session = new Session();
        session.setId(sessionId);
        
        Registration registration = new Registration();
        registration.setId(registrationId);
        
        SessionPrerequisite prerequisite = new SessionPrerequisite();
        prerequisite.setSessionId(sessionId);
        prerequisite.setPrerequisiteType(PrerequisiteType.CHECKIN_REQUIRED);
        prerequisite.setPrerequisiteSessionId(prerequisiteSessionId);
        
        when(mockSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mockRegistrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(mockSessionPrerequisiteRepository.findBySessionId(sessionId))
                .thenReturn(Arrays.asList(prerequisite));
        when(mockCheckInRepository.existsBySessionIdAndRegistrationId(prerequisiteSessionId, registrationId))
                .thenReturn(true); // Check-in exists

        // When
        var result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId);

        // Then
        assertTrue(result.isValid());
        assertTrue(result.getUnmetPrerequisites().isEmpty());
    }

    @Test
    @DisplayName("Should detect missing check-in prerequisite")
    void shouldDetectMissingCheckInPrerequisite() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        UUID prerequisiteSessionId = UUID.randomUUID();
        
        Session session = new Session();
        session.setId(sessionId);
        
        Registration registration = new Registration();
        registration.setId(registrationId);
        
        SessionPrerequisite prerequisite = new SessionPrerequisite();
        prerequisite.setSessionId(sessionId);
        prerequisite.setPrerequisiteType(PrerequisiteType.CHECKIN_REQUIRED);
        prerequisite.setPrerequisiteSessionId(prerequisiteSessionId);
        
        when(mockSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mockRegistrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(mockSessionPrerequisiteRepository.findBySessionId(sessionId))
                .thenReturn(Arrays.asList(prerequisite));
        when(mockCheckInRepository.existsBySessionIdAndRegistrationId(prerequisiteSessionId, registrationId))
                .thenReturn(false); // No check-in

        // When
        var result = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId);

        // Then
        assertFalse(result.isValid());
        assertEquals(1, result.getUnmetPrerequisites().size());
        assertTrue(result.getUnmetPrerequisites().get(0).getDescription()
                .contains("check-in required"));
    }

    @Test
    @DisplayName("Should get session prerequisites")
    void shouldGetSessionPrerequisites() {
        // Given
        UUID sessionId = UUID.randomUUID();
        
        SessionPrerequisite prerequisite1 = new SessionPrerequisite();
        prerequisite1.setSessionId(sessionId);
        prerequisite1.setPrerequisiteType(PrerequisiteType.PREVIOUS_SESSION);
        
        SessionPrerequisite prerequisite2 = new SessionPrerequisite();
        prerequisite2.setSessionId(sessionId);
        prerequisite2.setPrerequisiteType(PrerequisiteType.CHECKIN_REQUIRED);
        
        when(mockSessionPrerequisiteRepository.findBySessionId(sessionId))
                .thenReturn(Arrays.asList(prerequisite1, prerequisite2));

        // When
        var result = prerequisiteValidationService.getSessionPrerequisites(sessionId);

        // Then
        assertEquals(2, result.size());
        verify(mockSessionPrerequisiteRepository).findBySessionId(sessionId);
    }

    @Test
    @DisplayName("Should add session prerequisite")
    void shouldAddSessionPrerequisite() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID prerequisiteSessionId = UUID.randomUUID();
        
        SessionPrerequisite prerequisite = new SessionPrerequisite();
        prerequisite.setSessionId(sessionId);
        prerequisite.setPrerequisiteType(PrerequisiteType.PREVIOUS_SESSION);
        prerequisite.setPrerequisiteSessionId(prerequisiteSessionId);
        
        SessionPrerequisite savedPrerequisite = new SessionPrerequisite();
        savedPrerequisite.setId(UUID.randomUUID());
        savedPrerequisite.setSessionId(sessionId);
        savedPrerequisite.setPrerequisiteType(PrerequisiteType.PREVIOUS_SESSION);
        savedPrerequisite.setPrerequisiteSessionId(prerequisiteSessionId);
        
        when(mockSessionPrerequisiteRepository.save(any(SessionPrerequisite.class)))
                .thenReturn(savedPrerequisite);

        // When
        var result = prerequisiteValidationService.addSessionPrerequisite(sessionId, prerequisiteSessionId, PrerequisiteType.PREVIOUS_SESSION);

        // Then
        assertNotNull(result);
        assertEquals(sessionId, result.getSessionId());
        assertEquals(prerequisiteSessionId, result.getPrerequisiteSessionId());
        assertEquals(PrerequisiteType.PREVIOUS_SESSION, result.getPrerequisiteType());
        verify(mockSessionPrerequisiteRepository).save(any(SessionPrerequisite.class));
    }

    @Test
    @DisplayName("Should remove session prerequisite")
    void shouldRemoveSessionPrerequisite() {
        // Given
        UUID prerequisiteId = UUID.randomUUID();
        
        SessionPrerequisite prerequisite = new SessionPrerequisite();
        prerequisite.setId(prerequisiteId);
        
        when(mockSessionPrerequisiteRepository.findById(prerequisiteId))
                .thenReturn(Optional.of(prerequisite));

        // When
        boolean result = prerequisiteValidationService.removeSessionPrerequisite(prerequisiteId);

        // Then
        assertTrue(result);
        verify(mockSessionPrerequisiteRepository).findById(prerequisiteId);
        verify(mockSessionPrerequisiteRepository).delete(prerequisite);
    }

    @Test
    @DisplayName("Should return false when removing non-existent prerequisite")
    void shouldReturnFalseWhenRemovingNonExistentPrerequisite() {
        // Given
        UUID prerequisiteId = UUID.randomUUID();
        
        when(mockSessionPrerequisiteRepository.findById(prerequisiteId))
                .thenReturn(Optional.empty());

        // When
        boolean result = prerequisiteValidationService.removeSessionPrerequisite(prerequisiteId);

        // Then
        assertFalse(result);
        verify(mockSessionPrerequisiteRepository).findById(prerequisiteId);
        verify(mockSessionPrerequisiteRepository, never()).delete(any());
    }
}