package com.eventr.modules.event.internal

import com.eventr.model.Event
import com.eventr.model.EventCategory
import com.eventr.model.EventInstance
import com.eventr.model.EventStatus
import com.eventr.model.EventType
import com.eventr.modules.event.api.EventModuleApi
import com.eventr.modules.event.api.EventNotFoundException
import com.eventr.modules.event.api.dto.*
import com.eventr.modules.event.events.EventCancelled
import com.eventr.modules.event.events.EventCreated
import com.eventr.modules.event.events.EventDeleted
import com.eventr.modules.event.events.EventPublished
import com.eventr.repository.EventInstanceRepository
import com.eventr.repository.EventRepository
import com.eventr.shared.event.EventPublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Implementation of the Event module API.
 * 
 * This is the internal implementation - other modules should only
 * interact through the EventModuleApi interface.
 */
@Service
@Transactional
class EventModuleApiImpl(
    private val eventRepository: EventRepository,
    private val eventInstanceRepository: EventInstanceRepository,
    private val eventPublisher: EventPublisher
) : EventModuleApi {
    
    private val logger = LoggerFactory.getLogger(EventModuleApiImpl::class.java)
    
    // ==================== Event Operations ====================
    
    override fun createEvent(request: CreateEventRequest): EventResponse {
        logger.info("Creating new event: {}", request.name)
        
        // Create the event entity
        val event = Event().apply {
            name = request.name
            description = request.description
            status = EventStatus.DRAFT
            eventType = request.eventType
            category = request.category
            bannerImageUrl = request.bannerImageUrl
            thumbnailImageUrl = request.thumbnailImageUrl
            tags = request.tags?.toMutableList() ?: mutableListOf()
            capacity = request.capacity
            waitlistEnabled = request.waitlistEnabled
            venueName = request.venueName
            address = request.address
            city = request.city
            state = request.state
            zipCode = request.zipCode
            country = request.country
            virtualUrl = request.virtualUrl
            dialInNumber = request.dialInNumber
            accessCode = request.accessCode
            requiresApproval = request.requiresApproval
            maxRegistrations = request.maxRegistrations
            organizerName = request.organizerName
            organizerEmail = request.organizerEmail
            organizerPhone = request.organizerPhone
            organizerWebsite = request.organizerWebsite
            startDateTime = request.startDateTime
            endDateTime = request.endDateTime
            timezone = request.timezone
            agenda = request.agenda
        }
        
        val savedEvent = eventRepository.save(event)
        
        // AUTO-CREATE DEFAULT EVENT INSTANCE (Issue #54)
        // This ensures registration always has an instance to register against
        val defaultInstance = createDefaultInstance(savedEvent)
        
        // Publish domain event
        eventPublisher.publish(EventCreated(
            aggregateId = savedEvent.id!!,
            eventName = savedEvent.name ?: "",
            organizerEmail = savedEvent.organizerEmail,
            startDateTime = savedEvent.startDateTime,
            defaultInstanceId = defaultInstance?.id
        ))
        
        logger.info("Created event {} with ID {} and default instance {}", 
            savedEvent.name, savedEvent.id, defaultInstance?.id)
        
        return toResponse(savedEvent)
    }
    
    /**
     * Creates a default EventInstance for an event.
     * This solves Issue #54 - registration requires an EventInstance ID.
     */
    private fun createDefaultInstance(event: Event): EventInstance? {
        val instance = EventInstance().apply {
            this.event = event
            this.dateTime = event.startDateTime
            this.location = buildLocationString(event)
        }
        
        val savedInstance = eventInstanceRepository.save(instance)
        
        // Add to event's instance list
        if (event.instances == null) {
            event.instances = mutableListOf()
        }
        event.instances!!.add(savedInstance)
        
        logger.debug("Created default instance {} for event {}", savedInstance.id, event.id)
        return savedInstance
    }
    
    private fun buildLocationString(event: Event): String {
        return when (event.eventType) {
            EventType.VIRTUAL -> event.virtualUrl ?: "Virtual Event"
            EventType.HYBRID -> listOfNotNull(event.venueName, event.city, event.virtualUrl).joinToString(" / ")
            else -> listOfNotNull(event.venueName, event.address, event.city, event.state)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifBlank { "Location TBD" }
        }
    }
    
    override fun getEvent(id: UUID): EventResponse? {
        return eventRepository.findById(id)
            .map { toResponse(it) }
            .orElse(null)
    }
    
    override fun getEventOrThrow(id: UUID): EventResponse {
        return getEvent(id) ?: throw EventNotFoundException(id)
    }
    
    override fun updateEvent(id: UUID, request: UpdateEventRequest): EventResponse {
        val event = eventRepository.findById(id)
            .orElseThrow { EventNotFoundException(id) }
        
        // Apply updates (only non-null fields)
        request.name?.let { event.name = it }
        request.description?.let { event.description = it }
        request.eventType?.let { event.eventType = it }
        request.category?.let { event.category = it }
        request.bannerImageUrl?.let { event.bannerImageUrl = it }
        request.thumbnailImageUrl?.let { event.thumbnailImageUrl = it }
        request.tags?.let { event.tags = it.toMutableList() }
        request.capacity?.let { event.capacity = it }
        request.waitlistEnabled?.let { event.waitlistEnabled = it }
        request.venueName?.let { event.venueName = it }
        request.address?.let { event.address = it }
        request.city?.let { event.city = it }
        request.state?.let { event.state = it }
        request.zipCode?.let { event.zipCode = it }
        request.country?.let { event.country = it }
        request.virtualUrl?.let { event.virtualUrl = it }
        request.dialInNumber?.let { event.dialInNumber = it }
        request.accessCode?.let { event.accessCode = it }
        request.requiresApproval?.let { event.requiresApproval = it }
        request.maxRegistrations?.let { event.maxRegistrations = it }
        request.organizerName?.let { event.organizerName = it }
        request.organizerEmail?.let { event.organizerEmail = it }
        request.organizerPhone?.let { event.organizerPhone = it }
        request.organizerWebsite?.let { event.organizerWebsite = it }
        request.startDateTime?.let { event.startDateTime = it }
        request.endDateTime?.let { event.endDateTime = it }
        request.timezone?.let { event.timezone = it }
        request.agenda?.let { event.agenda = it }
        
        val savedEvent = eventRepository.save(event)
        
        // Update default instance if timing/location changed
        if (request.startDateTime != null || request.venueName != null || 
            request.address != null || request.city != null) {
            updateDefaultInstance(savedEvent)
        }
        
        return toResponse(savedEvent)
    }
    
    private fun updateDefaultInstance(event: Event) {
        event.instances?.firstOrNull()?.let { instance ->
            instance.dateTime = event.startDateTime
            instance.location = buildLocationString(event)
            eventInstanceRepository.save(instance)
        }
    }
    
    override fun deleteEvent(id: UUID) {
        val event = eventRepository.findById(id)
            .orElseThrow { EventNotFoundException(id) }
        
        val eventName = event.name ?: ""
        
        eventRepository.delete(event)
        
        eventPublisher.publish(EventDeleted(
            aggregateId = id,
            eventName = eventName
        ))
        
        logger.info("Deleted event {} ({})", eventName, id)
    }
    
    override fun findEvents(criteria: EventFilterCriteria): List<EventResponse> {
        val sortDirection = if (criteria.sortDirection.lowercase() == "desc") 
            Sort.Direction.DESC else Sort.Direction.ASC
        val sortProperty = when (criteria.sortBy.lowercase()) {
            "name" -> "name"
            "city" -> "city"
            "category" -> "category"
            else -> "startDateTime"
        }
        val sort = Sort.by(sortDirection, sortProperty)
        
        var events = if (criteria.publishedOnly) {
            eventRepository.findByStatus(EventStatus.PUBLISHED, sort)
        } else {
            eventRepository.findAll(sort)
        }
        
        // Apply filters
        criteria.search?.let { query ->
            val lowerQuery = query.lowercase()
            events = events.filter { event ->
                event.name?.lowercase()?.contains(lowerQuery) == true ||
                event.description?.lowercase()?.contains(lowerQuery) == true ||
                event.city?.lowercase()?.contains(lowerQuery) == true
            }
        }
        
        criteria.city?.let { city ->
            events = events.filter { it.city?.lowercase() == city.lowercase() }
        }
        
        criteria.category?.let { category ->
            events = events.filter { it.category == category }
        }
        
        criteria.eventType?.let { type ->
            events = events.filter { it.eventType == type }
        }
        
        return events.map { toResponse(it) }
    }
    
    // ==================== Event Lifecycle ====================
    
    override fun publishEvent(id: UUID): EventResponse {
        val event = eventRepository.findById(id)
            .orElseThrow { EventNotFoundException(id) }
        
        event.status = EventStatus.PUBLISHED
        val savedEvent = eventRepository.save(event)
        
        // Ensure event has at least one instance for registration
        if (savedEvent.instances.isNullOrEmpty()) {
            createDefaultInstance(savedEvent)
        }
        
        eventPublisher.publish(EventPublished(
            aggregateId = savedEvent.id!!,
            eventName = savedEvent.name ?: "",
            organizerEmail = savedEvent.organizerEmail,
            startDateTime = savedEvent.startDateTime
        ))
        
        logger.info("Published event {} ({})", savedEvent.name, savedEvent.id)
        
        return toResponse(savedEvent)
    }
    
    override fun cancelEvent(id: UUID, reason: String?): EventResponse {
        val event = eventRepository.findById(id)
            .orElseThrow { EventNotFoundException(id) }
        
        event.status = EventStatus.CANCELLED
        val savedEvent = eventRepository.save(event)
        
        eventPublisher.publish(EventCancelled(
            aggregateId = savedEvent.id!!,
            eventName = savedEvent.name ?: "",
            reason = reason,
            organizerEmail = savedEvent.organizerEmail
        ))
        
        logger.info("Cancelled event {} ({}) - reason: {}", savedEvent.name, savedEvent.id, reason)
        
        return toResponse(savedEvent)
    }
    
    override fun cloneEvent(id: UUID): EventResponse {
        val original = eventRepository.findById(id)
            .orElseThrow { EventNotFoundException(id) }
        
        val cloned = Event().apply {
            name = "${original.name} (Copy)"
            description = original.description
            status = EventStatus.DRAFT
            eventType = original.eventType
            category = original.category
            bannerImageUrl = original.bannerImageUrl
            thumbnailImageUrl = original.thumbnailImageUrl
            tags = original.tags?.toMutableList() ?: mutableListOf()
            capacity = original.capacity
            waitlistEnabled = original.waitlistEnabled
            venueName = original.venueName
            address = original.address
            city = original.city
            state = original.state
            zipCode = original.zipCode
            country = original.country
            virtualUrl = original.virtualUrl
            dialInNumber = original.dialInNumber
            accessCode = original.accessCode
            requiresApproval = original.requiresApproval
            maxRegistrations = original.maxRegistrations
            organizerName = original.organizerName
            organizerEmail = original.organizerEmail
            organizerPhone = original.organizerPhone
            organizerWebsite = original.organizerWebsite
            startDateTime = original.startDateTime
            endDateTime = original.endDateTime
            timezone = original.timezone
            agenda = original.agenda
        }
        
        val savedEvent = eventRepository.save(cloned)
        createDefaultInstance(savedEvent)
        
        logger.info("Cloned event {} to new event {}", id, savedEvent.id)
        
        return toResponse(savedEvent)
    }
    
    // ==================== Event Instance Operations ====================
    
    override fun getEventInstance(instanceId: UUID): EventInstanceResponse? {
        return eventInstanceRepository.findById(instanceId)
            .map { toInstanceResponse(it) }
            .orElse(null)
    }
    
    override fun getEventIdForInstance(instanceId: UUID): UUID? {
        return eventInstanceRepository.findById(instanceId)
            .map { it.event?.id }
            .orElse(null)
    }
    
    override fun getEventInstances(eventId: UUID): List<EventInstanceResponse> {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException(eventId) }
        
        return event.instances?.map { toInstanceResponse(it) } ?: emptyList()
    }
    
    // ==================== Mapping ====================
    
    private fun toResponse(event: Event): EventResponse {
        return EventResponse(
            id = event.id!!,
            name = event.name ?: "",
            description = event.description,
            status = event.status ?: EventStatus.DRAFT,
            eventType = event.eventType ?: EventType.IN_PERSON,
            category = event.category,
            bannerImageUrl = event.bannerImageUrl,
            thumbnailImageUrl = event.thumbnailImageUrl,
            tags = event.tags ?: emptyList(),
            capacity = event.capacity,
            waitlistEnabled = event.waitlistEnabled ?: false,
            venueName = event.venueName,
            address = event.address,
            city = event.city,
            state = event.state,
            zipCode = event.zipCode,
            country = event.country,
            virtualUrl = event.virtualUrl,
            dialInNumber = event.dialInNumber,
            accessCode = event.accessCode,
            requiresApproval = event.requiresApproval,
            maxRegistrations = event.maxRegistrations,
            organizerName = event.organizerName,
            organizerEmail = event.organizerEmail,
            organizerPhone = event.organizerPhone,
            organizerWebsite = event.organizerWebsite,
            startDateTime = event.startDateTime,
            endDateTime = event.endDateTime,
            timezone = event.timezone,
            agenda = event.agenda,
            instances = event.instances?.map { toInstanceResponse(it) } ?: emptyList()
        )
    }
    
    private fun toInstanceResponse(instance: EventInstance): EventInstanceResponse {
        return EventInstanceResponse(
            id = instance.id!!,
            startDateTime = instance.dateTime,
            endDateTime = instance.event?.endDateTime,
            location = instance.location,
            capacity = instance.event?.capacity,
            registrationCount = 0  // TODO: Get from registration module
        )
    }
}
