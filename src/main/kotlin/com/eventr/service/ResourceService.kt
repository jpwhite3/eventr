package com.eventr.service
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class ResourceService {

    // Mock data for development - replace with actual repository implementation
    private val mockResources = mutableListOf<ResourceDto>()

    init {
        // Initialize with mock data
        mockResources.addAll(
            listOf(
                ResourceDto(
                    id = UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
                    name = "Conference Room A",
                    type = "ROOM",
                    description = "Large conference room with projector and sound system",
                    location = "Building 1, Floor 2",
                    capacity = 50,
                    hourlyRate = 75.00,
                    isAvailable = true,
                    features = listOf("Projector", "Sound System", "Whiteboard", "Video Conferencing"),
                    imageUrl = "https://via.placeholder.com/300x200?text=Conference+Room+A",
                    contactEmail = "facilities@example.com",
                    contactPhone = "+1-555-0101",
                    bookingInstructions = "Book at least 24 hours in advance",
                    createdAt = "2024-01-15T10:00:00",
                    updatedAt = "2024-02-01T14:30:00"
                ),
                ResourceDto(
                    id = UUID.fromString("550e8400-e29b-41d4-a716-446655440002"),
                    name = "Audio/Video Equipment Set",
                    type = "EQUIPMENT",
                    description = "Professional AV equipment including cameras, microphones, and lighting",
                    location = "Equipment Room, Building 2",
                    capacity = null,
                    hourlyRate = 150.00,
                    isAvailable = true,
                    features = listOf("4K Cameras", "Wireless Mics", "LED Lighting", "Mixing Board"),
                    imageUrl = "https://via.placeholder.com/300x200?text=AV+Equipment",
                    contactEmail = "av-support@example.com",
                    contactPhone = "+1-555-0102",
                    bookingInstructions = "Technical setup required - contact AV team",
                    createdAt = "2024-01-20T09:15:00",
                    updatedAt = "2024-02-05T16:45:00"
                ),
                ResourceDto(
                    id = UUID.fromString("550e8400-e29b-41d4-a716-446655440003"),
                    name = "Outdoor Pavilion",
                    type = "VENUE",
                    description = "Covered outdoor space perfect for large gatherings and events",
                    location = "Campus Courtyard",
                    capacity = 200,
                    hourlyRate = 125.00,
                    isAvailable = true,
                    features = listOf("Weather Protection", "Power Outlets", "Sound System", "Staging Area"),
                    imageUrl = "https://via.placeholder.com/300x200?text=Outdoor+Pavilion",
                    contactEmail = "venues@example.com",
                    contactPhone = "+1-555-0103",
                    bookingInstructions = "Weather contingency plans required",
                    createdAt = "2024-01-10T11:30:00",
                    updatedAt = "2024-01-25T13:20:00"
                ),
                ResourceDto(
                    id = UUID.fromString("550e8400-e29b-41d4-a716-446655440004"),
                    name = "Catering Kitchen",
                    type = "FACILITY",
                    description = "Fully equipped commercial kitchen for event catering",
                    location = "Building 3, Ground Floor",
                    capacity = 10,
                    hourlyRate = 200.00,
                    isAvailable = false,
                    features = listOf("Commercial Ovens", "Prep Areas", "Dishwashing", "Storage"),
                    imageUrl = "https://via.placeholder.com/300x200?text=Catering+Kitchen",
                    contactEmail = "catering@example.com",
                    contactPhone = "+1-555-0104",
                    bookingInstructions = "Food safety certification required",
                    createdAt = "2024-02-01T08:00:00",
                    updatedAt = "2024-02-15T10:15:00"
                ),
                ResourceDto(
                    id = UUID.fromString("550e8400-e29b-41d4-a716-446655440005"),
                    name = "Presentation Screens",
                    type = "EQUIPMENT",
                    description = "Set of large LED displays for presentations and signage",
                    location = "AV Storage, Building 1",
                    capacity = null,
                    hourlyRate = 50.00,
                    isAvailable = true,
                    features = listOf("4K Resolution", "Wireless Connectivity", "Mobile Stands", "Remote Control"),
                    imageUrl = "https://via.placeholder.com/300x200?text=LED+Displays",
                    contactEmail = "tech-support@example.com",
                    contactPhone = "+1-555-0105",
                    bookingInstructions = "Setup and breakdown included in rental",
                    createdAt = "2024-01-25T14:45:00",
                    updatedAt = "2024-02-10T09:30:00"
                )
            )
        )
    }

    fun getAllResources(): List<ResourceDto> {
        return mockResources.toList()
    }

    fun getResourceById(id: UUID): ResourceDto? {
        return mockResources.find { it.id == id }
    }

    fun createResource(createDto: CreateResourceDto): ResourceDto {
        val newResource = ResourceDto(
            id = UUID.randomUUID(),
            name = createDto.name,
            type = createDto.type,
            description = createDto.description,
            location = createDto.location,
            capacity = createDto.capacity,
            hourlyRate = createDto.hourlyRate,
            isAvailable = true,
            features = createDto.features ?: emptyList(),
            imageUrl = createDto.imageUrl,
            contactEmail = createDto.contactEmail,
            contactPhone = createDto.contactPhone,
            bookingInstructions = createDto.bookingInstructions,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        mockResources.add(newResource)
        return newResource
    }

    fun updateResource(id: UUID, updateDto: UpdateResourceDto): ResourceDto? {
        val index = mockResources.indexOfFirst { it.id == id }
        if (index == -1) return null

        val existingResource = mockResources[index]
        val updatedResource = existingResource.copy(
            name = updateDto.name ?: existingResource.name,
            type = updateDto.type ?: existingResource.type,
            description = updateDto.description ?: existingResource.description,
            location = updateDto.location ?: existingResource.location,
            capacity = updateDto.capacity ?: existingResource.capacity,
            hourlyRate = updateDto.hourlyRate ?: existingResource.hourlyRate,
            isAvailable = updateDto.isAvailable ?: existingResource.isAvailable,
            features = updateDto.features ?: existingResource.features,
            imageUrl = updateDto.imageUrl ?: existingResource.imageUrl,
            contactEmail = updateDto.contactEmail ?: existingResource.contactEmail,
            contactPhone = updateDto.contactPhone ?: existingResource.contactPhone,
            bookingInstructions = updateDto.bookingInstructions ?: existingResource.bookingInstructions,
            updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )

        mockResources[index] = updatedResource
        return updatedResource
    }

    fun deleteResource(id: UUID): Boolean {
        return mockResources.removeIf { it.id == id }
    }

    fun getResourcesByEvent(eventId: UUID): List<ResourceDto> {
        // Mock implementation - in real scenario would query resource assignments
        return mockResources.filter { it.isAvailable }
    }

    fun getAvailableResources(startDate: String?, endDate: String?): List<ResourceDto> {
        // Mock implementation - in real scenario would check booking conflicts
        return mockResources.filter { it.isAvailable }
    }
}

// DTOs for resource management
data class ResourceDto(
    val id: UUID,
    val name: String,
    val type: String,
    val description: String?,
    val location: String?,
    val capacity: Int?,
    val hourlyRate: Double?,
    val isAvailable: Boolean,
    val features: List<String>,
    val imageUrl: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val bookingInstructions: String?,
    val createdAt: String,
    val updatedAt: String
)

data class CreateResourceDto(
    val name: String,
    val type: String,
    val description: String?,
    val location: String?,
    val capacity: Int?,
    val hourlyRate: Double?,
    val features: List<String>?,
    val imageUrl: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val bookingInstructions: String?
)

data class UpdateResourceDto(
    val name: String?,
    val type: String?,
    val description: String?,
    val location: String?,
    val capacity: Int?,
    val hourlyRate: Double?,
    val isAvailable: Boolean?,
    val features: List<String>?,
    val imageUrl: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val bookingInstructions: String?
)