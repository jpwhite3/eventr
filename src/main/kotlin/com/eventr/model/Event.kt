package com.eventr.model

import jakarta.persistence.*
import java.util.UUID

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
    
    var bannerImageUrl: String? = null,
    
    var thumbnailImageUrl: String? = null,
    
    @ElementCollection
    @CollectionTable(name = "event_tags", joinColumns = [JoinColumn(name = "event_id")])
    @Column(name = "tag")
    var tags: MutableList<String>? = mutableListOf(),
    
    var capacity: Int? = null,
    
    var waitlistEnabled: Boolean? = false,
    
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    var instances: MutableList<EventInstance>? = mutableListOf()
)
