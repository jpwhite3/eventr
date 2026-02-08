package com.eventr.controller

import com.eventr.dto.EventCreateDto
import com.eventr.dto.EventDto
import com.eventr.dto.EventInstanceDto
import com.eventr.dto.EventUpdateDto
import com.eventr.dto.RegistrationDto
import com.eventr.model.Event
import com.eventr.model.EventStatus
import com.eventr.model.RegistrationStatus
import com.eventr.repository.EventRepository
import com.eventr.repository.RegistrationRepository
import java.util.UUID
import org.springframework.beans.BeanUtils
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Event management operations")
class EventController(
        private val eventRepository: EventRepository,
        private val registrationRepository: RegistrationRepository
) {

    private fun convertToDto(event: Event): EventDto {
        val eventDto = EventDto()
        BeanUtils.copyProperties(event, eventDto)
        event.instances?.let { instances ->
            eventDto.instances =
                    instances.map { instance ->
                        EventInstanceDto().apply { BeanUtils.copyProperties(instance, this) }
                    }
        }
        return eventDto
    }

    @PostMapping
    @Operation(
        summary = "Create a new event",
        description = "Creates a new event in draft status with the provided details."
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
        val event = Event().apply {
            BeanUtils.copyProperties(eventCreateDto, this)
            status = EventStatus.DRAFT
        }
        val savedEvent = eventRepository.save(event)
        return convertToDto(savedEvent)
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
        val sortDirection = if (sortOrder?.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        val sortProperty = when (sortBy?.lowercase()) {
            "name" -> "name"
            "city" -> "city"
            "category" -> "category"
            "startdatetime", "date" -> "startDateTime"
            else -> "startDateTime"
        }
        val sort = Sort.by(sortDirection, sortProperty)
        
        var events = if (publishedOnly) {
            eventRepository.findByStatus(EventStatus.PUBLISHED, sort)
        } else {
            eventRepository.findAll(sort)
        }
        
        // Simple text search filtering
        q?.let { query ->
            val lowerQuery = query.lowercase()
            events = events.filter { event ->
                event.name?.lowercase()?.contains(lowerQuery) == true ||
                event.description?.lowercase()?.contains(lowerQuery) == true ||
                event.city?.lowercase()?.contains(lowerQuery) == true
            }
        }
        
        return events.map { convertToDto(it) }
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
        val optionalEvent = eventRepository.findById(eventId)
        return if (optionalEvent.isPresent) {
            ResponseEntity.ok(convertToDto(optionalEvent.get()))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{eventId}")
    fun updateEvent(
            @PathVariable eventId: UUID,
            @RequestBody eventUpdateDto: EventUpdateDto
    ): EventDto {
        val event = eventRepository.findById(eventId).orElseThrow()
        BeanUtils.copyProperties(eventUpdateDto, event)
        val updatedEvent = eventRepository.save(event)
        return convertToDto(updatedEvent)
    }

    @DeleteMapping("/{eventId}")
    fun deleteEvent(@PathVariable eventId: UUID): ResponseEntity<Void> {
        eventRepository.deleteById(eventId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{eventId}/publish")
    fun publishEvent(@PathVariable eventId: UUID): EventDto {
        val event = eventRepository.findById(eventId).orElseThrow()
        event.status = EventStatus.PUBLISHED
        val publishedEvent = eventRepository.save(event)
        return convertToDto(publishedEvent)
    }

    @PostMapping("/{eventId}/clone")
    fun cloneEvent(@PathVariable eventId: UUID): EventDto {
        val originalEvent = eventRepository.findById(eventId).orElseThrow()
        val newEvent = Event().apply {
            BeanUtils.copyProperties(originalEvent, this, "id")
            status = EventStatus.DRAFT
        }
        val clonedEvent = eventRepository.save(newEvent)
        return convertToDto(clonedEvent)
    }

    @GetMapping("/{eventId}/registrations")
    fun getEventRegistrations(@PathVariable eventId: UUID): List<RegistrationDto> {
        val event = eventRepository.findById(eventId).orElseThrow()
        val registrations = event.instances?.flatMap { instance ->
            registrationRepository.findByEventInstance(instance)
        } ?: emptyList()
        
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
    fun performBulkAction(
        @PathVariable eventId: UUID,
        @RequestBody request: BulkActionRequest
    ): ResponseEntity<Map<String, Any>> {
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
