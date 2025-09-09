package com.eventr.testutil.builders

import com.eventr.service.*
import com.eventr.testutil.TestConstants
import java.util.*

/**
 * Builders for DTO objects used in API requests and responses.
 * These builders help create valid DTOs for controller and service testing.
 */

// ================================================================================
// Event DTO Builders
// ================================================================================

class CreateEventDtoBuilder {
    private var name: String = TestConstants.DEFAULT_EVENT_NAME
    private var description: String = TestConstants.DEFAULT_EVENT_DESCRIPTION
    private var eventType: String = "IN_PERSON"
    private var category: String = "TECHNOLOGY"
    private var capacity: Int = TestConstants.DEFAULT_EVENT_CAPACITY
    private var venueName: String = TestConstants.DEFAULT_EVENT_LOCATION
    private var city: String = TestConstants.DEFAULT_EVENT_CITY
    private var state: String = TestConstants.DEFAULT_EVENT_STATE
    private var startDateTime: String = TestConstants.EVENT_START_TIME.toString()
    private var endDateTime: String = TestConstants.EVENT_END_TIME.toString()
    
    fun withName(name: String) = apply { this.name = name }
    fun withDescription(description: String) = apply { this.description = description }
    fun withEventType(eventType: String) = apply { this.eventType = eventType }
    fun withCategory(category: String) = apply { this.category = category }
    fun withCapacity(capacity: Int) = apply { this.capacity = capacity }
    fun withVenue(venueName: String, city: String, state: String) = apply {
        this.venueName = venueName
        this.city = city
        this.state = state
    }
    fun withTiming(startDateTime: String, endDateTime: String) = apply {
        this.startDateTime = startDateTime
        this.endDateTime = endDateTime
    }
    
    // Note: The actual DTO structure would need to be implemented based on the real DTOs
    fun build(): Map<String, Any> = mapOf(
        "name" to name,
        "description" to description,
        "eventType" to eventType,
        "category" to category,
        "capacity" to capacity,
        "venueName" to venueName,
        "city" to city,
        "state" to state,
        "startDateTime" to startDateTime,
        "endDateTime" to endDateTime
    )
}

class UpdateEventDtoBuilder {
    private var name: String? = null
    private var description: String? = null
    private var capacity: Int? = null
    private var status: String? = null
    
    fun withName(name: String) = apply { this.name = name }
    fun withDescription(description: String) = apply { this.description = description }
    fun withCapacity(capacity: Int) = apply { this.capacity = capacity }
    fun withStatus(status: String) = apply { this.status = status }
    
    fun build(): Map<String, Any?> = mapOf(
        "name" to name,
        "description" to description,
        "capacity" to capacity,
        "status" to status
    ).filterValues { it != null }
}

// ================================================================================
// Session DTO Builders
// ================================================================================

class CreateSessionDtoBuilder {
    private var eventId: UUID = TestConstants.DEFAULT_EVENT_ID
    private var title: String = TestConstants.DEFAULT_SESSION_TITLE
    private var description: String? = TestConstants.DEFAULT_SESSION_DESCRIPTION
    private var startDateTime: String = TestConstants.SESSION_START_TIME.toString()
    private var endDateTime: String = TestConstants.SESSION_END_TIME.toString()
    private var location: String? = TestConstants.DEFAULT_SESSION_LOCATION
    private var speakerName: String? = TestConstants.DEFAULT_PRESENTER_NAME
    private var speakerBio: String? = TestConstants.DEFAULT_PRESENTER_BIO
    private var capacity: Int? = TestConstants.DEFAULT_SESSION_CAPACITY
    private var sessionType: String? = "PRESENTATION"
    private var requirements: List<String>? = TestConstants.DEFAULT_REQUIREMENTS
    private var materials: List<String>? = TestConstants.DEFAULT_MATERIALS
    
    fun withEventId(eventId: UUID) = apply { this.eventId = eventId }
    fun withTitle(title: String) = apply { this.title = title }
    fun withDescription(description: String?) = apply { this.description = description }
    fun withTiming(startDateTime: String, endDateTime: String) = apply {
        this.startDateTime = startDateTime
        this.endDateTime = endDateTime
    }
    fun withLocation(location: String) = apply { this.location = location }
    fun withSpeaker(name: String, bio: String? = null) = apply {
        this.speakerName = name
        this.speakerBio = bio
    }
    fun withCapacity(capacity: Int) = apply { this.capacity = capacity }
    fun withSessionType(sessionType: String) = apply { this.sessionType = sessionType }
    fun withRequirements(requirements: List<String>) = apply { this.requirements = requirements }
    fun withMaterials(materials: List<String>) = apply { this.materials = materials }
    
    fun build(): CreateSessionDto = CreateSessionDto(
        eventId = eventId,
        title = title,
        description = description,
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        location = location,
        speakerName = speakerName,
        speakerBio = speakerBio,
        capacity = capacity,
        sessionType = sessionType,
        requirements = requirements,
        materials = materials
    )
}

class UpdateSessionDtoBuilder {
    private var title: String? = null
    private var description: String? = null
    private var startDateTime: String? = null
    private var endDateTime: String? = null
    private var location: String? = null
    private var speakerName: String? = null
    private var speakerBio: String? = null
    private var capacity: Int? = null
    private var sessionType: String? = null
    private var isActive: Boolean? = null
    private var requirements: List<String>? = null
    private var materials: List<String>? = null
    
    fun withTitle(title: String) = apply { this.title = title }
    fun withDescription(description: String) = apply { this.description = description }
    fun withTiming(startDateTime: String, endDateTime: String) = apply {
        this.startDateTime = startDateTime
        this.endDateTime = endDateTime
    }
    fun withLocation(location: String) = apply { this.location = location }
    fun withSpeaker(name: String, bio: String? = null) = apply {
        this.speakerName = name
        this.speakerBio = bio
    }
    fun withCapacity(capacity: Int) = apply { this.capacity = capacity }
    fun withSessionType(sessionType: String) = apply { this.sessionType = sessionType }
    fun withActiveStatus(isActive: Boolean) = apply { this.isActive = isActive }
    fun withRequirements(requirements: List<String>) = apply { this.requirements = requirements }
    fun withMaterials(materials: List<String>) = apply { this.materials = materials }
    
    fun build(): UpdateSessionDto = UpdateSessionDto(
        title = title,
        description = description,
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        location = location,
        speakerName = speakerName,
        speakerBio = speakerBio,
        capacity = capacity,
        sessionType = sessionType,
        isActive = isActive,
        requirements = requirements,
        materials = materials
    )
}

// ================================================================================
// Resource DTO Builders
// ================================================================================

class CreateResourceDtoBuilder {
    private var name: String = TestConstants.DEFAULT_RESOURCE_NAME
    private var type: String = "ROOM"
    private var description: String? = TestConstants.DEFAULT_RESOURCE_DESCRIPTION
    private var location: String? = TestConstants.DEFAULT_RESOURCE_LOCATION
    private var capacity: Int? = TestConstants.DEFAULT_RESOURCE_CAPACITY
    private var hourlyRate: Double? = TestConstants.DEFAULT_HOURLY_RATE
    private var features: List<String> = listOf("projector", "whiteboard")
    private var imageUrl: String? = null
    private var contactEmail: String? = TestConstants.DEFAULT_CONTACT_EMAIL
    private var contactPhone: String? = TestConstants.DEFAULT_CONTACT_PHONE
    private var bookingInstructions: String? = null
    
    fun withName(name: String) = apply { this.name = name }
    fun withType(type: String) = apply { this.type = type }
    fun withDescription(description: String?) = apply { this.description = description }
    fun withLocation(location: String) = apply { this.location = location }
    fun withCapacity(capacity: Int) = apply { this.capacity = capacity }
    fun withHourlyRate(rate: Double) = apply { this.hourlyRate = rate }
    fun withFeatures(features: List<String>) = apply { this.features = features }
    fun withImage(imageUrl: String) = apply { this.imageUrl = imageUrl }
    fun withContact(email: String, phone: String? = null) = apply {
        this.contactEmail = email
        this.contactPhone = phone
    }
    fun withBookingInstructions(instructions: String) = apply { this.bookingInstructions = instructions }
    
    // Note: Would use actual CreateResourceDto when available
    fun build(): Map<String, Any?> = mapOf(
        "name" to name,
        "type" to type,
        "description" to description,
        "location" to location,
        "capacity" to capacity,
        "hourlyRate" to hourlyRate,
        "features" to features,
        "imageUrl" to imageUrl,
        "contactEmail" to contactEmail,
        "contactPhone" to contactPhone,
        "bookingInstructions" to bookingInstructions
    ).filterValues { it != null }
}

class UpdateResourceDtoBuilder {
    private var name: String? = null
    private var type: String? = null
    private var capacity: Int? = null
    private var isAvailable: Boolean? = null
    private var hourlyRate: Double? = null
    private var description: String? = null
    private var location: String? = null
    private var features: List<String>? = null
    private var imageUrl: String? = null
    private var contactEmail: String? = null
    private var contactPhone: String? = null
    private var bookingInstructions: String? = null
    
    fun withName(name: String) = apply { this.name = name }
    fun withType(type: String) = apply { this.type = type }
    fun withCapacity(capacity: Int) = apply { this.capacity = capacity }
    fun withAvailability(isAvailable: Boolean) = apply { this.isAvailable = isAvailable }
    fun withHourlyRate(rate: Double) = apply { this.hourlyRate = rate }
    fun withDescription(description: String) = apply { this.description = description }
    fun withLocation(location: String) = apply { this.location = location }
    fun withFeatures(features: List<String>) = apply { this.features = features }
    fun withImage(imageUrl: String) = apply { this.imageUrl = imageUrl }
    fun withContact(email: String, phone: String? = null) = apply {
        this.contactEmail = email
        this.contactPhone = phone
    }
    fun withBookingInstructions(instructions: String) = apply { this.bookingInstructions = instructions }
    
    // Note: Would use actual UpdateResourceDto when available  
    fun build(): Map<String, Any?> = mapOf(
        "name" to name,
        "type" to type,
        "capacity" to capacity,
        "isAvailable" to isAvailable,
        "hourlyRate" to hourlyRate,
        "description" to description,
        "location" to location,
        "features" to features,
        "imageUrl" to imageUrl,
        "contactEmail" to contactEmail,
        "contactPhone" to contactPhone,
        "bookingInstructions" to bookingInstructions
    ).filterValues { it != null }
}