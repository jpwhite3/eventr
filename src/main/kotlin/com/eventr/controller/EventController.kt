package com.eventr.controller

import com.eventr.dto.EventCreateDto
import com.eventr.dto.EventDto
import com.eventr.dto.EventInstanceDto
import com.eventr.dto.EventUpdateDto
import com.eventr.dto.RegistrationDto
import com.eventr.model.RegistrationStatus
import com.eventr.modules.event.api.EventModuleApi
import com.eventr.model.EventType
import com.eventr.modules.event.api.dto.CreateEventRequest
import com.eventr.modules.event.api.dto.EventFilterCriteria
import com.eventr.modules.event.api.dto.EventResponse
import com.eventr.modules.event.api.dto.UpdateEventRequest
import com.eventr.repository.RegistrationRepository
import java.util.UUID
import org.springframework.beans.BeanUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema

/**
 * REST controller for event operations.
 * 
 * This is a THIN controller - all business logic is delegated to EventModuleApi.
 * The controller only handles HTTP concerns (request/response mapping, status codes).
 */
@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Event management operations")
class EventController(
    private val eventModule: EventModuleApi,
    private val registrationRepository: RegistrationRepository  // TODO: Move to RegistrationModuleApi
) {

    // ==================== Mapping Helpers ====================
    
    private fun toDto(response: EventResponse): EventDto {
        return EventDto().apply {
            id = response.id
            name = response.name
            description = response.description
            status = response.status
            eventType = response.eventType
            category = response.category
            bannerImageUrl = response.bannerImageUrl
            thumbnailImageUrl = response.thumbnailImageUrl
            tags = response.tags.toMutableList()
            capacity = response.capacity
            waitlistEnabled = response.waitlistEnabled
            venueName = response.venueName
            address = response.address
            city = response.city
            state = response.state
            zipCode = response.zipCode
            country = response.country
            virtualUrl = response.virtualUrl
            dialInNumber = response.dialInNumber
            accessCode = response.accessCode
            requiresApproval = response.requiresApproval
            maxRegistrations = response.maxRegistrations
            organizerName = response.organizerName
            organizerEmail = response.organizerEmail
            organizerPhone = response.organizerPhone
            organizerWebsite = response.organizerWebsite
            startDateTime = response.startDateTime
            endDateTime = response.endDateTime
            timezone = response.timezone
            agenda = response.agenda
            instances = response.instances.map { inst ->
                EventInstanceDto().apply {
                    id = inst.id
                    dateTime = inst.startDateTime
                    location = inst.location
                }
            }
        }
    }
    
    private fun toCreateRequest(dto: EventCreateDto): CreateEventRequest {
        return CreateEventRequest(
            name = dto.name ?: "",
            description = dto.description,
            eventType = dto.eventType ?: EventType.IN_PERSON,
            category = dto.category,
            bannerImageUrl = dto.bannerImageUrl,
            thumbnailImageUrl = dto.thumbnailImageUrl,
            tags = dto.tags,
            capacity = dto.capacity,
            waitlistEnabled = dto.waitlistEnabled ?: false,
            venueName = dto.venueName,
            address = dto.address,
            city = dto.city,
            state = dto.state,
            zipCode = dto.zipCode,
            country = dto.country,
            virtualUrl = dto.virtualUrl,
            dialInNumber = dto.dialInNumber,
            accessCode = dto.accessCode,
            requiresApproval = dto.requiresApproval ?: false,
            maxRegistrations = dto.maxRegistrations,
            organizerName = dto.organizerName,
            organizerEmail = dto.organizerEmail,
            organizerPhone = dto.organizerPhone,
            organizerWebsite = dto.organizerWebsite,
            startDateTime = dto.startDateTime,
            endDateTime = dto.endDateTime,
            timezone = dto.timezone ?: "UTC",
            agenda = dto.agenda
        )
    }
    
    private fun toUpdateRequest(dto: EventUpdateDto): UpdateEventRequest {
        return UpdateEventRequest(
            name = dto.name,
            description = dto.description,
            eventType = dto.eventType,
            category = dto.category,
            bannerImageUrl = dto.bannerImageUrl,
            thumbnailImageUrl = dto.thumbnailImageUrl,
            tags = dto.tags,
            capacity = dto.capacity,
            waitlistEnabled = dto.waitlistEnabled,
            venueName = dto.venueName,
            address = dto.address,
            city = dto.city,
            state = dto.state,
            zipCode = dto.zipCode,
            country = dto.country,
            virtualUrl = dto.virtualUrl,
            dialInNumber = dto.dialInNumber,
            accessCode = dto.accessCode,
            requiresApproval = dto.requiresApproval,
            maxRegistrations = dto.maxRegistrations,
            organizerName = dto.organizerName,
            organizerEmail = dto.organizerEmail,
            organizerPhone = dto.organizerPhone,
            organizerWebsite = dto.organizerWebsite,
            startDateTime = dto.startDateTime,
            endDateTime = dto.endDateTime,
            timezone = dto.timezone,
            agenda = dto.agenda
        )
    }

    // ==================== Endpoints ====================

    @PostMapping
    @Operation(
        summary = "Create a new event",
        description = "Creates a new event in draft status. Automatically creates a default EventInstance."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Event created successfully",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = EventDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid event data provided")
    ])
    fun createEvent(
        @Parameter(description = "Event creation details")
        @RequestBody eventCreateDto: EventCreateDto
    ): EventDto {
        val response = eventModule.createEvent(toCreateRequest(eventCreateDto))
        return toDto(response)
    }

    @GetMapping
    @Operation(
        summary = "Get all events",
        description = "Retrieves events with optional filtering by status and sorting."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Events retrieved successfully",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Array<EventDto>::class))])
    ])
    fun getAllEvents(
        @Parameter(description = "Text search query") @RequestParam(required = false) q: String?,
        @Parameter(description = "Sort field") @RequestParam(required = false) sortBy: String?,
        @Parameter(description = "Sort order (asc, desc)") @RequestParam(required = false) sortOrder: String?,
        @Parameter(description = "Show only published events") @RequestParam(defaultValue = "true") publishedOnly: Boolean
    ): List<EventDto> {
        val criteria = EventFilterCriteria(
            search = q,
            sortBy = sortBy ?: "startDateTime",
            sortDirection = sortOrder ?: "asc",
            publishedOnly = publishedOnly
        )
        return eventModule.findEvents(criteria).map { toDto(it) }
    }

    @GetMapping("/{eventId}")
    @Operation(
        summary = "Get event by ID",
        description = "Retrieves a specific event by its unique identifier."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Event found",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = EventDto::class))]),
        ApiResponse(responseCode = "404", description = "Event not found")
    ])
    fun getEventById(
        @Parameter(description = "Unique identifier of the event") @PathVariable eventId: UUID
    ): ResponseEntity<EventDto> {
        val event = eventModule.getEvent(eventId)
        return if (event != null) {
            ResponseEntity.ok(toDto(event))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{eventId}")
    @Operation(summary = "Update an event")
    fun updateEvent(
        @PathVariable eventId: UUID,
        @RequestBody eventUpdateDto: EventUpdateDto
    ): EventDto {
        val response = eventModule.updateEvent(eventId, toUpdateRequest(eventUpdateDto))
        return toDto(response)
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Delete an event")
    fun deleteEvent(@PathVariable eventId: UUID): ResponseEntity<Void> {
        eventModule.deleteEvent(eventId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{eventId}/publish")
    @Operation(summary = "Publish an event", description = "Makes the event visible to attendees")
    fun publishEvent(@PathVariable eventId: UUID): EventDto {
        val response = eventModule.publishEvent(eventId)
        return toDto(response)
    }

    @PostMapping("/{eventId}/clone")
    @Operation(summary = "Clone an event", description = "Creates a copy of the event in draft status")
    fun cloneEvent(@PathVariable eventId: UUID): EventDto {
        val response = eventModule.cloneEvent(eventId)
        return toDto(response)
    }
    
    @GetMapping("/{eventId}/instances")
    @Operation(summary = "Get event instances")
    fun getEventInstances(@PathVariable eventId: UUID): List<EventInstanceDto> {
        return eventModule.getEventInstances(eventId).map { inst ->
            EventInstanceDto().apply {
                id = inst.id
                dateTime = inst.startDateTime
                location = inst.location
            }
        }
    }

    // ==================== Registration Operations (TODO: Move to RegistrationModuleApi) ====================

    @GetMapping("/{eventId}/registrations")
    @Operation(summary = "Get registrations for an event")
    fun getEventRegistrations(@PathVariable eventId: UUID): List<RegistrationDto> {
        // TODO: This should call RegistrationModuleApi when that module is created
        val instances = eventModule.getEventInstances(eventId)
        val instanceIds = instances.map { it.id }
        
        // For now, still using repository directly - will migrate when Registration module is created
        val registrations = registrationRepository.findAll().filter { reg ->
            reg.eventInstance?.id in instanceIds
        }
        
        return registrations.map { registration ->
            RegistrationDto().apply {
                BeanUtils.copyProperties(registration, this)
                eventInstanceId = registration.eventInstance?.id
            }
        }
    }

    data class BulkActionRequest(
        val action: String,
        val registrationIds: List<UUID>
    )

    @PostMapping("/{eventId}/registrations/bulk")
    @Operation(summary = "Perform bulk action on registrations")
    fun performBulkAction(
        @PathVariable eventId: UUID,
        @RequestBody request: BulkActionRequest
    ): ResponseEntity<Map<String, Any>> {
        // TODO: Move to RegistrationModuleApi
        val registrations = registrationRepository.findAllById(request.registrationIds)
        val results = mutableMapOf<String, Int>()
        
        when (request.action.lowercase()) {
            "approve" -> {
                registrations.forEach { registration ->
                    if (registration.status == RegistrationStatus.WAITLISTED) {
                        registration.status = RegistrationStatus.REGISTERED
                        registrationRepository.save(registration)
                    }
                }
                results["approved"] = registrations.count { it.status == RegistrationStatus.REGISTERED }
            }
            
            "cancel" -> {
                registrations.forEach { registration ->
                    registration.status = RegistrationStatus.CANCELLED
                    registrationRepository.save(registration)
                }
                results["cancelled"] = registrations.size
            }
            
            "checkin" -> {
                registrations.forEach { registration ->
                    if (registration.status == RegistrationStatus.REGISTERED) {
                        registration.status = RegistrationStatus.CHECKED_IN
                        registrationRepository.save(registration)
                    }
                }
                results["checkedIn"] = registrations.count { it.status == RegistrationStatus.CHECKED_IN }
            }
        }
        
        return ResponseEntity.ok(mapOf(
            "action" to request.action,
            "processed" to registrations.size,
            "results" to results
        ))
    }
}
