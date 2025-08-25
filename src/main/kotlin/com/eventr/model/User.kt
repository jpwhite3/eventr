package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: UUID? = null
    
    @Column(unique = true, nullable = false)
    var email: String? = null
    
    @Column(nullable = false)
    var firstName: String? = null
    
    @Column(nullable = false)
    var lastName: String? = null
    
    @Column(nullable = false)
    var passwordHash: String? = null
    
    var phone: String? = null
    var company: String? = null
    var jobTitle: String? = null
    var bio: String? = null
    var profileImageUrl: String? = null
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.ATTENDEE
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = UserStatus.ACTIVE
    
    var emailVerified: Boolean = false
    var emailVerificationToken: String? = null
    var passwordResetToken: String? = null
    var passwordResetExpires: LocalDateTime? = null
    
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
    
    var updatedAt: LocalDateTime = LocalDateTime.now()
    var lastLoginAt: LocalDateTime? = null
    
    // Preferences
    var timezone: String? = "UTC"
    var language: String? = "en"
    var marketingEmails: Boolean = true
    var eventReminders: Boolean = true
    var weeklyDigest: Boolean = true
    
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

enum class UserRole {
    SUPER_ADMIN,
    ADMIN,
    ORGANIZER, 
    ATTENDEE
}

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION
}