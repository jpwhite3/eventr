package com.eventr.service;

import com.eventr.dto.SessionCapacityDto;
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

@DisplayName("CapacityManagement Service Tests")
public class CapacityManagementServiceTest {

    @Mock
    private SessionCapacityRepository mockSessionCapacityRepository;
    
    @Mock
    private SessionRepository mockSessionRepository;
    
    @Mock
    private SessionRegistrationRepository mockSessionRegistrationRepository;
    
    @Mock
    private RegistrationRepository mockRegistrationRepository;
    
    @Mock
    private EventRepository mockEventRepository;
    
    private CapacityManagementService capacityManagementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        capacityManagementService = new CapacityManagementService(
            mockSessionCapacityRepository,
            mockSessionRepository,
            mockSessionRegistrationRepository,
            mockRegistrationRepository,
            mockEventRepository
        );
    }

    @Test
    @DisplayName("Should create session capacity successfully")
    void shouldCreateSessionCapacitySuccessfully() {
        // Given
        UUID sessionId = UUID.randomUUID();
        
        Session session = new Session();
        session.setId(sessionId);
        session.setTitle("Test Session");
        
        SessionCapacityDto capacityDto = new SessionCapacityDto();
        capacityDto.setCapacityType(CapacityType.FIXED);
        capacityDto.setMaximumCapacity(100);
        capacityDto.setMinimumCapacity(10);
        capacityDto.setEnableWaitlist(true);
        capacityDto.setWaitlistCapacity(20);
        capacityDto.setAllowOverbooking(false);
        capacityDto.setAutoPromoteFromWaitlist(true);
        
        SessionCapacity savedCapacity = new SessionCapacity();
        savedCapacity.setId(UUID.randomUUID());
        savedCapacity.setSession(session);
        savedCapacity.setCapacityType(capacityDto.getCapacityType());
        savedCapacity.setMaximumCapacity(capacityDto.getMaximumCapacity());
        
        when(mockSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mockSessionCapacityRepository.findBySessionId(sessionId)).thenReturn(null);
        when(mockSessionCapacityRepository.save(any(SessionCapacity.class))).thenReturn(savedCapacity);
        when(mockSessionRegistrationRepository.countBySessionIdAndStatus(eq(sessionId), eq(SessionRegistrationStatus.CONFIRMED)))
                .thenReturn(0L);

        // When
        SessionCapacityDto result = capacityManagementService.createSessionCapacity(sessionId, capacityDto);

        // Then
        assertNotNull(result);
        assertEquals(CapacityType.FIXED, result.getCapacityType());
        assertEquals(100, result.getMaximumCapacity());
        verify(mockSessionRepository).findById(sessionId);
        verify(mockSessionCapacityRepository).save(any(SessionCapacity.class));
    }

    @Test
    @DisplayName("Should throw exception when session not found")
    void shouldThrowExceptionWhenSessionNotFound() {
        // Given
        UUID sessionId = UUID.randomUUID();
        SessionCapacityDto capacityDto = new SessionCapacityDto();
        capacityDto.setCapacityType(CapacityType.FIXED);
        capacityDto.setMaximumCapacity(100);
        
        when(mockSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                capacityManagementService.createSessionCapacity(sessionId, capacityDto)
        );
        
        assertEquals("Session not found", exception.getMessage());
        verify(mockSessionRepository).findById(sessionId);
        verify(mockSessionCapacityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when capacity already exists")
    void shouldThrowExceptionWhenCapacityAlreadyExists() {
        // Given
        UUID sessionId = UUID.randomUUID();
        Session session = new Session();
        session.setId(sessionId);
        
        SessionCapacityDto capacityDto = new SessionCapacityDto();
        capacityDto.setCapacityType(CapacityType.FIXED);
        capacityDto.setMaximumCapacity(100);
        
        SessionCapacity existingCapacity = new SessionCapacity();
        existingCapacity.setSession(session);
        
        when(mockSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mockSessionCapacityRepository.findBySessionId(sessionId)).thenReturn(existingCapacity);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                capacityManagementService.createSessionCapacity(sessionId, capacityDto)
        );
        
        assertEquals("Capacity configuration already exists for this session", exception.getMessage());
        verify(mockSessionCapacityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get session capacity by session ID")
    void shouldGetSessionCapacityBySessionId() {
        // Given
        UUID sessionId = UUID.randomUUID();
        Session session = new Session();
        session.setId(sessionId);
        
        SessionCapacity capacity = new SessionCapacity();
        capacity.setId(UUID.randomUUID());
        capacity.setSession(session);
        capacity.setCapacityType(CapacityType.FIXED);
        capacity.setMaximumCapacity(50);
        capacity.setCurrentRegistered(25);
        
        when(mockSessionCapacityRepository.findBySessionId(sessionId)).thenReturn(capacity);

        // When
        Optional<SessionCapacityDto> result = capacityManagementService.getSessionCapacity(sessionId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(CapacityType.FIXED, result.get().getCapacityType());
        assertEquals(50, result.get().getMaximumCapacity());
        assertEquals(25, result.get().getCurrentRegistered());
        verify(mockSessionCapacityRepository).findBySessionId(sessionId);
    }

    @Test
    @DisplayName("Should return empty when session capacity not found")
    void shouldReturnEmptyWhenSessionCapacityNotFound() {
        // Given
        UUID sessionId = UUID.randomUUID();
        when(mockSessionCapacityRepository.findBySessionId(sessionId)).thenReturn(null);

        // When
        Optional<SessionCapacityDto> result = capacityManagementService.getSessionCapacity(sessionId);

        // Then
        assertFalse(result.isPresent());
        verify(mockSessionCapacityRepository).findBySessionId(sessionId);
    }

    @Test
    @DisplayName("Should check availability for session")
    void shouldCheckAvailabilityForSession() {
        // Given
        UUID sessionId = UUID.randomUUID();
        Session session = new Session();
        session.setId(sessionId);
        
        SessionCapacity capacity = new SessionCapacity();
        capacity.setSession(session);
        capacity.setCapacityType(CapacityType.FIXED);
        capacity.setMaximumCapacity(100);
        capacity.setCurrentRegistered(75);
        capacity.setEnableWaitlist(true);
        capacity.setWaitlistCapacity(25);
        capacity.setCurrentWaitlisted(10);
        
        when(mockSessionCapacityRepository.findBySessionId(sessionId)).thenReturn(capacity);

        // When
        var result = capacityManagementService.checkAvailability(sessionId);

        // Then
        assertNotNull(result);
        assertTrue(result.isAvailable());
        assertEquals(25, result.getAvailableSlots()); // 100 - 75
        assertEquals(15, result.getWaitlistSlots()); // 25 - 10
        verify(mockSessionCapacityRepository).findBySessionId(sessionId);
    }

    @Test
    @DisplayName("Should update capacity counts")
    void shouldUpdateCapacityCounts() {
        // Given
        UUID sessionId = UUID.randomUUID();
        Session session = new Session();
        session.setId(sessionId);
        
        SessionCapacity capacity = new SessionCapacity();
        capacity.setSession(session);
        capacity.setMaximumCapacity(100);
        
        when(mockSessionCapacityRepository.findBySessionId(sessionId)).thenReturn(capacity);
        when(mockSessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.CONFIRMED))
                .thenReturn(45L);
        when(mockSessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.WAITLISTED))
                .thenReturn(15L);
        when(mockSessionRegistrationRepository.countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.PENDING))
                .thenReturn(5L);
        when(mockSessionCapacityRepository.save(any(SessionCapacity.class))).thenReturn(capacity);

        // When
        capacityManagementService.updateCapacityCounts(sessionId);

        // Then
        verify(mockSessionCapacityRepository).findBySessionId(sessionId);
        verify(mockSessionRegistrationRepository).countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.CONFIRMED);
        verify(mockSessionRegistrationRepository).countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.WAITLISTED);
        verify(mockSessionRegistrationRepository).countBySessionIdAndStatus(sessionId, SessionRegistrationStatus.PENDING);
        verify(mockSessionCapacityRepository).save(capacity);
    }

    @Test
    @DisplayName("Should handle capacity optimization suggestions")
    void shouldHandleCapacityOptimizationSuggestions() {
        // Given
        UUID eventId = UUID.randomUUID();
        when(mockSessionCapacityRepository.findCapacitiesByEventId(eventId))
                .thenReturn(Collections.emptyList());

        // When
        var result = capacityManagementService.getCapacityOptimizationSuggestions(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockSessionCapacityRepository).findCapacitiesByEventId(eventId);
    }
}