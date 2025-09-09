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
import com.eventr.service.DynamoDbService
import com.eventr.service.EmailNotificationService
import com.eventr.service.EventSpecification
import java.time.LocalDate
import java.util.UUID
import org.springframework.beans.BeanUtils
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.eventr.util.SecureLogger

@RestController
@RequestMapping("/api/events")
class EventController(
        private val eventRepository: EventRepository,
        private val registrationRepository: RegistrationRepository,
        private val emailNotificationService: EmailNotificationService,
        private val dynamoDbService: DynamoDbService
) {
    
    private val secureLogger = SecureLogger(EventController::class.java)

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
    fun createEvent(@RequestBody eventCreateDto: EventCreateDto): EventDto {
        val event =
                Event().apply {
                    BeanUtils.copyProperties(eventCreateDto, this)
                    status = EventStatus.DRAFT
                }
        val savedEvent = eventRepository.save(event)
        eventCreateDto.formData?.let { formData ->
            dynamoDbService.saveFormDefinition(savedEvent.id.toString(), formData)
        }
        return convertToDto(savedEvent)
    }

    @GetMapping
    fun getAllEvents(
            @RequestParam(required = false) category: String?,
            @RequestParam(required = false) eventType: String?,
            @RequestParam(required = false) city: String?,
            @RequestParam(required = false) tags: String?,
            @RequestParam(required = false) startDate: String?,
            @RequestParam(required = false) endDate: String?,
            @RequestParam(required = false) q: String?,
            @RequestParam(required = false) latitude: Double?,
            @RequestParam(required = false) longitude: Double?,
            @RequestParam(required = false) radius: Int?,
            @RequestParam(required = false) sortBy: String?,
            @RequestParam(required = false) sortOrder: String?,
            @RequestParam(defaultValue = "true") publishedOnly: Boolean
    ): List<EventDto> {
        // Parse tags from comma-separated string
        val tagsList = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        
        // Parse dates
        val dateStart = startDate?.let { LocalDate.parse(it) }
        val dateEnd = endDate?.let { LocalDate.parse(it) }
        
        // Create sort configuration
        val sortDirection = if (sortOrder?.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        val sortProperty = when (sortBy?.lowercase()) {
            "name" -> "name"
            "city" -> "city"
            "category" -> "category"
            "startdatetime", "date" -> "startDateTime"
            else -> "startDateTime"
        }
        val sort = Sort.by(sortDirection, sortProperty)
        
        val spec: Specification<Event> = EventSpecification.filterBy(
            category = category,
            eventType = eventType,
            city = city,
            dateStart = dateStart,
            dateEnd = dateEnd,
            tags = tagsList,
            searchQuery = q,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            publishedOnly = publishedOnly
        )
        return eventRepository.findAll(spec, sort).map { convertToDto(it) }
    }

    @GetMapping("/{eventId}")
    fun getEventById(@PathVariable eventId: UUID): ResponseEntity<EventDto> {
        val optionalEvent = eventRepository.findById(eventId)
        return if (optionalEvent.isPresent) {
            ResponseEntity.ok(convertToDto(optionalEvent.get()))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{eventId}/form")
    fun getFormDefinition(@PathVariable eventId: UUID): ResponseEntity<String> {
        val formDefinition = dynamoDbService.getFormDefinition(eventId.toString())
        return if (formDefinition != null) {
            ResponseEntity.ok(formDefinition)
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
        val newEvent =
                Event().apply {
                    BeanUtils.copyProperties(originalEvent, this, "id")
                    status = EventStatus.DRAFT
                }
        val clonedEvent = eventRepository.save(newEvent)

        val formDefinition = dynamoDbService.getFormDefinition(originalEvent.id.toString())
        if (formDefinition != null) {
            dynamoDbService.saveFormDefinition(clonedEvent.id.toString(), formDefinition)
        }
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
        val registrationIds: List<UUID>,
        val reason: String? = null,
        val emailSubject: String? = null,
        val emailBody: String? = null
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
                    
                    // Send cancellation email if reason provided
                    request.reason?.let { reason ->
                        try {
                            emailNotificationService.sendCancellationNotification(registration, reason)
                        } catch (e: Exception) {
                            // Log but don't fail the operation
                            secureLogger.logErrorEvent("EVENT_CANCELLATION_EMAIL_FAILED", null, e, "Failed to send event cancellation email")
                        }
                    }
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
            
            "email" -> {
                request.emailSubject?.let { subject ->
                    request.emailBody?.let { body ->
                        var emailsSent = 0
                        registrations.forEach { registration ->
                            try {
                                emailNotificationService.sendCustomEmail(registration, subject, body)
                                emailsSent++
                            } catch (e: Exception) {
                                secureLogger.logErrorEvent("EVENT_UPDATE_EMAIL_FAILED", registration.user?.id, e, "Failed to send event update email")
                            }
                        }
                        results["emailsSent"] = emailsSent
                    }
                }
            }
        }
        
        return ResponseEntity.ok(mapOf(
            "action" to request.action,
            "processed" to registrations.size,
            "results" to results
        ))
    }

    data class EmailRequest(
        val recipientType: String, // "selected", "all", "status"
        val statusFilter: String? = null,
        val selectedRegistrationIds: List<UUID>? = null,
        val subject: String,
        val body: String
    )

    @PostMapping("/{eventId}/email")
    fun sendEventEmail(
        @PathVariable eventId: UUID,
        @RequestBody request: EmailRequest
    ): ResponseEntity<Map<String, Any>> {
        val event = eventRepository.findById(eventId).orElseThrow()
        val allRegistrations = event.instances?.flatMap { instance ->
            registrationRepository.findByEventInstance(instance)
        } ?: emptyList()
        
        val targetRegistrations = when (request.recipientType) {
            "selected" -> {
                request.selectedRegistrationIds?.let { ids ->
                    allRegistrations.filter { it.id in ids }
                } ?: emptyList()
            }
            "all" -> allRegistrations
            "status" -> {
                request.statusFilter?.let { status ->
                    allRegistrations.filter { it.status == RegistrationStatus.valueOf(status) }
                } ?: emptyList()
            }
            else -> emptyList()
        }
        
        var emailsSent = 0
        var emailsFailed = 0
        
        targetRegistrations.forEach { registration ->
            try {
                emailService.sendCustomEmail(registration, request.subject, request.body)
                emailsSent++
            } catch (e: Exception) {
                emailsFailed++
                secureLogger.logErrorEvent("EVENT_DELETION_EMAIL_FAILED", registration.user?.id, e, "Failed to send event deletion email")
            }
        }
        
        return ResponseEntity.ok(mapOf(
            "totalRecipients" to targetRegistrations.size,
            "emailsSent" to emailsSent,
            "emailsFailed" to emailsFailed,
            "recipientType" to request.recipientType
        ))
    }
}
