package com.eventr.service;

import com.eventr.dto.SessionCreateDto;
import com.eventr.dto.SessionDto;
import com.eventr.model.Event;
import com.eventr.model.EventStatus;
import com.eventr.model.Session;
import com.eventr.repository.EventRepository;
import com.eventr.repository.RegistrationRepository;
import com.eventr.repository.SessionRegistrationRepository;
import com.eventr.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Session Service Tests")
public class SessionServiceTest {

    @Mock
    private SessionRepository mockSessionRepository;
    
    @Mock
    private SessionRegistrationRepository mockSessionRegistrationRepository;
    
    @Mock
    private EventRepository mockEventRepository;
    
    @Mock
    private RegistrationRepository mockRegistrationRepository;
    
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sessionService = new SessionService(
            mockSessionRepository,
            mockSessionRegistrationRepository,
            mockEventRepository,
            mockRegistrationRepository
        );
    }

    @Test
    @DisplayName("Should create session successfully")
    void shouldCreateSessionSuccessfully() {
        // Given
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        event.setName("Test Event");
        event.setStatus(EventStatus.PUBLISHED);
        
        SessionCreateDto createDto = new SessionCreateDto();
        createDto.setEventId(eventId);
        createDto.setName("Test Session");
        createDto.setDescription("Test Description");
        createDto.setStartTime(LocalDateTime.of(2024, 12, 15, 10, 0));
        createDto.setEndTime(LocalDateTime.of(2024, 12, 15, 11, 0));
        createDto.setRoom("Room A");
        createDto.setPresenter("John Doe");
        
        Session savedSession = new Session();
        savedSession.setId(UUID.randomUUID());
        savedSession.setName(createDto.getName());
        savedSession.setEvent(event);
        
        when(mockEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(mockSessionRepository.findConflictingSessions(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(mockSessionRepository.findSessionsByPresenterAndTimeConflict(any(), any(), any())).thenReturn(Collections.emptyList());
        when(mockSessionRepository.save(any(Session.class))).thenReturn(savedSession);

        // When
        SessionDto result = sessionService.createSession(createDto);

        // Then
        assertNotNull(result);
        assertEquals(createDto.getName(), result.getName());
        verify(mockEventRepository).findById(eventId);
        verify(mockSessionRepository).save(any(Session.class));
    }

    @Test
    @DisplayName("Should throw exception when event not found")
    void shouldThrowExceptionWhenEventNotFound() {
        // Given
        UUID eventId = UUID.randomUUID();
        SessionCreateDto createDto = new SessionCreateDto();
        createDto.setEventId(eventId);
        
        when(mockEventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                sessionService.createSession(createDto)
        );
        
        assertEquals("Event not found", exception.getMessage());
        verify(mockEventRepository).findById(eventId);
        verify(mockSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when room has conflict")
    void shouldThrowExceptionWhenRoomHasConflict() {
        // Given
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        
        SessionCreateDto createDto = new SessionCreateDto();
        createDto.setEventId(eventId);
        createDto.setRoom("Room A");
        createDto.setStartTime(LocalDateTime.of(2024, 12, 15, 10, 0));
        createDto.setEndTime(LocalDateTime.of(2024, 12, 15, 11, 0));
        
        Session conflictingSession = new Session();
        conflictingSession.setId(UUID.randomUUID());
        
        when(mockEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(mockSessionRepository.findConflictingSessions(eventId, "Room A", createDto.getStartTime(), createDto.getEndTime()))
                .thenReturn(Collections.singletonList(conflictingSession));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                sessionService.createSession(createDto)
        );
        
        assertEquals("Room 'Room A' is already booked for this time slot", exception.getMessage());
        verify(mockSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when presenter has conflict")
    void shouldThrowExceptionWhenPresenterHasConflict() {
        // Given
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        
        SessionCreateDto createDto = new SessionCreateDto();
        createDto.setEventId(eventId);
        createDto.setPresenter("John Doe");
        createDto.setStartTime(LocalDateTime.of(2024, 12, 15, 10, 0));
        createDto.setEndTime(LocalDateTime.of(2024, 12, 15, 11, 0));
        
        Session conflictingSession = new Session();
        conflictingSession.setId(UUID.randomUUID());
        
        when(mockEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(mockSessionRepository.findConflictingSessions(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(mockSessionRepository.findSessionsByPresenterAndTimeConflict("John Doe", createDto.getStartTime(), createDto.getEndTime()))
                .thenReturn(Collections.singletonList(conflictingSession));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                sessionService.createSession(createDto)
        );
        
        assertEquals("Presenter 'John Doe' has a conflict at this time", exception.getMessage());
        verify(mockSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create session without room and presenter")
    void shouldCreateSessionWithoutRoomAndPresenter() {
        // Given
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        
        SessionCreateDto createDto = new SessionCreateDto();
        createDto.setEventId(eventId);
        createDto.setName("Test Session");
        createDto.setStartTime(LocalDateTime.of(2024, 12, 15, 10, 0));
        createDto.setEndTime(LocalDateTime.of(2024, 12, 15, 11, 0));
        // room and presenter are null
        
        Session savedSession = new Session();
        savedSession.setId(UUID.randomUUID());
        savedSession.setName(createDto.getName());
        savedSession.setEvent(event);
        
        when(mockEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(mockSessionRepository.save(any(Session.class))).thenReturn(savedSession);

        // When
        SessionDto result = sessionService.createSession(createDto);

        // Then
        assertNotNull(result);
        verify(mockSessionRepository, never()).findConflictingSessions(any(), any(), any(), any());
        verify(mockSessionRepository, never()).findSessionsByPresenterAndTimeConflict(any(), any(), any());
        verify(mockSessionRepository).save(any(Session.class));
    }
}