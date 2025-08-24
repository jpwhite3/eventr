package com.eventr.service;

import com.eventr.dto.CheckInCreateDto;
import com.eventr.dto.CheckInDto;
import com.eventr.dto.QRCheckInDto;
import com.eventr.model.*;
import com.eventr.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CheckIn Service Tests")
public class CheckInServiceTest {

    @Mock
    private CheckInRepository mockCheckInRepository;
    
    @Mock
    private RegistrationRepository mockRegistrationRepository;
    
    @Mock
    private SessionRepository mockSessionRepository;
    
    @Mock
    private EventRepository mockEventRepository;
    
    @Mock
    private QRCodeService mockQRCodeService;
    
    private CheckInService checkInService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        checkInService = new CheckInService(
            mockCheckInRepository,
            mockRegistrationRepository,
            mockSessionRepository,
            mockEventRepository,
            mockQRCodeService
        );
    }

    @Test
    @DisplayName("Should perform manual check-in successfully")
    void shouldPerformManualCheckInSuccessfully() {
        // Given
        UUID registrationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        
        Event event = new Event();
        event.setId(eventId);
        event.setName("Test Event");
        
        EventInstance eventInstance = new EventInstance();
        eventInstance.setId(UUID.randomUUID());
        eventInstance.setEvent(event);
        eventInstance.setDateTime(LocalDateTime.now().plusDays(1));
        
        Registration registration = new Registration();
        registration.setId(registrationId);
        registration.setUserEmail("test@example.com");
        registration.setEventInstance(eventInstance);
        
        CheckInCreateDto createDto = new CheckInCreateDto();
        createDto.setRegistrationId(registrationId);
        
        CheckIn savedCheckIn = new CheckIn();
        savedCheckIn.setId(UUID.randomUUID());
        savedCheckIn.setRegistration(registration);
        savedCheckIn.setCheckedInAt(LocalDateTime.now());
        
        when(mockRegistrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(mockCheckInRepository.save(any(CheckIn.class))).thenReturn(savedCheckIn);

        // When
        CheckInDto result = checkInService.manualCheckIn(createDto);

        // Then
        assertNotNull(result);
        verify(mockRegistrationRepository).findById(registrationId);
        verify(mockCheckInRepository).save(any(CheckIn.class));
    }

    @Test
    @DisplayName("Should throw exception when registration not found for manual check-in")
    void shouldThrowExceptionWhenRegistrationNotFoundForManualCheckIn() {
        // Given
        UUID registrationId = UUID.randomUUID();
        
        CheckInCreateDto createDto = new CheckInCreateDto();
        createDto.setRegistrationId(registrationId);
        
        when(mockRegistrationRepository.findById(registrationId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                checkInService.manualCheckIn(createDto)
        );
        
        assertEquals("Registration not found", exception.getMessage());
        verify(mockRegistrationRepository).findById(registrationId);
        verify(mockCheckInRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid QR code signature")
    void shouldThrowExceptionForInvalidQRCodeSignature() {
        // Given
        QRCheckInDto qrCheckInDto = new QRCheckInDto();
        qrCheckInDto.setQrCode("https://test.com/checkin/event/123?user=456&t=1234567890&sig=invalid");
        
        when(mockQRCodeService.validateQRSignature(any(), any(), any(), any(), any())).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                checkInService.checkInWithQR(qrCheckInDto)
        );
        
        assertEquals("Invalid or expired QR code", exception.getMessage());
        verify(mockQRCodeService).validateQRSignature(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle QR check-in with valid signature")
    void shouldHandleQRCheckInWithValidSignature() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Event event = new Event();
        event.setId(eventId);
        event.setName("Test Event");
        
        EventInstance eventInstance = new EventInstance();
        eventInstance.setId(UUID.randomUUID());
        eventInstance.setEvent(event);
        eventInstance.setDateTime(LocalDateTime.now().plusDays(1));
        
        Registration registration = new Registration();
        registration.setId(UUID.randomUUID());
        registration.setUserEmail("test@example.com");
        registration.setEventInstance(eventInstance);
        
        QRCheckInDto qrCheckInDto = new QRCheckInDto();
        qrCheckInDto.setQrCode("https://test.com/checkin/event/" + eventId + "?user=" + userId + "&t=1234567890&sig=valid");
        
        CheckIn savedCheckIn = new CheckIn();
        savedCheckIn.setId(UUID.randomUUID());
        savedCheckIn.setRegistration(registration);
        savedCheckIn.setCheckedInAt(LocalDateTime.now());
        
        when(mockQRCodeService.validateQRSignature("event", eventId.toString(), userId.toString(), "1234567890", "valid")).thenReturn(true);
        when(mockEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(mockRegistrationRepository.findByEventInstanceAndUserEmail(any(), eq("test@example.com"))).thenReturn(Optional.of(registration));
        when(mockCheckInRepository.save(any(CheckIn.class))).thenReturn(savedCheckIn);

        // When
        assertDoesNotThrow(() -> checkInService.checkInWithQR(qrCheckInDto));

        // Then
        verify(mockQRCodeService).validateQRSignature("event", eventId.toString(), userId.toString(), "1234567890", "valid");
    }

    @Test
    @DisplayName("Should throw exception for invalid QR code type")
    void shouldThrowExceptionForInvalidQRCodeType() {
        // Given
        QRCheckInDto qrCheckInDto = new QRCheckInDto();
        qrCheckInDto.setQrCode("https://test.com/checkin/invalid/123?user=456&t=1234567890&sig=valid");
        
        when(mockQRCodeService.validateQRSignature(any(), any(), any(), any(), any())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                checkInService.checkInWithQR(qrCheckInDto)
        );
        
        assertEquals("Invalid QR code type", exception.getMessage());
    }
}