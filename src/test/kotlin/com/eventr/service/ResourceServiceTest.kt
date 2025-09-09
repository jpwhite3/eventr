package com.eventr.service

import com.eventr.model.Resource
import com.eventr.model.ResourceType
import com.eventr.model.ResourceStatus
import com.eventr.repository.ResourceRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ResourceServiceTest {

    @Mock
    private lateinit var resourceRepository: ResourceRepository
    
    private lateinit var resourceService: ResourceService

    @BeforeEach
    fun setUp() {
        resourceService = ResourceService(resourceRepository)
    }

    companion object {
        private val TEST_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        private val NOW = LocalDateTime.now()
        
        fun createTestResource(
            id: UUID = TEST_ID,
            name: String = "Test Conference Room",
            type: ResourceType = ResourceType.ROOM,
            status: ResourceStatus = ResourceStatus.AVAILABLE,
            isActive: Boolean = true,
            isBookable: Boolean = true,
            capacity: Int? = 50,
            hourlyRate: BigDecimal? = BigDecimal.valueOf(100.0),
            location: String? = "Building A, Floor 2",
            contactEmail: String? = "facilities@example.com",
            contactPhone: String? = "+1-555-0123",
            specifications: String? = "Projector, Whiteboard, Conference Table",
            tags: String? = "conference, meeting, presentation",
            createdAt: LocalDateTime = NOW,
            updatedAt: LocalDateTime = NOW
        ): Resource {
            return Resource(
                id = id,
                name = name,
                type = type,
                status = status,
                isActive = isActive,
                isBookable = isBookable,
                capacity = capacity,
                hourlyRate = hourlyRate,
                location = location,
                contactEmail = contactEmail,
                contactPhone = contactPhone,
                specifications = specifications,
                tags = tags,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
        
        fun createCreateResourceDto(
            name: String = "New Conference Room",
            type: String = "ROOM",
            description: String? = "Modern conference room with AV equipment",
            location: String? = "Building B, Floor 1",
            capacity: Int? = 25,
            hourlyRate: Double? = 75.0,
            contactEmail: String? = "booking@example.com",
            contactPhone: String? = "+1-555-0456",
            bookingInstructions: String? = "Book 24 hours in advance"
        ): CreateResourceDto {
            return CreateResourceDto(
                name = name,
                type = type,
                description = description,
                location = location,
                capacity = capacity,
                hourlyRate = hourlyRate,
                features = listOf("projector", "whiteboard"),
                imageUrl = null,
                contactEmail = contactEmail,
                contactPhone = contactPhone,
                bookingInstructions = bookingInstructions
            )
        }
        
        fun createUpdateResourceDto(
            name: String? = "Updated Conference Room",
            type: String? = "ROOM",
            capacity: Int? = 40,
            isAvailable: Boolean? = true,
            hourlyRate: Double? = 85.0
        ): UpdateResourceDto {
            return UpdateResourceDto(
                name = name,
                type = type,
                description = null,
                location = null,
                capacity = capacity,
                hourlyRate = hourlyRate,
                isAvailable = isAvailable,
                features = null,
                imageUrl = null,
                contactEmail = null,
                contactPhone = null,
                bookingInstructions = null
            )
        }
    }

    // ================================================================================
    // CRUD Operations Tests
    // ================================================================================

    @Test
    fun `getAllResources should return active resources as DTOs`() {
        // Arrange
        val activeResources = listOf(
            createTestResource(id = UUID.randomUUID(), name = "Room A"),
            createTestResource(id = UUID.randomUUID(), name = "Room B", type = ResourceType.EQUIPMENT)
        )
        whenever(resourceRepository.findByIsActiveTrue()).thenReturn(activeResources)

        // Act
        val result = resourceService.getAllResources()

        // Assert
        assertEquals(2, result.size)
        assertEquals("Room A", result[0].name)
        assertEquals("Room B", result[1].name)
        assertEquals("ROOM", result[0].type)
        assertEquals("EQUIPMENT", result[1].type)
        assertTrue(result[0].isAvailable)
        verify(resourceRepository).findByIsActiveTrue()
    }

    @Test
    fun `getAllResources should return empty list when no active resources`() {
        // Arrange
        whenever(resourceRepository.findByIsActiveTrue()).thenReturn(emptyList())

        // Act
        val result = resourceService.getAllResources()

        // Assert
        assertEquals(0, result.size)
        verify(resourceRepository).findByIsActiveTrue()
    }

    @Test
    fun `getResourceById should return resource DTO when found`() {
        // Arrange
        val resource = createTestResource()
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.of(resource))

        // Act
        val result = resourceService.getResourceById(TEST_ID)

        // Assert
        assertNotNull(result)
        assertEquals(TEST_ID, result!!.id)
        assertEquals("Test Conference Room", result.name)
        assertEquals("ROOM", result.type)
        assertEquals(50, result.capacity)
        assertEquals(100.0, result.hourlyRate)
        assertTrue(result.isAvailable)
        assertEquals(listOf("conference", "meeting", "presentation"), result.features)
        assertEquals("facilities@example.com", result.contactEmail)
        verify(resourceRepository).findById(TEST_ID)
    }

    @Test
    fun `getResourceById should return null when resource not found`() {
        // Arrange
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.empty())

        // Act
        val result = resourceService.getResourceById(TEST_ID)

        // Assert
        assertNull(result)
        verify(resourceRepository).findById(TEST_ID)
    }

    @Test
    fun `createResource should create and return new resource DTO`() {
        // Arrange
        val createDto = createCreateResourceDto()
        val createdResource = createTestResource(
            name = "New Conference Room",
            type = ResourceType.ROOM,
            capacity = 25,
            hourlyRate = BigDecimal.valueOf(75.0),
            location = "Building B, Floor 1",
            contactEmail = "booking@example.com",
            contactPhone = "+1-555-0456",
            specifications = "Book 24 hours in advance"
        )
        
        whenever(resourceRepository.save(any<Resource>())).thenReturn(createdResource)

        // Act
        val result = resourceService.createResource(createDto)

        // Assert
        assertEquals("New Conference Room", result.name)
        assertEquals("ROOM", result.type)
        assertEquals(25, result.capacity)
        assertEquals(75.0, result.hourlyRate)
        assertEquals("Building B, Floor 1", result.location)
        assertEquals("booking@example.com", result.contactEmail)
        assertEquals("+1-555-0456", result.contactPhone)
        assertEquals("Book 24 hours in advance", result.bookingInstructions)
        
        verify(resourceRepository).save(any<Resource>())
    }

    @Test
    fun `createResource should handle invalid ResourceType`() {
        // Arrange
        val createDto = createCreateResourceDto(type = "INVALID_TYPE")

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            resourceService.createResource(createDto)
        }
    }

    @Test
    fun `updateResource should update existing resource and return DTO`() {
        // Arrange
        val existingResource = createTestResource()
        val updateDto = createUpdateResourceDto()
        val updatedResource = existingResource.copy(
            name = "Updated Conference Room",
            capacity = 40,
            hourlyRate = BigDecimal.valueOf(85.0),
            updatedAt = NOW.plusMinutes(1)
        )
        
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.of(existingResource))
        whenever(resourceRepository.save(any<Resource>())).thenReturn(updatedResource)

        // Act
        val result = resourceService.updateResource(TEST_ID, updateDto)

        // Assert
        assertNotNull(result)
        assertEquals("Updated Conference Room", result!!.name)
        assertEquals(40, result.capacity)
        assertEquals(85.0, result.hourlyRate)
        assertTrue(result.isAvailable) // Should remain available
        
        verify(resourceRepository).findById(TEST_ID)
        verify(resourceRepository).save(any<Resource>())
    }

    @Test
    fun `updateResource should return null when resource not found`() {
        // Arrange
        val updateDto = createUpdateResourceDto()
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.empty())

        // Act
        val result = resourceService.updateResource(TEST_ID, updateDto)

        // Assert
        assertNull(result)
        verify(resourceRepository).findById(TEST_ID)
        verify(resourceRepository, never()).save(any<Resource>())
    }

    @Test
    fun `deleteResource should soft delete existing resource`() {
        // Arrange
        val existingResource = createTestResource()
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.of(existingResource))
        whenever(resourceRepository.save(any<Resource>())).thenReturn(existingResource)

        // Act
        val result = resourceService.deleteResource(TEST_ID)

        // Assert
        assertTrue(result)
        verify(resourceRepository).findById(TEST_ID)
        verify(resourceRepository).save(any<Resource>())
    }

    @Test
    fun `deleteResource should return false when resource not found`() {
        // Arrange
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.empty())

        // Act
        val result = resourceService.deleteResource(TEST_ID)

        // Assert
        assertFalse(result)
        verify(resourceRepository).findById(TEST_ID)
        verify(resourceRepository, never()).save(any<Resource>())
    }

    // ================================================================================
    // Business Logic Tests
    // ================================================================================

    @Test
    fun `getResourcesByEvent should return active resources for event`() {
        // Arrange
        val eventId = UUID.randomUUID()
        val eventResources = listOf(
            createTestResource(id = UUID.randomUUID(), name = "Event Room"),
            createTestResource(id = UUID.randomUUID(), name = "Event Equipment", type = ResourceType.EQUIPMENT)
        )
        whenever(resourceRepository.findByIsActiveTrue()).thenReturn(eventResources)

        // Act
        val result = resourceService.getResourcesByEvent(eventId)

        // Assert
        assertEquals(2, result.size)
        assertEquals("Event Room", result[0].name)
        assertEquals("Event Equipment", result[1].name)
        verify(resourceRepository).findByIsActiveTrue()
    }

    @Test
    fun `getAvailableResources should return available resources when no date range`() {
        // Arrange
        val availableResources = listOf(
            createTestResource(id = UUID.randomUUID(), name = "Available Room 1"),
            createTestResource(id = UUID.randomUUID(), name = "Available Room 2")
        )
        whenever(resourceRepository.findByIsBookableTrueAndIsActiveTrueAndStatus(ResourceStatus.AVAILABLE))
            .thenReturn(availableResources)

        // Act
        val result = resourceService.getAvailableResources(null, null)

        // Assert
        assertEquals(2, result.size)
        assertEquals("Available Room 1", result[0].name)
        assertEquals("Available Room 2", result[1].name)
        verify(resourceRepository).findByIsBookableTrueAndIsActiveTrueAndStatus(ResourceStatus.AVAILABLE)
    }

    @Test
    fun `getAvailableResources should use date range when provided`() {
        // Arrange
        val startDate = NOW.toString()
        val endDate = NOW.plusHours(2).toString()
        val availableResources = listOf(createTestResource())
        
        whenever(resourceRepository.findAvailableResources(any(), any(), isNull(), isNull()))
            .thenReturn(availableResources)

        // Act
        val result = resourceService.getAvailableResources(startDate, endDate)

        // Assert
        assertEquals(1, result.size)
        assertEquals("Test Conference Room", result[0].name)
        verify(resourceRepository).findAvailableResources(any(), any(), isNull(), isNull())
    }

    // ================================================================================
    // DTO Conversion Tests
    // ================================================================================

    @Test
    fun `toDto should convert Resource entity to ResourceDto correctly`() {
        // Arrange
        val resource = createTestResource(
            tags = "tag1, tag2, tag3",
            specifications = "Test specifications",
            contactEmail = "test@example.com",
            contactPhone = "+1-555-0123"
        )
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.of(resource))

        // Act
        val result = resourceService.getResourceById(TEST_ID)!!

        // Assert
        assertEquals(TEST_ID, result.id)
        assertEquals("Test Conference Room", result.name)
        assertEquals("ROOM", result.type)
        assertEquals("Building A, Floor 2", result.location)
        assertEquals(50, result.capacity)
        assertEquals(100.0, result.hourlyRate)
        assertTrue(result.isAvailable)
        assertEquals(listOf("tag1", "tag2", "tag3"), result.features)
        assertEquals("test@example.com", result.contactEmail)
        assertEquals("+1-555-0123", result.contactPhone)
        assertEquals("Test specifications", result.bookingInstructions)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun `toDto should handle empty tags correctly`() {
        // Arrange
        val resource = createTestResource(tags = null)
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.of(resource))

        // Act
        val result = resourceService.getResourceById(TEST_ID)!!

        // Assert
        assertTrue(result.features.isEmpty())
    }

    @Test
    fun `toDto should handle different resource statuses`() {
        // Arrange
        val unavailableResource = createTestResource(status = ResourceStatus.OUT_OF_SERVICE)
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.of(unavailableResource))

        // Act
        val result = resourceService.getResourceById(TEST_ID)!!

        // Assert
        assertFalse(result.isAvailable)
        assertEquals("ROOM", result.type)
    }

    @Test
    fun `toDto should handle null optional fields correctly`() {
        // Arrange
        val minimalResource = Resource(
            id = TEST_ID,
            name = "Minimal Resource",
            type = ResourceType.OTHER,
            isActive = true,
            createdAt = NOW,
            updatedAt = NOW
        )
        whenever(resourceRepository.findById(TEST_ID)).thenReturn(Optional.of(minimalResource))

        // Act
        val result = resourceService.getResourceById(TEST_ID)!!

        // Assert
        assertEquals("Minimal Resource", result.name)
        assertEquals("OTHER", result.type)
        assertNull(result.location)
        assertNull(result.capacity)
        assertNull(result.hourlyRate)
        assertNull(result.contactEmail)
        assertNull(result.contactPhone)
        assertNull(result.bookingInstructions)
        assertTrue(result.features.isEmpty())
        assertTrue(result.isAvailable) // Default for AVAILABLE status
    }
}