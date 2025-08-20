package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class SessionType {
    KEYNOTE, PRESENTATION, WORKSHOP, LAB, BREAKOUT, NETWORKING, MEAL, BREAK, TRAINING, PANEL, QA, OTHER
}

@Entity
data class Session(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event? = null,
    
    var title: String = "",
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Enumerated(EnumType.STRING)
    var type: SessionType = SessionType.PRESENTATION,
    
    var startTime: LocalDateTime? = null,
    var endTime: LocalDateTime? = null,
    
    var location: String? = null,
    var room: String? = null,
    var building: String? = null,
    
    var capacity: Int? = null,
    var isRegistrationRequired: Boolean = true,
    var isWaitlistEnabled: Boolean = true,
    
    // Speaker/Presenter information
    var presenter: String? = null,
    var presenterTitle: String? = null,
    var presenterBio: String? = null,
    var presenterEmail: String? = null,
    
    // Session materials and resources
    var materialUrl: String? = null,
    var recordingUrl: String? = null,
    var slidesUrl: String? = null,
    
    // Session requirements
    var prerequisites: String? = null,
    var targetAudience: String? = null,
    var difficultyLevel: String? = null, // Beginner, Intermediate, Advanced
    
    // Session tags for categorization
    @ElementCollection
    @CollectionTable(name = "session_tags", joinColumns = [JoinColumn(name = "session_id")])
    @Column(name = "tag")
    var tags: MutableList<String>? = mutableListOf(),
    
    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], orphanRemoval = true)
    var sessionRegistrations: MutableList<SessionRegistration>? = mutableListOf(),
    
    var isActive: Boolean = true,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)