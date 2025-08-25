package com.eventr.service

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class CheckInStatsService {

    // Mock check-in data
    private val mockEventStats = mutableMapOf<UUID, Map<String, Any>>()
    private val mockSessionStats = mutableMapOf<UUID, Map<String, Any>>()
    private val mockAttendees = mutableMapOf<UUID, List<Map<String, Any>>>()

    init {
        // Initialize mock data for events
        val eventId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val eventId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001")

        mockEventStats[eventId1] = mapOf(
            "eventId" to eventId1.toString(),
            "totalRegistrations" to 500,
            "totalCheckedIn" to 387,
            "checkInRate" to 77.4,
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "sessions" to listOf(
                mapOf(
                    "sessionId" to "650e8400-e29b-41d4-a716-446655440001",
                    "name" to "Opening Keynote",
                    "checkedIn" to 387,
                    "capacity" to 500
                ),
                mapOf(
                    "sessionId" to "650e8400-e29b-41d4-a716-446655440002",
                    "name" to "Advanced React Patterns",
                    "checkedIn" to 45,
                    "capacity" to 50
                )
            )
        )

        mockEventStats[eventId2] = mapOf(
            "eventId" to eventId2.toString(),
            "totalRegistrations" to 50,
            "totalCheckedIn" to 42,
            "checkInRate" to 84.0,
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "sessions" to listOf(
                mapOf(
                    "sessionId" to "650e8400-e29b-41d4-a716-446655440003",
                    "name" to "Machine Learning Fundamentals",
                    "checkedIn" to 25,
                    "capacity" to 30
                )
            )
        )

        // Initialize mock session stats
        val sessionId1 = UUID.fromString("650e8400-e29b-41d4-a716-446655440001")
        val sessionId2 = UUID.fromString("650e8400-e29b-41d4-a716-446655440002")
        val sessionId3 = UUID.fromString("650e8400-e29b-41d4-a716-446655440003")

        mockSessionStats[sessionId1] = mapOf(
            "sessionId" to sessionId1.toString(),
            "sessionName" to "Opening Keynote",
            "totalRegistrations" to 500,
            "totalCheckedIn" to 387,
            "checkInRate" to 77.4,
            "capacity" to 500,
            "startTime" to "2024-03-15T09:00:00",
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )

        mockSessionStats[sessionId2] = mapOf(
            "sessionId" to sessionId2.toString(),
            "sessionName" to "Advanced React Patterns",
            "totalRegistrations" to 50,
            "totalCheckedIn" to 45,
            "checkInRate" to 90.0,
            "capacity" to 50,
            "startTime" to "2024-03-15T10:30:00",
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )

        mockSessionStats[sessionId3] = mapOf(
            "sessionId" to sessionId3.toString(),
            "sessionName" to "Machine Learning Fundamentals",
            "totalRegistrations" to 30,
            "totalCheckedIn" to 25,
            "checkInRate" to 83.3,
            "capacity" to 30,
            "startTime" to "2024-03-20T10:00:00",
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )

        // Initialize mock attendees
        mockAttendees[eventId1] = listOf(
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "firstName" to "John",
                "lastName" to "Doe",
                "email" to "john.doe@example.com",
                "registrationDate" to "2024-02-20T10:30:00",
                "checkInStatus" to "CHECKED_IN",
                "checkInTime" to "2024-03-15T08:45:00"
            ),
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "firstName" to "Jane",
                "lastName" to "Smith",
                "email" to "jane.smith@example.com",
                "registrationDate" to "2024-02-21T14:15:00",
                "checkInStatus" to "REGISTERED",
                "checkInTime" to null
            ),
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "firstName" to "Bob",
                "lastName" to "Johnson",
                "email" to "bob.johnson@example.com",
                "registrationDate" to "2024-02-19T16:20:00",
                "checkInStatus" to "CHECKED_IN",
                "checkInTime" to "2024-03-15T08:52:00"
            )
        )

        mockAttendees[eventId2] = listOf(
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "firstName" to "Alice",
                "lastName" to "Wilson",
                "email" to "alice.wilson@example.com",
                "registrationDate" to "2024-02-25T11:00:00",
                "checkInStatus" to "CHECKED_IN",
                "checkInTime" to "2024-03-20T09:30:00"
            ),
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "firstName" to "Charlie",
                "lastName" to "Brown",
                "email" to "charlie.brown@example.com",
                "registrationDate" to "2024-02-26T13:45:00",
                "checkInStatus" to "REGISTERED",
                "checkInTime" to null
            )
        )
    }

    fun getEventCheckInStats(eventId: UUID): Map<String, Any> {
        return mockEventStats[eventId] ?: mapOf(
            "eventId" to eventId.toString(),
            "totalRegistrations" to 0,
            "totalCheckedIn" to 0,
            "checkInRate" to 0.0,
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "sessions" to emptyList<Map<String, Any>>()
        )
    }

    fun getSessionCheckInStats(sessionId: UUID): Map<String, Any> {
        return mockSessionStats[sessionId] ?: mapOf(
            "sessionId" to sessionId.toString(),
            "sessionName" to "Unknown Session",
            "totalRegistrations" to 0,
            "totalCheckedIn" to 0,
            "checkInRate" to 0.0,
            "capacity" to 0,
            "startTime" to "",
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    fun checkInToEvent(eventId: UUID, checkInData: Map<String, String>): Map<String, Any> {
        val email = checkInData["email"] ?: throw IllegalArgumentException("Email is required")
        val qrCode = checkInData["qrCode"]
        
        // Mock check-in process
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        // Update stats (simulated)
        val currentStats = mockEventStats[eventId]?.toMutableMap() ?: mutableMapOf()
        val currentCheckedIn = currentStats["totalCheckedIn"] as? Int ?: 0
        val totalRegistrations = currentStats["totalRegistrations"] as? Int ?: 0
        
        currentStats["totalCheckedIn"] = currentCheckedIn + 1
        currentStats["checkInRate"] = ((currentCheckedIn + 1).toDouble() / totalRegistrations.toDouble()) * 100
        currentStats["lastUpdated"] = currentTime
        
        mockEventStats[eventId] = currentStats
        
        return mapOf(
            "success" to true,
            "message" to "Successfully checked in to event",
            "checkInTime" to currentTime,
            "attendeeInfo" to mapOf(
                "email" to email,
                "checkInMethod" to if (qrCode != null) "QR_CODE" else "MANUAL"
            )
        )
    }

    fun checkInToSession(sessionId: UUID, checkInData: Map<String, String>): Map<String, Any> {
        val email = checkInData["email"] ?: throw IllegalArgumentException("Email is required")
        val qrCode = checkInData["qrCode"]
        
        // Mock check-in process
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        // Update stats (simulated)
        val currentStats = mockSessionStats[sessionId]?.toMutableMap() ?: mutableMapOf()
        val currentCheckedIn = currentStats["totalCheckedIn"] as? Int ?: 0
        val totalRegistrations = currentStats["totalRegistrations"] as? Int ?: 0
        
        currentStats["totalCheckedIn"] = currentCheckedIn + 1
        currentStats["checkInRate"] = ((currentCheckedIn + 1).toDouble() / totalRegistrations.toDouble()) * 100
        currentStats["lastUpdated"] = currentTime
        
        mockSessionStats[sessionId] = currentStats
        
        return mapOf(
            "success" to true,
            "message" to "Successfully checked in to session",
            "checkInTime" to currentTime,
            "sessionInfo" to mapOf(
                "sessionId" to sessionId.toString(),
                "sessionName" to currentStats["sessionName"]
            ),
            "attendeeInfo" to mapOf(
                "email" to email,
                "checkInMethod" to if (qrCode != null) "QR_CODE" else "MANUAL"
            )
        )
    }

    fun getEventAttendees(eventId: UUID): List<Map<String, Any>> {
        return mockAttendees[eventId] ?: emptyList()
    }
}