package com.eventr.service;

import com.eventr.dto.ScheduleConflictDto;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ConflictDetection Service Tests")
public class ConflictDetectionServiceTest {

    @Mock
    private ScheduleConflictRepository mockScheduleConflictRepository;
    
    @Mock
    private ConflictResolutionRepository mockConflictResolutionRepository;
    
    @Mock
    private SessionRepository mockSessionRepository;
    
    @Mock
    private SessionResourceRepository mockSessionResourceRepository;
    
    @Mock
    private SessionRegistrationRepository mockSessionRegistrationRepository;
    
    @Mock
    private ResourceRepository mockResourceRepository;
    
    @Mock
    private RegistrationRepository mockRegistrationRepository;
    
    private ConflictDetectionService conflictDetectionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        conflictDetectionService = new ConflictDetectionService(
            mockScheduleConflictRepository,
            mockConflictResolutionRepository,
            mockSessionRepository,
            mockSessionResourceRepository,
            mockSessionRegistrationRepository,
            mockResourceRepository,
            mockRegistrationRepository
        );
    }

    @Test
    @DisplayName("Should detect no conflicts when no sessions exist")
    void shouldDetectNoConflictsWhenNoSessionsExist() {
        // Given
        UUID eventId = UUID.randomUUID();
        when(mockSessionRepository.findByEventIdAndIsActiveTrue(eventId))
                .thenReturn(Collections.emptyList());

        // When
        List<ScheduleConflictDto> conflicts = conflictDetectionService.detectAllConflicts(eventId);

        // Then
        assertTrue(conflicts.isEmpty());
        verify(mockSessionRepository).findByEventIdAndIsActiveTrue(eventId);
    }

    @Test
    @DisplayName("Should detect time overlap conflicts between sessions")
    void shouldDetectTimeOverlapConflictsBetweenSessions() {
        // Given
        UUID eventId = UUID.randomUUID();
        
        Session session1 = createSession(
            UUID.randomUUID(),
            "Session 1",
            LocalDateTime.of(2024, 12, 15, 10, 0),
            LocalDateTime.of(2024, 12, 15, 11, 30)
        );
        
        Session session2 = createSession(
            UUID.randomUUID(),
            "Session 2", 
            LocalDateTime.of(2024, 12, 15, 11, 0),
            LocalDateTime.of(2024, 12, 15, 12, 0)
        );
        
        when(mockSessionRepository.findByEventIdAndIsActiveTrue(eventId))
                .thenReturn(Arrays.asList(session1, session2));
        when(mockSessionResourceRepository.findBySessionId(any()))
                .thenReturn(Collections.emptyList());
        when(mockSessionRegistrationRepository.findBySessionId(any()))
                .thenReturn(Collections.emptyList());
        when(mockScheduleConflictRepository.existsByPrimarySessionIdAndSecondarySessionId(any(), any()))
                .thenReturn(false);
        when(mockScheduleConflictRepository.save(any(ScheduleConflict.class)))
                .thenReturn(new ScheduleConflict());

        // When
        List<ScheduleConflictDto> conflicts = conflictDetectionService.detectTimeOverlapConflicts(eventId);

        // Then
        assertFalse(conflicts.isEmpty());
        assertEquals(1, conflicts.size());
        
        ScheduleConflictDto conflict = conflicts.get(0);
        assertEquals(ConflictType.TIME_OVERLAP, conflict.getType());
        assertEquals("Session Time Overlap", conflict.getTitle());
        assertTrue(conflict.getDescription().contains("Session 1"));
        assertTrue(conflict.getDescription().contains("Session 2"));
        assertEquals(session1.getId(), conflict.getPrimarySessionId());
        assertEquals(session2.getId(), conflict.getSecondarySessionId());
    }

    @Test
    @DisplayName("Should not detect conflicts for non-overlapping sessions")
    void shouldNotDetectConflictsForNonOverlappingSessions() {
        // Given
        UUID eventId = UUID.randomUUID();
        
        Session session1 = createSession(
            UUID.randomUUID(),
            "Session 1",
            LocalDateTime.of(2024, 12, 15, 10, 0),
            LocalDateTime.of(2024, 12, 15, 11, 0)
        );
        
        Session session2 = createSession(
            UUID.randomUUID(),
            "Session 2",
            LocalDateTime.of(2024, 12, 15, 11, 30),
            LocalDateTime.of(2024, 12, 15, 12, 30)
        );
        
        when(mockSessionRepository.findByEventIdAndIsActiveTrue(eventId))
                .thenReturn(Arrays.asList(session1, session2));

        // When
        List<ScheduleConflictDto> conflicts = conflictDetectionService.detectTimeOverlapConflicts(eventId);

        // Then
        assertTrue(conflicts.isEmpty());
        verify(mockSessionRepository).findByEventIdAndIsActiveTrue(eventId);
    }

    @Test
    @DisplayName("Should detect resource conflicts")
    void shouldDetectResourceConflicts() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        
        Session session1 = createSession(
            UUID.randomUUID(),
            "Session 1",
            LocalDateTime.of(2024, 12, 15, 10, 0),
            LocalDateTime.of(2024, 12, 15, 11, 0)
        );
        
        Session session2 = createSession(
            UUID.randomUUID(),
            "Session 2",
            LocalDateTime.of(2024, 12, 15, 10, 30),
            LocalDateTime.of(2024, 12, 15, 11, 30)
        );
        
        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setName("Meeting Room A");
        
        SessionResource sessionResource1 = new SessionResource();
        sessionResource1.setSessionId(session1.getId());
        sessionResource1.setResourceId(resourceId);
        
        SessionResource sessionResource2 = new SessionResource();
        sessionResource2.setSessionId(session2.getId());
        sessionResource2.setResourceId(resourceId);
        
        when(mockSessionRepository.findByEventIdAndIsActiveTrue(eventId))
                .thenReturn(Arrays.asList(session1, session2));
        when(mockSessionResourceRepository.findBySessionId(session1.getId()))
                .thenReturn(Arrays.asList(sessionResource1));
        when(mockSessionResourceRepository.findBySessionId(session2.getId()))
                .thenReturn(Arrays.asList(sessionResource2));
        when(mockResourceRepository.findById(resourceId))
                .thenReturn(java.util.Optional.of(resource));
        when(mockSessionRegistrationRepository.findBySessionId(any()))
                .thenReturn(Collections.emptyList());
        when(mockScheduleConflictRepository.existsByPrimarySessionIdAndSecondarySessionId(any(), any()))
                .thenReturn(false);
        when(mockScheduleConflictRepository.save(any(ScheduleConflict.class)))
                .thenReturn(new ScheduleConflict());

        // When
        List<ScheduleConflictDto> conflicts = conflictDetectionService.detectResourceConflicts(eventId);

        // Then
        assertFalse(conflicts.isEmpty());
        assertEquals(1, conflicts.size());
        
        ScheduleConflictDto conflict = conflicts.get(0);
        assertEquals(ConflictType.RESOURCE_CONFLICT, conflict.getType());
        assertTrue(conflict.getDescription().contains("Meeting Room A"));
    }

    @Test
    @DisplayName("Should handle capacity conflicts detection")
    void shouldHandleCapacityConflictsDetection() {
        // Given
        UUID eventId = UUID.randomUUID();
        when(mockSessionRepository.findByEventIdAndIsActiveTrue(eventId))
                .thenReturn(Collections.emptyList());

        // When
        List<ScheduleConflictDto> conflicts = conflictDetectionService.detectCapacityConflicts(eventId);

        // Then
        assertNotNull(conflicts);
        assertTrue(conflicts.isEmpty());
        verify(mockSessionRepository).findByEventIdAndIsActiveTrue(eventId);
    }

    @Test
    @DisplayName("Should handle user conflicts detection")
    void shouldHandleUserConflictsDetection() {
        // Given
        UUID eventId = UUID.randomUUID();
        when(mockSessionRepository.findByEventIdAndIsActiveTrue(eventId))
                .thenReturn(Collections.emptyList());

        // When
        List<ScheduleConflictDto> conflicts = conflictDetectionService.detectUserConflicts(eventId);

        // Then
        assertNotNull(conflicts);
        assertTrue(conflicts.isEmpty());
        verify(mockSessionRepository).findByEventIdAndIsActiveTrue(eventId);
    }

    private Session createSession(UUID id, String title, LocalDateTime startTime, LocalDateTime endTime) {
        Session session = new Session();
        session.setId(id);
        session.setTitle(title);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setIsActive(true);
        return session;
    }
}