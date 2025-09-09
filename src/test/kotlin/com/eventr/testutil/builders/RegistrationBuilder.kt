package com.eventr.testutil.builders

import com.eventr.model.*
import com.eventr.testutil.TestConstants
import java.util.*

/**
 * Fluent builder for creating Registration entities in tests.
 * Provides sensible defaults and allows easy customization for different test scenarios.
 */
class RegistrationBuilder {
    private var id: UUID = TestConstants.DEFAULT_REGISTRATION_ID
    private var eventInstance: EventInstance? = null
    private var user: User? = null
    private var userEmail: String = TestConstants.DEFAULT_USER_EMAIL
    private var userName: String = TestConstants.DEFAULT_USER_NAME
    private var status: RegistrationStatus = RegistrationStatus.REGISTERED
    private var checkedIn: Boolean = false
    private var formData: String? = null
    
    // ================================================================================
    // Basic Property Setters
    // ================================================================================
    
    fun withId(id: UUID) = apply { this.id = id }
    fun withEventInstance(eventInstance: EventInstance) = apply { this.eventInstance = eventInstance }
    fun withUser(user: User) = apply { 
        this.user = user
        // Sync user details if available
        // Sync user details if available - would implement when User entity is available
    }
    fun withUserEmail(userEmail: String) = apply { this.userEmail = userEmail }
    fun withUserName(userName: String) = apply { this.userName = userName }
    fun withStatus(status: RegistrationStatus) = apply { this.status = status }
    fun withFormData(formData: String) = apply { this.formData = formData }
    
    // ================================================================================
    // User Information Convenience Methods
    // ================================================================================
    
    fun withUserDetails(name: String, email: String) = apply {
        this.userName = name
        this.userEmail = email
    }
    
    fun withAlternativeUser() = apply {
        this.userName = TestConstants.SECONDARY_USER_NAME
        this.userEmail = TestConstants.SECONDARY_USER_EMAIL
        this.id = TestConstants.SECONDARY_REGISTRATION_ID
    }
    
    fun withThirdUser() = apply {
        this.userName = TestConstants.THIRD_USER_NAME
        this.userEmail = TestConstants.THIRD_USER_EMAIL
        this.id = TestConstants.uniqueId()
    }
    
    fun withSingleNameUser() = apply {
        this.userName = TestConstants.SINGLE_NAME_USER
        this.userEmail = TestConstants.SINGLE_NAME_EMAIL
    }
    
    fun withUniqueUser(baseName: String = "User") = apply {
        this.userName = TestConstants.uniqueName(baseName)
        this.userEmail = TestConstants.uniqueEmail(baseName.lowercase())
        this.id = TestConstants.uniqueId()
    }
    
    // ================================================================================
    // Status Configuration
    // ================================================================================
    
    fun asRegistered() = apply { 
        this.status = RegistrationStatus.REGISTERED
        this.checkedIn = false
    }
    
    fun asCancelled() = apply { 
        this.status = RegistrationStatus.CANCELLED
        this.checkedIn = false
    }
    
    fun asCheckedIn() = apply { 
        this.status = RegistrationStatus.CHECKED_IN
        this.checkedIn = true
    }
    
    fun asNoShow() = apply { 
        this.status = RegistrationStatus.NO_SHOW
        this.checkedIn = false
    }
    
    fun asWaitlisted() = apply { 
        this.status = RegistrationStatus.WAITLISTED
        this.checkedIn = false
    }
    
    // ================================================================================
    // Check-in Configuration
    // ================================================================================
    
    fun withCheckIn() = apply { 
        this.checkedIn = true
        if (this.status == RegistrationStatus.REGISTERED) {
            this.status = RegistrationStatus.CHECKED_IN
        }
    }
    
    fun withoutCheckIn() = apply { 
        this.checkedIn = false
        if (this.status == RegistrationStatus.CHECKED_IN) {
            this.status = RegistrationStatus.REGISTERED
        }
    }
    
    // ================================================================================
    // Form Data Configuration
    // ================================================================================
    
    fun withSimpleFormData() = apply {
        this.formData = """
            {
                "dietaryRestrictions": "None",
                "company": "Test Company",
                "jobTitle": "Developer",
                "experience": "Intermediate"
            }
        """.trimIndent()
    }
    
    fun withDetailedFormData() = apply {
        this.formData = """
            {
                "dietaryRestrictions": "Vegetarian",
                "company": "Tech Corp",
                "jobTitle": "Senior Software Engineer",
                "experience": "Advanced",
                "interests": ["AI", "Web Development", "Cloud Computing"],
                "linkedin": "https://linkedin.com/in/test-user",
                "github": "https://github.com/test-user",
                "sessionPreferences": ["keynote", "workshop", "panel"],
                "accommodation": {
                    "wheelchair": false,
                    "signLanguage": false,
                    "other": ""
                },
                "emergencyContact": {
                    "name": "Emergency Contact",
                    "phone": "+1-555-0000",
                    "relationship": "Family"
                }
            }
        """.trimIndent()
    }
    
    fun withCustomFormData(data: String) = apply {
        this.formData = data
    }
    
    // ================================================================================
    // Relationship Builders
    // ================================================================================
    
    fun withSessionRegistrations(sessionIds: List<UUID>): RegistrationWithSessionsBuilder {
        return RegistrationWithSessionsBuilder(this, sessionIds)
    }
    
    fun withSessionRegistrations(vararg sessionIds: UUID): RegistrationWithSessionsBuilder {
        return withSessionRegistrations(sessionIds.toList())
    }
    
    // ================================================================================
    // Preset Configurations
    // ================================================================================
    
    fun asConferenceAttendee() = apply {
        withDetailedFormData()
        asRegistered()
        withUserDetails("Conference Attendee", "attendee@company.com")
    }
    
    fun asWorkshopParticipant() = apply {
        this.formData = """
            {
                "experience": "Beginner",
                "laptop": true,
                "programmingLanguage": "Java",
                "expectations": "Learn new skills"
            }
        """.trimIndent()
        asRegistered()
    }
    
    fun asVIPAttendee() = apply {
        this.formData = """
            {
                "vipStatus": true,
                "company": "Premium Sponsor",
                "jobTitle": "CTO",
                "specialRequests": "VIP seating"
            }
        """.trimIndent()
        asRegistered()
    }
    
    fun asSpeaker() = apply {
        this.formData = """
            {
                "role": "speaker",
                "bio": "Expert speaker with 10+ years experience",
                "topics": ["Technology", "Leadership", "Innovation"],
                "socialMedia": {
                    "twitter": "@speaker_handle",
                    "linkedin": "linkedin.com/in/speaker"
                }
            }
        """.trimIndent()
        asRegistered()
        withUserDetails("Speaker Name", "speaker@example.com")
    }
    
    fun asVolunteer() = apply {
        this.formData = """
            {
                "role": "volunteer",
                "availability": ["full-day"],
                "skills": ["event management", "technical support"],
                "tshirtSize": "M"
            }
        """.trimIndent()
        asRegistered()
    }
    
    fun asLastMinuteRegistration() = apply {
        // Registration just happened recently
        asRegistered()
        withSimpleFormData()
    }
    
    fun asEarlyBird() = apply {
        this.formData = """
            {
                "registrationType": "early-bird",
                "discount": "25%",
                "paymentStatus": "completed"
            }
        """.trimIndent()
        asRegistered()
    }
    
    // ================================================================================
    // Test Scenario Configurations
    // ================================================================================
    
    fun forLoadTesting(index: Int) = apply {
        withUserDetails("Load Test User $index", "loadtest$index@example.com")
        withId(UUID.randomUUID())
        asRegistered()
    }
    
    fun forBulkOperations(batchIndex: Int, userIndex: Int) = apply {
        withUserDetails("Batch $batchIndex User $userIndex", "batch${batchIndex}user${userIndex}@example.com")
        withId(UUID.randomUUID())
        asRegistered()
    }
    
    // ================================================================================
    // Build Method
    // ================================================================================
    
    fun build(): Registration {
        return Registration(id = id).apply {
            this.eventInstance = this@RegistrationBuilder.eventInstance
            this.user = this@RegistrationBuilder.user
            this.userEmail = this@RegistrationBuilder.userEmail
            this.userName = this@RegistrationBuilder.userName
            this.status = this@RegistrationBuilder.status
            this.checkedIn = this@RegistrationBuilder.checkedIn
            this.formData = this@RegistrationBuilder.formData
        }
    }
}