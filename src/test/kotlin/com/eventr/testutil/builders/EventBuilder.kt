package com.eventr.testutil.builders

import com.eventr.model.*
import com.eventr.testutil.TestConstants
import java.time.LocalDateTime
import java.util.*

/**
 * Fluent builder for creating Event entities in tests.
 * Provides sensible defaults and allows easy customization for different test scenarios.
 */
class EventBuilder {
    private var id: UUID = TestConstants.DEFAULT_EVENT_ID
    private var name: String = TestConstants.DEFAULT_EVENT_NAME
    private var description: String? = TestConstants.DEFAULT_EVENT_DESCRIPTION
    private var status: EventStatus = EventStatus.PUBLISHED
    private var eventType: EventType = EventType.IN_PERSON
    private var category: EventCategory = EventCategory.TECHNOLOGY
    private var bannerImageUrl: String? = null
    private var thumbnailImageUrl: String? = null
    private var tags: MutableList<String> = TestConstants.DEFAULT_TAGS.toMutableList()
    private var capacity: Int = TestConstants.DEFAULT_EVENT_CAPACITY
    private var waitlistEnabled: Boolean = false
    
    // Location fields for in-person events
    private var venueName: String? = TestConstants.DEFAULT_EVENT_LOCATION
    private var address: String? = "123 Test Street"
    private var city: String? = TestConstants.DEFAULT_EVENT_CITY
    private var state: String? = TestConstants.DEFAULT_EVENT_STATE
    private var zipCode: String? = "12345"
    private var country: String? = "USA"
    
    // Virtual event fields
    private var virtualUrl: String? = null
    private var dialInNumber: String? = null
    private var accessCode: String? = null
    
    // Registration settings
    private var requiresApproval: Boolean = false
    private var maxRegistrations: Int? = capacity
    
    // Organizer information
    private var organizerName: String? = "Test Organizer"
    private var organizerEmail: String? = "organizer@example.com"
    private var organizerPhone: String? = TestConstants.DEFAULT_USER_PHONE
    private var organizerWebsite: String? = null
    
    // Event timing
    private var startDateTime: LocalDateTime = TestConstants.EVENT_START_TIME
    private var endDateTime: LocalDateTime = TestConstants.EVENT_END_TIME
    private var timezone: String = "UTC"
    
    // Agenda/Schedule
    private var agenda: String? = null
    
    // Session configuration
    private var isMultiSession: Boolean = false
    private var allowSessionSelection: Boolean = false
    
    // ================================================================================
    // Basic Property Setters
    // ================================================================================
    
    fun withId(id: UUID) = apply { this.id = id }
    fun withName(name: String) = apply { this.name = name }
    fun withDescription(description: String?) = apply { this.description = description }
    fun withStatus(status: EventStatus) = apply { this.status = status }
    fun withEventType(eventType: EventType) = apply { this.eventType = eventType }
    fun withCategory(category: EventCategory) = apply { this.category = category }
    fun withCapacity(capacity: Int) = apply { 
        this.capacity = capacity
        this.maxRegistrations = capacity
    }
    
    // ================================================================================
    // Image and Media
    // ================================================================================
    
    fun withBannerImage(url: String) = apply { this.bannerImageUrl = url }
    fun withThumbnailImage(url: String) = apply { this.thumbnailImageUrl = url }
    fun withImages(bannerUrl: String, thumbnailUrl: String) = apply {
        this.bannerImageUrl = bannerUrl
        this.thumbnailImageUrl = thumbnailUrl
    }
    
    // ================================================================================
    // Tags and Categorization
    // ================================================================================
    
    fun withTags(vararg tags: String) = apply { 
        this.tags = tags.toMutableList()
    }
    fun withTag(tag: String) = apply { this.tags.add(tag) }
    fun withoutTags() = apply { this.tags.clear() }
    
    // ================================================================================
    // Location Configuration
    // ================================================================================
    
    fun withLocation(venueName: String, city: String, state: String? = null) = apply {
        this.venueName = venueName
        this.city = city
        this.state = state
        this.eventType = EventType.IN_PERSON
    }
    
    fun withFullAddress(
        venueName: String,
        address: String,
        city: String,
        state: String,
        zipCode: String,
        country: String = "USA"
    ) = apply {
        this.venueName = venueName
        this.address = address
        this.city = city
        this.state = state
        this.zipCode = zipCode
        this.country = country
        this.eventType = EventType.IN_PERSON
    }
    
    fun asVirtual(url: String, dialIn: String? = null, accessCode: String? = null) = apply {
        this.eventType = EventType.VIRTUAL
        this.virtualUrl = url
        this.dialInNumber = dialIn
        this.accessCode = accessCode
        // Clear physical location for virtual events
        this.venueName = null
        this.address = null
    }
    
    fun asHybrid(venueName: String, virtualUrl: String) = apply {
        this.eventType = EventType.HYBRID
        this.venueName = venueName
        this.virtualUrl = virtualUrl
    }
    
    // ================================================================================
    // Registration Settings
    // ================================================================================
    
    fun withWaitlist() = apply { this.waitlistEnabled = true }
    fun withoutWaitlist() = apply { this.waitlistEnabled = false }
    fun requiresApproval() = apply { this.requiresApproval = true }
    fun withMaxRegistrations(max: Int) = apply { this.maxRegistrations = max }
    
    // ================================================================================
    // Organizer Information
    // ================================================================================
    
    fun withOrganizer(name: String, email: String, phone: String? = null, website: String? = null) = apply {
        this.organizerName = name
        this.organizerEmail = email
        this.organizerPhone = phone
        this.organizerWebsite = website
    }
    
    // ================================================================================
    // Timing Configuration
    // ================================================================================
    
    fun withStartTime(startDateTime: LocalDateTime) = apply { this.startDateTime = startDateTime }
    fun withEndTime(endDateTime: LocalDateTime) = apply { this.endDateTime = endDateTime }
    fun withDuration(startDateTime: LocalDateTime, durationHours: Long) = apply {
        this.startDateTime = startDateTime
        this.endDateTime = startDateTime.plusHours(durationHours)
    }
    fun withTimezone(timezone: String) = apply { this.timezone = timezone }
    
    // ================================================================================
    // Content Configuration
    // ================================================================================
    
    fun withAgenda(agenda: String) = apply { this.agenda = agenda }
    
    // ================================================================================
    // Session Configuration
    // ================================================================================
    
    fun asMultiSession() = apply { 
        this.isMultiSession = true
        this.allowSessionSelection = true
    }
    fun asSingleSession() = apply { 
        this.isMultiSession = false
        this.allowSessionSelection = false
    }
    fun allowSessionSelection() = apply { this.allowSessionSelection = true }
    
    // ================================================================================
    // Status Configurations
    // ================================================================================
    
    fun asDraft() = apply { this.status = EventStatus.DRAFT }
    fun asPublished() = apply { this.status = EventStatus.PUBLISHED }
    
    // ================================================================================
    // Preset Configurations
    // ================================================================================
    
    fun asConference() = apply {
        this.name = TestConstants.CONFERENCE_EVENT_NAME
        this.category = EventCategory.TECHNOLOGY
        this.capacity = TestConstants.LARGE_CAPACITY
        this.isMultiSession = true
        this.allowSessionSelection = true
        this.withTags("conference", "technology", "networking")
        this.withDuration(startDateTime, 8) // Full day conference
    }
    
    fun asWorkshop() = apply {
        this.name = TestConstants.WORKSHOP_EVENT_NAME
        this.category = EventCategory.EDUCATION
        this.capacity = TestConstants.SMALL_CAPACITY
        this.withTags("workshop", "hands-on", "learning")
        this.withDuration(startDateTime, 4) // Half day workshop
    }
    
    fun asMeetup() = apply {
        this.name = TestConstants.MEETUP_EVENT_NAME
        this.category = EventCategory.COMMUNITY
        this.capacity = TestConstants.MEDIUM_CAPACITY
        this.withTags("meetup", "community", "networking")
        this.withDuration(startDateTime, 3) // Evening meetup
    }
    
    fun withAllOptionalFields() = apply {
        this.description = "Comprehensive event with all optional fields populated"
        this.bannerImageUrl = TestConstants.TEST_IMAGE_URL
        this.thumbnailImageUrl = TestConstants.TEST_IMAGE_URL
        this.agenda = "Full day agenda with multiple sessions and networking breaks"
        this.organizerWebsite = "https://organizer.example.com"
        this.waitlistEnabled = true
    }
    
    // ================================================================================
    // Timing Presets
    // ================================================================================
    
    fun asPastEvent() = apply {
        this.startDateTime = TestConstants.PAST_EVENT_START
        this.endDateTime = TestConstants.PAST_EVENT_END
    }
    
    fun asFutureEvent() = apply {
        this.startDateTime = TestConstants.FUTURE_EVENT_START
        this.endDateTime = TestConstants.FUTURE_EVENT_END
    }
    
    fun asCurrentEvent() = apply {
        val now = LocalDateTime.now()
        this.startDateTime = now.minusHours(1)
        this.endDateTime = now.plusHours(3)
    }
    
    // ================================================================================
    // Build Method
    // ================================================================================
    
    fun build(): Event {
        return Event(id = id).apply {
            this.name = this@EventBuilder.name
            this.description = this@EventBuilder.description
            this.status = this@EventBuilder.status
            this.eventType = this@EventBuilder.eventType
            this.category = this@EventBuilder.category
            this.bannerImageUrl = this@EventBuilder.bannerImageUrl
            this.thumbnailImageUrl = this@EventBuilder.thumbnailImageUrl
            this.tags = this@EventBuilder.tags
            this.capacity = this@EventBuilder.capacity
            this.waitlistEnabled = this@EventBuilder.waitlistEnabled
            
            // Location fields
            this.venueName = this@EventBuilder.venueName
            this.address = this@EventBuilder.address
            this.city = this@EventBuilder.city
            this.state = this@EventBuilder.state
            this.zipCode = this@EventBuilder.zipCode
            this.country = this@EventBuilder.country
            
            // Virtual fields
            this.virtualUrl = this@EventBuilder.virtualUrl
            this.dialInNumber = this@EventBuilder.dialInNumber
            this.accessCode = this@EventBuilder.accessCode
            
            // Registration settings
            this.requiresApproval = this@EventBuilder.requiresApproval
            this.maxRegistrations = this@EventBuilder.maxRegistrations
            
            // Organizer information
            this.organizerName = this@EventBuilder.organizerName
            this.organizerEmail = this@EventBuilder.organizerEmail
            this.organizerPhone = this@EventBuilder.organizerPhone
            this.organizerWebsite = this@EventBuilder.organizerWebsite
            
            // Timing
            this.startDateTime = this@EventBuilder.startDateTime
            this.endDateTime = this@EventBuilder.endDateTime
            this.timezone = this@EventBuilder.timezone
            
            // Content
            this.agenda = this@EventBuilder.agenda
            
            // Session configuration
            this.isMultiSession = this@EventBuilder.isMultiSession
            this.allowSessionSelection = this@EventBuilder.allowSessionSelection
        }
    }
}