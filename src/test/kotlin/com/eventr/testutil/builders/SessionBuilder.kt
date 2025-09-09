package com.eventr.testutil.builders

import com.eventr.model.*
import com.eventr.testutil.TestConstants
import java.time.LocalDateTime
import java.util.*

/**
 * Fluent builder for creating Session entities in tests.
 * Provides sensible defaults and allows easy customization for different test scenarios.
 */
class SessionBuilder {
    private var id: UUID = TestConstants.DEFAULT_SESSION_ID
    private var event: Event? = null
    private var title: String = TestConstants.DEFAULT_SESSION_TITLE
    private var description: String? = TestConstants.DEFAULT_SESSION_DESCRIPTION
    private var type: SessionType = SessionType.PRESENTATION
    private var startTime: LocalDateTime = TestConstants.SESSION_START_TIME
    private var endTime: LocalDateTime = TestConstants.SESSION_END_TIME
    private var location: String? = TestConstants.DEFAULT_SESSION_LOCATION
    private var room: String? = TestConstants.DEFAULT_SESSION_LOCATION
    private var building: String? = null
    private var capacity: Int? = TestConstants.DEFAULT_SESSION_CAPACITY
    private var isRegistrationRequired: Boolean = true
    private var isWaitlistEnabled: Boolean = true
    
    // Speaker/Presenter information
    private var presenter: String? = TestConstants.DEFAULT_PRESENTER_NAME
    private var presenterTitle: String? = null
    private var presenterBio: String? = TestConstants.DEFAULT_PRESENTER_BIO
    private var presenterEmail: String? = TestConstants.DEFAULT_PRESENTER_EMAIL
    
    // Session materials and resources
    private var materialUrl: String? = null
    private var recordingUrl: String? = null
    private var slidesUrl: String? = null
    
    // Session requirements
    private var prerequisites: String? = null
    private var targetAudience: String? = null
    private var difficultyLevel: String? = null
    
    // Session tags for categorization
    private var tags: MutableList<String> = mutableListOf()
    
    // Administrative fields
    private var isActive: Boolean = true
    private var createdAt: LocalDateTime = TestConstants.BASE_TIME
    private var updatedAt: LocalDateTime = TestConstants.BASE_TIME
    
    // ================================================================================
    // Basic Property Setters
    // ================================================================================
    
    fun withId(id: UUID) = apply { this.id = id }
    fun withEvent(event: Event) = apply { this.event = event }
    fun withTitle(title: String) = apply { this.title = title }
    fun withDescription(description: String?) = apply { this.description = description }
    fun withType(type: SessionType) = apply { this.type = type }
    fun withCapacity(capacity: Int?) = apply { this.capacity = capacity }
    
    // ================================================================================
    // Timing Configuration
    // ================================================================================
    
    fun withStartTime(startTime: LocalDateTime) = apply { this.startTime = startTime }
    fun withEndTime(endTime: LocalDateTime) = apply { this.endTime = endTime }
    fun withDuration(startTime: LocalDateTime, durationMinutes: Long) = apply {
        this.startTime = startTime
        this.endTime = startTime.plusMinutes(durationMinutes)
    }
    fun withTimeSlot(startTime: LocalDateTime, endTime: LocalDateTime) = apply {
        this.startTime = startTime
        this.endTime = endTime
    }
    
    // ================================================================================
    // Location Configuration
    // ================================================================================
    
    fun withLocation(location: String) = apply { this.location = location }
    fun withRoom(room: String) = apply { 
        this.room = room
        this.location = room
    }
    fun withBuilding(building: String) = apply { this.building = building }
    fun withFullLocation(building: String, room: String) = apply {
        this.building = building
        this.room = room
        this.location = "$building, $room"
    }
    
    // ================================================================================
    // Registration Settings
    // ================================================================================
    
    fun requiresRegistration() = apply { this.isRegistrationRequired = true }
    fun noRegistrationRequired() = apply { this.isRegistrationRequired = false }
    fun withWaitlist() = apply { this.isWaitlistEnabled = true }
    fun withoutWaitlist() = apply { this.isWaitlistEnabled = false }
    
    // ================================================================================
    // Speaker/Presenter Configuration
    // ================================================================================
    
    fun withPresenter(name: String, bio: String? = null, email: String? = null, title: String? = null) = apply {
        this.presenter = name
        this.presenterBio = bio
        this.presenterEmail = email
        this.presenterTitle = title
    }
    
    fun withoutPresenter() = apply {
        this.presenter = null
        this.presenterBio = null
        this.presenterEmail = null
        this.presenterTitle = null
    }
    
    // ================================================================================
    // Materials and Resources
    // ================================================================================
    
    fun withMaterials(materialUrl: String) = apply { this.materialUrl = materialUrl }
    fun withSlides(slidesUrl: String) = apply { this.slidesUrl = slidesUrl }
    fun withRecording(recordingUrl: String) = apply { this.recordingUrl = recordingUrl }
    fun withAllMaterials(materialUrl: String, slidesUrl: String, recordingUrl: String? = null) = apply {
        this.materialUrl = materialUrl
        this.slidesUrl = slidesUrl
        this.recordingUrl = recordingUrl
    }
    
    // ================================================================================
    // Requirements and Audience
    // ================================================================================
    
    fun withPrerequisites(prerequisites: String) = apply { this.prerequisites = prerequisites }
    fun withTargetAudience(targetAudience: String) = apply { this.targetAudience = targetAudience }
    fun withDifficultyLevel(level: String) = apply { this.difficultyLevel = level }
    
    fun forBeginners() = apply { this.difficultyLevel = "Beginner" }
    fun forIntermediate() = apply { this.difficultyLevel = "Intermediate" }
    fun forAdvanced() = apply { this.difficultyLevel = "Advanced" }
    
    // ================================================================================
    // Tags and Categorization
    // ================================================================================
    
    fun withTags(vararg tags: String) = apply { 
        this.tags = tags.toMutableList()
    }
    fun withTag(tag: String) = apply { this.tags.add(tag) }
    fun withoutTags() = apply { this.tags.clear() }
    
    // ================================================================================
    // Administrative Configuration
    // ================================================================================
    
    fun asActive() = apply { this.isActive = true }
    fun asInactive() = apply { this.isActive = false }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }
    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }
    
    // ================================================================================
    // Session Type Presets
    // ================================================================================
    
    fun asKeynote() = apply {
        this.type = SessionType.KEYNOTE
        this.title = TestConstants.KEYNOTE_SESSION_TITLE
        this.capacity = TestConstants.LARGE_CAPACITY
        this.withDuration(startTime, 60) // 1 hour keynote
        this.withTags("keynote", "opening", "featured")
    }
    
    fun asWorkshop() = apply {
        this.type = SessionType.WORKSHOP
        this.title = TestConstants.WORKSHOP_SESSION_TITLE
        this.capacity = TestConstants.SMALL_CAPACITY
        this.withDuration(startTime, 180) // 3 hour workshop
        this.withTags("workshop", "hands-on", "practical")
        this.withPrerequisites("Laptop required")
    }
    
    fun asPresentation() = apply {
        this.type = SessionType.PRESENTATION
        this.capacity = TestConstants.MEDIUM_CAPACITY
        this.withDuration(startTime, 45) // 45 minute presentation
        this.withTags("presentation", "talk")
    }
    
    fun asPanel() = apply {
        this.type = SessionType.PANEL
        this.title = TestConstants.PANEL_SESSION_TITLE
        this.capacity = TestConstants.LARGE_CAPACITY
        this.withDuration(startTime, 60) // 1 hour panel
        this.withTags("panel", "discussion", "q&a")
        this.withoutPresenter() // Panels have multiple speakers
    }
    
    fun asBreakout() = apply {
        this.type = SessionType.BREAKOUT
        this.capacity = TestConstants.SMALL_CAPACITY
        this.withDuration(startTime, 30) // 30 minute breakout
        this.withTags("breakout", "discussion")
    }
    
    fun asNetworking() = apply {
        this.type = SessionType.NETWORKING
        this.title = "Networking Break"
        this.capacity = TestConstants.LARGE_CAPACITY
        this.withDuration(startTime, 30) // 30 minute break
        this.withTags("networking", "break")
        this.noRegistrationRequired()
        this.withoutPresenter()
    }
    
    fun asMeal() = apply {
        this.type = SessionType.MEAL
        this.title = "Lunch Break"
        this.capacity = TestConstants.LARGE_CAPACITY
        this.withDuration(startTime, 60) // 1 hour meal
        this.withTags("meal", "lunch", "break")
        this.noRegistrationRequired()
        this.withoutPresenter()
    }
    
    fun asLab() = apply {
        this.type = SessionType.LAB
        this.title = "Hands-on Lab"
        this.capacity = TestConstants.SMALL_CAPACITY
        this.withDuration(startTime, 120) // 2 hour lab
        this.withTags("lab", "hands-on", "practical")
        this.withPrerequisites("Development environment setup required")
    }
    
    // ================================================================================
    // Timing Presets
    // ================================================================================
    
    fun asShortSession() = apply { this.withDuration(startTime, 30) } // 30 minutes
    fun asStandardSession() = apply { this.withDuration(startTime, 60) } // 1 hour
    fun asLongSession() = apply { this.withDuration(startTime, 90) } // 1.5 hours
    fun asExtendedSession() = apply { this.withDuration(startTime, 180) } // 3 hours
    
    // ================================================================================
    // Relationship Builders
    // ================================================================================
    
    fun withRegistrations(count: Int): SessionWithRegistrationsBuilder {
        return SessionWithRegistrationsBuilder(this, count)
    }
    
    fun withResources(vararg resources: Resource): SessionWithResourcesBuilder {
        return SessionWithResourcesBuilder(this, resources.toList())
    }
    
    // ================================================================================
    // Build Method
    // ================================================================================
    
    fun build(): Session {
        return Session(id = id).apply {
            this.event = this@SessionBuilder.event
            this.title = this@SessionBuilder.title
            this.description = this@SessionBuilder.description
            this.type = this@SessionBuilder.type
            this.startTime = this@SessionBuilder.startTime
            this.endTime = this@SessionBuilder.endTime
            this.location = this@SessionBuilder.location
            this.room = this@SessionBuilder.room
            this.building = this@SessionBuilder.building
            this.capacity = this@SessionBuilder.capacity
            this.isRegistrationRequired = this@SessionBuilder.isRegistrationRequired
            this.isWaitlistEnabled = this@SessionBuilder.isWaitlistEnabled
            
            // Speaker/Presenter information
            this.presenter = this@SessionBuilder.presenter
            this.presenterTitle = this@SessionBuilder.presenterTitle
            this.presenterBio = this@SessionBuilder.presenterBio
            this.presenterEmail = this@SessionBuilder.presenterEmail
            
            // Materials
            this.materialUrl = this@SessionBuilder.materialUrl
            this.recordingUrl = this@SessionBuilder.recordingUrl
            this.slidesUrl = this@SessionBuilder.slidesUrl
            
            // Requirements
            this.prerequisites = this@SessionBuilder.prerequisites
            this.targetAudience = this@SessionBuilder.targetAudience
            this.difficultyLevel = this@SessionBuilder.difficultyLevel
            
            // Tags
            this.tags = this@SessionBuilder.tags
            
            // Administrative
            this.isActive = this@SessionBuilder.isActive
            this.createdAt = this@SessionBuilder.createdAt
            this.updatedAt = this@SessionBuilder.updatedAt
        }
    }
}