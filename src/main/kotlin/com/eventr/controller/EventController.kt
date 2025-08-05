package com.eventr.controller

import com.eventr.dto.EventCreateDto
import com.eventr.dto.EventDto
import com.eventr.dto.EventInstanceDto
import com.eventr.dto.EventUpdateDto
import com.eventr.model.Event
import com.eventr.model.EventStatus
import com.eventr.repository.EventRepository
import com.eventr.service.DynamoDbService
import com.eventr.service.EventSpecification
import java.time.LocalDate
import java.util.UUID
import org.springframework.beans.BeanUtils
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/events")
class EventController(
        private val eventRepository: EventRepository,
        private val dynamoDbService: DynamoDbService
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
            @RequestParam(required = false) location: String?,
            @RequestParam(required = false, name = "date_start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            dateStart: LocalDate?,
            @RequestParam(required = false, name = "date_end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            dateEnd: LocalDate?,
            @RequestParam(required = false) tags: List<String>?,
            @RequestParam(defaultValue = "true") publishedOnly: Boolean,
            sort: Sort
    ): List<EventDto> {
        val spec: Specification<Event> =
                EventSpecification.filterBy(location, dateStart, dateEnd, tags, publishedOnly)
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
}
