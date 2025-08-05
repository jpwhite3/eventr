package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class EventInstance(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne
    @JoinColumn(name = "event_id")
    var event: Event? = null,
    
    var dateTime: LocalDateTime? = null,
    
    var location: String? = null
)
