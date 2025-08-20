package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class EventType {
    IN_PERSON, VIRTUAL, HYBRID
}

enum class EventCategory {
    MUSIC, NIGHTLIFE, PERFORMING_VISUAL_ARTS, HOLIDAYS, DATING, HOBBIES, BUSINESS, FOOD_DRINK, SPORTS_FITNESS, HEALTH_WELLNESS, TECHNOLOGY, EDUCATION, COMMUNITY, FAMILY, CHARITY_CAUSES, FASHION_BEAUTY, TRAVEL_OUTDOOR, AUTO_BOAT_AIR, FILM_MEDIA, CRAFTS, GAMES, SCIENCE, RELIGION_SPIRITUALITY, HOME_LIFESTYLE, GOVERNMENT_POLITICS, OTHER
}

@Entity
data class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    var name: String? = null,
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Enumerated(EnumType.STRING)
    var status: EventStatus? = null,
    
    @Enumerated(EnumType.STRING)
    var eventType: EventType? = EventType.IN_PERSON,
    
    @Enumerated(EnumType.STRING)
    var category: EventCategory? = null,
    
    var bannerImageUrl: String? = null,
    
    var thumbnailImageUrl: String? = null,
    
    @ElementCollection
    @CollectionTable(name = "event_tags", joinColumns = [JoinColumn(name = "event_id")])
    @Column(name = "tag")
    var tags: MutableList<String>? = mutableListOf(),
    
    var capacity: Int? = null,
    
    var waitlistEnabled: Boolean? = false,
    
    // Location fields for in-person events
    var venueName: String? = null,
    var address: String? = null,
    var city: String? = null,
    var state: String? = null,
    var zipCode: String? = null,
    var country: String? = null,
    
    // Virtual event fields
    var virtualUrl: String? = null,
    var dialInNumber: String? = null,
    var accessCode: String? = null,
    
    // Registration settings
    var requiresApproval: Boolean = false,
    var maxRegistrations: Int? = null,
    
    // Organizer information
    var organizerName: String? = null,
    var organizerEmail: String? = null,
    var organizerPhone: String? = null,
    var organizerWebsite: String? = null,
    
    // Event timing
    var startDateTime: LocalDateTime? = null,
    var endDateTime: LocalDateTime? = null,
    var timezone: String? = "UTC",
    
    // Agenda/Schedule
    @Column(columnDefinition = "TEXT")
    var agenda: String? = null,
    
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    var instances: MutableList<EventInstance>? = mutableListOf()
)
