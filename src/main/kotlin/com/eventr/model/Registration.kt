package com.eventr.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
data class Registration(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne
    @JoinColumn(name = "event_instance_id")
    var eventInstance: EventInstance? = null,
    
    var userEmail: String? = null,
    
    var userName: String? = null,
    
    @Enumerated(EnumType.STRING)
    var status: RegistrationStatus? = null,
    
    var checkedIn: Boolean = false,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "TEXT")
    var formData: String? = null,
    
    @OneToMany(mappedBy = "registration", cascade = [CascadeType.ALL], orphanRemoval = true)
    var sessionRegistrations: MutableList<SessionRegistration>? = mutableListOf()
)
