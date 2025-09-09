package com.eventr.testutil

import java.time.LocalDateTime
import java.util.*

/**
 * Centralized constants for test data to ensure consistency across all test files.
 * This eliminates hardcoded values scattered throughout the test suite and provides
 * a single source of truth for common test data values.
 */
object TestConstants {
    
    // ================================================================================
    // Fixed UUIDs for Predictable Testing
    // ================================================================================
    
    val DEFAULT_EVENT_ID: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
    val DEFAULT_SESSION_ID: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174002")
    val DEFAULT_REGISTRATION_ID: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174003")
    val DEFAULT_RESOURCE_ID: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174004")
    val DEFAULT_USER_ID: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174005")
    val DEFAULT_SESSION_REGISTRATION_ID: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174006")
    
    // Secondary UUIDs for multi-entity scenarios
    val SECONDARY_EVENT_ID: UUID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001")
    val SECONDARY_SESSION_ID: UUID = UUID.fromString("223e4567-e89b-12d3-a456-426614174002")
    val SECONDARY_REGISTRATION_ID: UUID = UUID.fromString("223e4567-e89b-12d3-a456-426614174003")
    val SECONDARY_RESOURCE_ID: UUID = UUID.fromString("223e4567-e89b-12d3-a456-426614174004")
    
    // ================================================================================
    // DateTime Constants
    // ================================================================================
    
    val BASE_TIME: LocalDateTime = LocalDateTime.of(2024, 12, 15, 10, 0, 0)
    val EVENT_START_TIME: LocalDateTime = BASE_TIME
    val EVENT_END_TIME: LocalDateTime = BASE_TIME.plusHours(8)
    val SESSION_START_TIME: LocalDateTime = BASE_TIME.plusHours(1)
    val SESSION_END_TIME: LocalDateTime = BASE_TIME.plusHours(2)
    val REGISTRATION_DATE: LocalDateTime = BASE_TIME.minusDays(7)
    val CHECK_IN_TIME: LocalDateTime = BASE_TIME.minusMinutes(15)
    
    // Future dates for testing upcoming events
    val FUTURE_EVENT_START: LocalDateTime = BASE_TIME.plusDays(30)
    val FUTURE_EVENT_END: LocalDateTime = FUTURE_EVENT_START.plusHours(6)
    
    // Past dates for testing completed events
    val PAST_EVENT_START: LocalDateTime = BASE_TIME.minusDays(30)
    val PAST_EVENT_END: LocalDateTime = PAST_EVENT_START.plusHours(6)
    
    // ================================================================================
    // Event Constants
    // ================================================================================
    
    const val DEFAULT_EVENT_NAME = "Test Event"
    const val DEFAULT_EVENT_DESCRIPTION = "A comprehensive test event for unit testing"
    const val DEFAULT_EVENT_LOCATION = "Test Convention Center"
    const val DEFAULT_EVENT_CITY = "Test City"
    const val DEFAULT_EVENT_STATE = "Test State"
    const val DEFAULT_EVENT_CAPACITY = 100
    
    // Alternative event data
    const val CONFERENCE_EVENT_NAME = "Tech Conference 2024"
    const val WORKSHOP_EVENT_NAME = "Spring Boot Workshop"
    const val MEETUP_EVENT_NAME = "Developer Meetup"
    
    // ================================================================================
    // Session Constants
    // ================================================================================
    
    const val DEFAULT_SESSION_TITLE = "Test Session"
    const val DEFAULT_SESSION_DESCRIPTION = "A test session for unit testing"
    const val DEFAULT_SESSION_LOCATION = "Room A"
    const val DEFAULT_SESSION_CAPACITY = 50
    const val DEFAULT_PRESENTER_NAME = "John Doe"
    const val DEFAULT_PRESENTER_BIO = "Experienced presenter and expert in the field"
    const val DEFAULT_PRESENTER_EMAIL = "presenter@example.com"
    
    // Session variations
    const val KEYNOTE_SESSION_TITLE = "Opening Keynote"
    const val WORKSHOP_SESSION_TITLE = "Hands-on Workshop"
    const val PANEL_SESSION_TITLE = "Expert Panel Discussion"
    
    // ================================================================================
    // User and Registration Constants
    // ================================================================================
    
    const val DEFAULT_USER_NAME = "John Doe"
    const val DEFAULT_USER_EMAIL = "john.doe@example.com"
    const val DEFAULT_USER_PHONE = "+1-555-0123"
    
    // Alternative user data for multi-user scenarios
    const val SECONDARY_USER_NAME = "Jane Smith"
    const val SECONDARY_USER_EMAIL = "jane.smith@example.com"
    const val SECONDARY_USER_PHONE = "+1-555-0456"
    
    const val THIRD_USER_NAME = "Bob Johnson"
    const val THIRD_USER_EMAIL = "bob.johnson@example.com"
    
    // Single name user for edge case testing
    const val SINGLE_NAME_USER = "Madonna"
    const val SINGLE_NAME_EMAIL = "madonna@example.com"
    
    // ================================================================================
    // Resource Constants
    // ================================================================================
    
    const val DEFAULT_RESOURCE_NAME = "Test Conference Room"
    const val DEFAULT_RESOURCE_DESCRIPTION = "A well-equipped conference room for meetings"
    const val DEFAULT_RESOURCE_LOCATION = "Building A, Floor 2"
    const val DEFAULT_RESOURCE_CAPACITY = 50
    const val DEFAULT_HOURLY_RATE = 100.0
    const val DEFAULT_CONTACT_EMAIL = "facilities@example.com"
    const val DEFAULT_CONTACT_PHONE = "+1-555-0123"
    const val DEFAULT_RESOURCE_SPECS = "Projector, Whiteboard, Conference Table"
    const val DEFAULT_RESOURCE_TAGS = "conference, meeting, presentation"
    
    // Resource variations
    const val EQUIPMENT_RESOURCE_NAME = "Projector Equipment"
    const val ROOM_RESOURCE_NAME = "Meeting Room B"
    const val VEHICLE_RESOURCE_NAME = "Company Van"
    
    // ================================================================================
    // Business Logic Constants
    // ================================================================================
    
    // Capacity and limits
    const val SMALL_CAPACITY = 25
    const val MEDIUM_CAPACITY = 50
    const val LARGE_CAPACITY = 100
    const val EXTRA_LARGE_CAPACITY = 500
    
    // Rates and pricing
    const val LOW_HOURLY_RATE = 50.0
    const val STANDARD_HOURLY_RATE = 100.0
    const val PREMIUM_HOURLY_RATE = 200.0
    
    // Check-in and attendance
    const val HIGH_ATTENDANCE_RATE = 85.0
    const val MEDIUM_ATTENDANCE_RATE = 60.0
    const val LOW_ATTENDANCE_RATE = 30.0
    
    // ================================================================================
    // Test Scenario Constants
    // ================================================================================
    
    // File paths and URLs
    const val TEST_IMAGE_URL = "https://example.com/test-image.jpg"
    const val TEST_MATERIAL_URL = "https://example.com/materials.pdf"
    const val TEST_QR_CODE = "TEST_QR_CODE_123"
    const val TEST_QR_CODE_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
    
    // Error messages and validation
    const val INVALID_EMAIL = "invalid-email"
    const val NONEXISTENT_EMAIL = "nonexistent@example.com"
    const val TEST_ERROR_MESSAGE = "Test error message"
    
    // Lists and collections
    val DEFAULT_TAGS = listOf("test", "unit-testing", "example")
    val DEFAULT_REQUIREMENTS = listOf("Basic knowledge", "Laptop required")
    val DEFAULT_MATERIALS = listOf("Slides", "Exercise files", "Reference materials")
    
    // ================================================================================
    // Helper Methods
    // ================================================================================
    
    /**
     * Generates a unique UUID for each test run to avoid conflicts in parallel test execution.
     */
    fun uniqueId(): UUID = UUID.randomUUID()
    
    /**
     * Generates a unique email for each test to avoid conflicts.
     */
    fun uniqueEmail(prefix: String = "test"): String = "${prefix}+${System.currentTimeMillis()}@example.com"
    
    /**
     * Generates a unique name by appending a timestamp.
     */
    fun uniqueName(baseName: String): String = "$baseName ${System.currentTimeMillis()}"
    
    /**
     * Creates a future datetime offset from BASE_TIME.
     */
    fun futureTime(hoursOffset: Long): LocalDateTime = BASE_TIME.plusHours(hoursOffset)
    
    /**
     * Creates a past datetime offset from BASE_TIME.
     */
    fun pastTime(hoursOffset: Long): LocalDateTime = BASE_TIME.minusHours(hoursOffset)
}