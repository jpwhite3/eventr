package com.eventr.service;

import com.eventr.dto.ResourceCreateDto;
import com.eventr.dto.ResourceDto;
import com.eventr.model.Resource;
import com.eventr.model.ResourceType;
import com.eventr.repository.ResourceRepository;
import com.eventr.repository.SessionRepository;
import com.eventr.repository.SessionResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ResourceManagement Service Tests")
public class ResourceManagementServiceTest {

    @Mock
    private ResourceRepository mockResourceRepository;
    
    @Mock
    private SessionResourceRepository mockSessionResourceRepository;
    
    @Mock
    private SessionRepository mockSessionRepository;
    
    private ResourceManagementService resourceManagementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resourceManagementService = new ResourceManagementService(
            mockResourceRepository,
            mockSessionResourceRepository,
            mockSessionRepository
        );
    }

    @Test
    @DisplayName("Should create resource successfully")
    void shouldCreateResourceSuccessfully() {
        // Given
        ResourceCreateDto createDto = new ResourceCreateDto();
        createDto.setName("Conference Room A");
        createDto.setDescription("Large conference room");
        createDto.setType(ResourceType.ROOM);
        createDto.setCapacity(50);
        createDto.setLocation("Building A, Floor 2");
        createDto.setFloor(2);
        createDto.setBuilding("Building A");
        createDto.setIsBookable(true);
        createDto.setRequiresApproval(false);
        createDto.setBookingLeadTimeHours(2);
        createDto.setMaxBookingDurationHours(8);
        createDto.setHourlyRate(BigDecimal.valueOf(100.00));
        createDto.setTags(Arrays.asList("conference", "large", "projector"));
        createDto.setCategory("meeting-rooms");
        
        Resource savedResource = new Resource();
        savedResource.setId(UUID.randomUUID());
        savedResource.setName(createDto.getName());
        savedResource.setType(createDto.getType());
        
        when(mockResourceRepository.save(any(Resource.class))).thenReturn(savedResource);

        // When
        ResourceDto result = resourceManagementService.createResource(createDto);

        // Then
        assertNotNull(result);
        assertEquals(createDto.getName(), result.getName());
        verify(mockResourceRepository).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should handle resource with minimal information")
    void shouldHandleResourceWithMinimalInformation() {
        // Given
        ResourceCreateDto createDto = new ResourceCreateDto();
        createDto.setName("Basic Room");
        createDto.setType(ResourceType.ROOM);
        createDto.setIsBookable(true);
        createDto.setTags(Collections.emptyList());
        
        Resource savedResource = new Resource();
        savedResource.setId(UUID.randomUUID());
        savedResource.setName(createDto.getName());
        savedResource.setType(createDto.getType());
        
        when(mockResourceRepository.save(any(Resource.class))).thenReturn(savedResource);

        // When
        ResourceDto result = resourceManagementService.createResource(createDto);

        // Then
        assertNotNull(result);
        assertEquals(createDto.getName(), result.getName());
        verify(mockResourceRepository).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should find resource by id successfully")
    void shouldFindResourceByIdSuccessfully() {
        // Given
        UUID resourceId = UUID.randomUUID();
        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setName("Test Resource");
        resource.setType(ResourceType.EQUIPMENT);
        
        when(mockResourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        // When
        Optional<ResourceDto> result = resourceManagementService.getResourceById(resourceId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Resource", result.get().getName());
        verify(mockResourceRepository).findById(resourceId);
    }

    @Test
    @DisplayName("Should return empty when resource not found")
    void shouldReturnEmptyWhenResourceNotFound() {
        // Given
        UUID resourceId = UUID.randomUUID();
        when(mockResourceRepository.findById(resourceId)).thenReturn(Optional.empty());

        // When
        Optional<ResourceDto> result = resourceManagementService.getResourceById(resourceId);

        // Then
        assertFalse(result.isPresent());
        verify(mockResourceRepository).findById(resourceId);
    }

    @Test
    @DisplayName("Should update resource successfully")
    void shouldUpdateResourceSuccessfully() {
        // Given
        UUID resourceId = UUID.randomUUID();
        Resource existingResource = new Resource();
        existingResource.setId(resourceId);
        existingResource.setName("Old Name");
        existingResource.setType(ResourceType.ROOM);
        
        ResourceCreateDto updateDto = new ResourceCreateDto();
        updateDto.setName("Updated Name");
        updateDto.setDescription("Updated description");
        updateDto.setType(ResourceType.ROOM);
        updateDto.setCapacity(75);
        updateDto.setIsBookable(true);
        updateDto.setTags(Arrays.asList("updated", "new"));
        
        when(mockResourceRepository.findById(resourceId)).thenReturn(Optional.of(existingResource));
        when(mockResourceRepository.save(any(Resource.class))).thenReturn(existingResource);

        // When
        Optional<ResourceDto> result = resourceManagementService.updateResource(resourceId, updateDto);

        // Then
        assertTrue(result.isPresent());
        verify(mockResourceRepository).findById(resourceId);
        verify(mockResourceRepository).save(existingResource);
    }

    @Test
    @DisplayName("Should return empty when updating non-existent resource")
    void shouldReturnEmptyWhenUpdatingNonExistentResource() {
        // Given
        UUID resourceId = UUID.randomUUID();
        ResourceCreateDto updateDto = new ResourceCreateDto();
        updateDto.setName("Updated Name");
        updateDto.setType(ResourceType.ROOM);
        updateDto.setIsBookable(true);
        updateDto.setTags(Collections.emptyList());
        
        when(mockResourceRepository.findById(resourceId)).thenReturn(Optional.empty());

        // When
        Optional<ResourceDto> result = resourceManagementService.updateResource(resourceId, updateDto);

        // Then
        assertFalse(result.isPresent());
        verify(mockResourceRepository).findById(resourceId);
        verify(mockResourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete resource successfully")
    void shouldDeleteResourceSuccessfully() {
        // Given
        UUID resourceId = UUID.randomUUID();
        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setName("Test Resource");
        
        when(mockResourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        // When
        boolean result = resourceManagementService.deleteResource(resourceId);

        // Then
        assertTrue(result);
        verify(mockResourceRepository).findById(resourceId);
        verify(mockResourceRepository).delete(resource);
    }

    @Test
    @DisplayName("Should return false when deleting non-existent resource")
    void shouldReturnFalseWhenDeletingNonExistentResource() {
        // Given
        UUID resourceId = UUID.randomUUID();
        when(mockResourceRepository.findById(resourceId)).thenReturn(Optional.empty());

        // When
        boolean result = resourceManagementService.deleteResource(resourceId);

        // Then
        assertFalse(result);
        verify(mockResourceRepository).findById(resourceId);
        verify(mockResourceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should get all resources")
    void shouldGetAllResources() {
        // Given
        Resource resource1 = new Resource();
        resource1.setId(UUID.randomUUID());
        resource1.setName("Resource 1");
        resource1.setType(ResourceType.ROOM);
        
        Resource resource2 = new Resource();
        resource2.setId(UUID.randomUUID());
        resource2.setName("Resource 2");
        resource2.setType(ResourceType.EQUIPMENT);
        
        when(mockResourceRepository.findAll()).thenReturn(Arrays.asList(resource1, resource2));

        // When
        var result = resourceManagementService.getAllResources();

        // Then
        assertEquals(2, result.size());
        verify(mockResourceRepository).findAll();
    }
}