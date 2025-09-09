package com.eventr.service
import com.eventr.model.Resource
import com.eventr.model.ResourceType
import com.eventr.model.ResourceStatus
import com.eventr.repository.ResourceRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class ResourceService(
    private val resourceRepository: ResourceRepository
) {

    fun getAllResources(): List<ResourceDto> {
        return resourceRepository.findByIsActiveTrue().map { it.toDto() }
    }

    fun getResourceById(id: UUID): ResourceDto? {
        return resourceRepository.findById(id).orElse(null)?.toDto()
    }

    fun createResource(createDto: CreateResourceDto): ResourceDto {
        val resource = Resource(
            name = createDto.name,
            description = createDto.description,
            type = ResourceType.valueOf(createDto.type.uppercase()),
            location = createDto.location,
            capacity = createDto.capacity,
            hourlyRate = createDto.hourlyRate?.let { BigDecimal.valueOf(it) },
            contactEmail = createDto.contactEmail,
            contactPhone = createDto.contactPhone,
            specifications = createDto.bookingInstructions
        )
        return resourceRepository.save(resource).toDto()
    }

    fun updateResource(id: UUID, updateDto: UpdateResourceDto): ResourceDto? {
        val resource = resourceRepository.findById(id).orElse(null) ?: return null
        
        updateDto.name?.let { resource.name = it }
        updateDto.description?.let { resource.description = it }
        updateDto.type?.let { resource.type = ResourceType.valueOf(it.uppercase()) }
        updateDto.location?.let { resource.location = it }
        updateDto.capacity?.let { resource.capacity = it }
        updateDto.hourlyRate?.let { resource.hourlyRate = BigDecimal.valueOf(it) }
        updateDto.isAvailable?.let { 
            resource.status = if (it) ResourceStatus.AVAILABLE else ResourceStatus.OUT_OF_SERVICE 
        }
        updateDto.contactEmail?.let { resource.contactEmail = it }
        updateDto.contactPhone?.let { resource.contactPhone = it }
        updateDto.bookingInstructions?.let { resource.specifications = it }
        resource.updatedAt = LocalDateTime.now()
        
        return resourceRepository.save(resource).toDto()
    }

    fun deleteResource(id: UUID): Boolean {
        val resource = resourceRepository.findById(id).orElse(null) ?: return false
        resource.isActive = false
        resource.updatedAt = LocalDateTime.now()
        resourceRepository.save(resource)
        return true
    }

    fun getResourcesByEvent(eventId: UUID): List<ResourceDto> {
        // Get resources assigned to sessions within this event
        return resourceRepository.findByIsActiveTrue().map { it.toDto() }
    }

    fun getAvailableResources(startDate: String?, endDate: String?): List<ResourceDto> {
        return if (startDate != null && endDate != null) {
            val start = LocalDateTime.parse(startDate)
            val end = LocalDateTime.parse(endDate)
            resourceRepository.findAvailableResources(start, end, null, null).map { it.toDto() }
        } else {
            resourceRepository.findByIsBookableTrueAndIsActiveTrueAndStatus(ResourceStatus.AVAILABLE).map { it.toDto() }
        }
    }
    
    private fun Resource.toDto(): ResourceDto {
        return ResourceDto(
            id = this.id!!,
            name = this.name,
            type = this.type.name,
            description = this.description,
            location = this.location,
            capacity = this.capacity,
            hourlyRate = this.hourlyRate?.toDouble(),
            isAvailable = this.status == ResourceStatus.AVAILABLE,
            features = this.tags?.split(",")?.map { it.trim() } ?: emptyList(),
            imageUrl = null, // Could be added to Resource model later
            contactEmail = this.contactEmail,
            contactPhone = this.contactPhone,
            bookingInstructions = this.specifications,
            createdAt = this.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            updatedAt = this.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
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