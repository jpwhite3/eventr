package com.eventr.testutil

import com.eventr.model.*
import com.eventr.testutil.builders.*
import java.time.LocalDateTime

/**
 * Development seed data generator for creating realistic demo scenarios.
 * These scenarios provide comprehensive, interconnected data for development,
 * demos, and manual testing of the entire system.
 * 
 * Unlike test fixtures which focus on isolated testing scenarios,
 * development seed data creates complete, realistic business scenarios
 * that demonstrate the full capabilities of the EventR system.
 * 
 * Usage:
 * ```kotlin
 * // Create a complete conference environment
 * val conference = DevelopmentSeedData.createTechConferenceDemo()
 * 
 * // Create workshop series
 * val workshops = DevelopmentSeedData.createWorkshopSeries()
 * 
 * // Create corporate meeting scenarios
 * val meetings = DevelopmentSeedData.createCorporateMeetingSuite()
 * ```
 */
object DevelopmentSeedData {
    
    // ================================================================================
    // Conference Demo Scenarios
    // ================================================================================
    
    /**
     * Creates a comprehensive technology conference with realistic sessions,
     * speakers, registrations, and resource bookings.
     */
    fun createTechConferenceDemo(): ConferenceDemo {
        val conferenceStart = LocalDateTime.now().plusDays(30)
        
        // Main conference event
        val event = TestDataBuilders.event()
            .withName("DevTech Conference 2024")
            .withDescription("""
                Join industry leaders and innovators for two days of cutting-edge technology presentations,
                hands-on workshops, and networking opportunities. Topics include AI/ML, Cloud Computing,
                DevOps, Cybersecurity, and Web Development.
                
                Featured speakers include CTOs from major tech companies, open source maintainers,
                and thought leaders driving the future of technology.
                
                Conference includes:
                • 12 technical sessions across 3 tracks
                • 4 hands-on workshops
                • Opening and closing keynotes
                • Networking lunch and evening reception
                • Expo hall with 20+ vendor booths
            """.trimIndent())
            .asConference()
            .asPublished()
            .withCategory(EventCategory.TECHNOLOGY)
            .withCapacity(500)
            .withLocation("Tech Conference Center", "San Francisco", "CA")
            .withDuration(conferenceStart, 12) // 2-day event
            .withTags("technology", "conference", "networking", "professional-development")
            .build()
        
        // Conference sessions with realistic schedule
        val sessions = createConferenceSessions(event, conferenceStart)
        
        // Diverse registrations representing real conference attendance
        val registrations = createConferenceRegistrations(300)
        
        // Resource bookings for rooms, equipment, catering
        val resources = createConferenceResources()
        val resourceBookings = createConferenceResourceBookings(resources, sessions)
        
        return ConferenceDemo(
            event = event,
            sessions = sessions,
            registrations = registrations,
            resources = resources,
            resourceBookings = resourceBookings
        )
    }
    
    /**
     * Creates a startup pitch competition event.
     */
    fun createStartupPitchDemo(): EventDemo {
        val pitchStart = LocalDateTime.now().plusDays(45)
        
        val event = TestDataBuilders.event()
            .withName("Silicon Valley Startup Pitch Competition")
            .withDescription("""
                Early-stage startups present their innovations to a panel of venture capitalists
                and angel investors. Competition features 20 startups across multiple categories
                including FinTech, HealthTech, EdTech, and GreenTech.
                
                Event includes:
                • 20 startup pitches (5 minutes each)
                • Expert panel discussions
                • Investor networking session
                • Awards ceremony
                
                Prize categories:
                • Best Innovation: $50,000
                • People's Choice: $25,000
                • Social Impact: $15,000
            """.trimIndent())
            .asMeetup()
            .asPublished()
            .withCategory(EventCategory.BUSINESS)
            .withCapacity(200)
            .withLocation("Innovation Hub", "Palo Alto", "CA")
            .withDuration(pitchStart, 6)
            .withTags("startup", "pitching", "venture-capital", "innovation")
            .build()
        
        val sessions = listOf(
            // Competition rounds
            TestDataBuilders.session()
                .withEvent(event)
                .withTitle("FinTech Pitch Round")
                .withType(SessionType.PRESENTATION)
                .withDuration(pitchStart.plusHours(1), 90)
                .withCapacity(200)
                .build(),
            
            TestDataBuilders.session()
                .withEvent(event)
                .withTitle("HealthTech & BioTech Pitches")
                .withType(SessionType.PRESENTATION)
                .withDuration(pitchStart.plusHours(3), 90)
                .withCapacity(200)
                .build(),
            
            TestDataBuilders.session()
                .withEvent(event)
                .withTitle("Investor Panel: Future of Tech")
                .withType(SessionType.PANEL)
                .withDuration(pitchStart.plusHours(4).plusMinutes(45), 60)
                .withCapacity(200)
                .withPresenter("Panel of VCs", "Leading venture capitalists share insights")
                .build()
        )
        
        val registrations = createStartupPitchRegistrations(150)
        
        return EventDemo(
            event = event,
            sessions = sessions,
            registrations = registrations
        )
    }
    
    // ================================================================================
    // Workshop Series Scenarios
    // ================================================================================
    
    /**
     * Creates a series of professional development workshops.
     */
    fun createWorkshopSeries(): List<EventDemo> {
        val workshopStart = LocalDateTime.now().plusDays(14)
        
        return listOf(
            createSingleWorkshop(
                name = "Advanced Kotlin Development",
                description = "Deep dive into Kotlin coroutines, flow, and advanced language features",
                startTime = workshopStart,
                prerequisites = "2+ years Kotlin experience, laptop with IntelliJ IDEA",
                materials = "Sample code repositories, presentation slides, exercises"
            ),
            
            createSingleWorkshop(
                name = "Cloud Architecture Patterns",
                description = "Hands-on workshop covering microservices, serverless, and container orchestration",
                startTime = workshopStart.plusDays(7),
                prerequisites = "Basic cloud platform knowledge, Docker experience",
                materials = "AWS sandbox environment, deployment scripts, architecture diagrams"
            ),
            
            createSingleWorkshop(
                name = "Modern Frontend Development",
                description = "Build modern web applications with React, TypeScript, and advanced tooling",
                startTime = workshopStart.plusDays(14),
                prerequisites = "JavaScript experience, Node.js installed",
                materials = "Starter project templates, component libraries, testing frameworks"
            )
        )
    }
    
    /**
     * Creates a corporate training workshop event.
     */
    fun createCorporateTrainingDemo(): EventDemo {
        val trainingStart = LocalDateTime.now().plusDays(21)
        
        val event = TestDataBuilders.event()
            .withName("Enterprise Security Best Practices")
            .withDescription("""
                Comprehensive security training for development teams and IT professionals.
                Learn to identify vulnerabilities, implement security controls, and establish
                secure development lifecycle practices.
                
                Training covers:
                • OWASP Top 10 vulnerabilities
                • Secure coding practices
                • Infrastructure security
                • Incident response procedures
                • Compliance requirements (SOC2, GDPR, HIPAA)
                
                Includes hands-on labs with real-world scenarios.
            """.trimIndent())
            .asWorkshop()
            .asPublished()
            .withCategory(EventCategory.EDUCATION)
            .withCapacity(40)
            .withLocation("Corporate Training Center", "Austin", "TX")
            .withDuration(trainingStart, 8) // Full day
            .withTags("security", "training", "enterprise", "compliance")
            .build()
        
        val sessions = listOf(
            TestDataBuilders.session()
                .withEvent(event)
                .asWorkshop()
                .withTitle("Secure Development Fundamentals")
                .withDuration(trainingStart.plusHours(1), 180)
                .withCapacity(40)
                .withPresenter("Sarah Security", "CISSP certified security architect with 15 years experience")
                .withPrerequisites("Basic programming knowledge, security clearance")
                .withMaterials("Security testing tools, code examples, compliance checklists")
                .build(),
            
            TestDataBuilders.session()
                .withEvent(event)
                .asWorkshop()
                .withTitle("Hands-on Penetration Testing")
                .withDuration(trainingStart.plusHours(5), 150)
                .withCapacity(40)
                .withPresenter("Mike Hacker", "Ethical hacker and security consultant")
                .withPrerequisites("Laptop with Kali Linux VM")
                .withMaterials("Testing lab environment, penetration testing tools")
                .build()
        )
        
        val registrations = createCorporateTrainingRegistrations(35)
        
        return EventDemo(
            event = event,
            sessions = sessions,
            registrations = registrations
        )
    }
    
    // ================================================================================
    // Virtual Event Scenarios
    // ================================================================================
    
    /**
     * Creates a global virtual summit with international attendees.
     */
    fun createGlobalVirtualSummit(): EventDemo {
        val summitStart = LocalDateTime.now().plusDays(60)
        
        val event = TestDataBuilders.event()
            .withName("Global Innovation Summit 2024")
            .withDescription("""
                Connect with innovators, entrepreneurs, and thought leaders from around the world
                in this virtual summit exploring the future of technology and business.
                
                Summit spans multiple time zones with sessions designed for global participation:
                • APAC-friendly sessions (early UTC)
                • European prime time sessions
                • Americas-focused sessions (late UTC)
                
                Features:
                • 15+ expert presentations
                • Interactive Q&A sessions
                • Virtual networking rooms
                • Digital expo hall
                • Recorded sessions for on-demand viewing
                
                Languages: English (primary), simultaneous interpretation available
            """.trimIndent())
            .asMeetup()
            .asVirtual("https://summit.globalinnovation.com", "+1-800-555-0199", "SUMMIT2024")
            .asPublished()
            .withCategory(EventCategory.BUSINESS)
            .withCapacity(2000)
            .withDuration(summitStart, 16) // Multiple time zones
            .withTags("virtual", "global", "innovation", "business", "networking")
            .build()
        
        val sessions = createGlobalSummitSessions(event, summitStart)
        val registrations = createGlobalRegistrations(1200)
        
        return EventDemo(
            event = event,
            sessions = sessions,
            registrations = registrations
        )
    }
    
    // ================================================================================
    // Community Event Scenarios
    // ================================================================================
    
    /**
     * Creates a local tech meetup series.
     */
    fun createLocalTechMeetups(): List<EventDemo> {
        val meetupStart = LocalDateTime.now().plusDays(7)
        
        return listOf(
            createTechMeetup(
                name = "React Native Developers Meetup",
                date = meetupStart,
                topic = "Cross-platform mobile development with React Native and Expo",
                sponsor = "Mobile Dev Co"
            ),
            
            createTechMeetup(
                name = "DevOps Engineers Meetup",
                date = meetupStart.plusWeeks(2),
                topic = "Kubernetes deployment strategies and GitOps workflows",
                sponsor = "Cloud Solutions Inc"
            ),
            
            createTechMeetup(
                name = "AI/ML Practitioners Group",
                date = meetupStart.plusWeeks(4),
                topic = "Practical machine learning model deployment and monitoring",
                sponsor = "DataTech Analytics"
            )
        )
    }
    
    /**
     * Creates a community hackathon event.
     */
    fun createHackathonDemo(): EventDemo {
        val hackathonStart = LocalDateTime.now().plusDays(28)
        
        val event = TestDataBuilders.event()
            .withName("Code for Good Hackathon")
            .withDescription("""
                48-hour hackathon focused on creating technology solutions for social impact.
                Teams work on projects addressing climate change, education, healthcare,
                and social justice challenges.
                
                Event includes:
                • Team formation session
                • Mentor office hours
                • Technical workshops
                • Final presentations and judging
                • Awards ceremony
                
                Prizes:
                • Best Overall Solution: $10,000 to winning charity
                • Best Technical Implementation: $5,000
                • Best Social Impact: $5,000
                • People's Choice: $2,500
                
                All projects must be open source and benefit nonprofit organizations.
            """.trimIndent())
            .asMeetup()
            .asPublished()
            .withCategory(EventCategory.TECHNOLOGY)
            .withCapacity(150)
            .withLocation("Innovation Labs", "Seattle", "WA")
            .withDuration(hackathonStart, 48) // Weekend hackathon
            .withTags("hackathon", "social-impact", "coding", "competition")
            .build()
        
        val sessions = createHackathonSessions(event, hackathonStart)
        val registrations = createHackathonRegistrations(120)
        
        return EventDemo(
            event = event,
            sessions = sessions,
            registrations = registrations
        )
    }
    
    // ================================================================================
    // Support Functions for Creating Complex Scenarios
    // ================================================================================
    
    private fun createConferenceSessions(event: Event, startTime: LocalDateTime): List<Session> {
        return listOf(
            // Day 1 - Opening
            TestDataBuilders.session()
                .withEvent(event)
                .asKeynote()
                .withTitle("Opening Keynote: The Future of Distributed Systems")
                .withDuration(startTime.plusHours(1), 60)
                .withPresenter("Dr. Jane Chen", "Distinguished Engineer at CloudTech, PhD Computer Science Stanford")
                .withCapacity(500)
                .build(),
            
            // Track A - AI/ML
            TestDataBuilders.session()
                .withEvent(event)
                .asPresentation()
                .withTitle("Production ML Pipelines: From Jupyter to Production")
                .withDuration(startTime.plusHours(2).plusMinutes(30), 45)
                .withPresenter("Alex Rodriguez", "Senior ML Engineer, DataCorp")
                .withCapacity(150)
                .withMaterials("Code samples, deployment templates, monitoring dashboards")
                .build(),
            
            TestDataBuilders.session()
                .withEvent(event)
                .asWorkshop()
                .withTitle("Hands-on: Building LLM Applications")
                .withDuration(startTime.plusHours(3).plusMinutes(30), 120)
                .withPresenter("Dr. Maria Santos", "AI Research Lead, TechStartup Inc")
                .withCapacity(40)
                .withPrerequisites("Python experience, laptop with 16GB+ RAM")
                .withMaterials("Development environment setup guide, API keys, sample datasets")
                .build(),
            
            // Track B - Cloud & DevOps
            TestDataBuilders.session()
                .withEvent(event)
                .asPresentation()
                .withTitle("Kubernetes at Scale: Lessons from Production")
                .withDuration(startTime.plusHours(2).plusMinutes(30), 45)
                .withPresenter("Mike Thompson", "Platform Engineering Manager, MegaCorp")
                .withCapacity(150)
                .build(),
            
            TestDataBuilders.session()
                .withEvent(event)
                .asWorkshop()
                .withTitle("GitOps Workshop: Automated Deployments")
                .withDuration(startTime.plusHours(3).plusMinutes(30), 120)
                .withPresenter("Sarah DevOps", "Staff SRE, CloudNative Co")
                .withCapacity(50)
                .withPrerequisites("Git knowledge, Docker basics, cloud account")
                .build(),
            
            // Day 2 - Closing
            TestDataBuilders.session()
                .withEvent(event)
                .asKeynote()
                .withTitle("Closing Keynote: Building Sustainable Tech Organizations")
                .withDuration(startTime.plusDays(1).plusHours(4), 60)
                .withPresenter("Robert Leader", "CTO, GrowthTech, Author of 'Scaling Engineering'")
                .withCapacity(500)
                .build()
        )
    }
    
    private fun createConferenceRegistrations(count: Int): List<Registration> {
        return (1..count).map { index ->
            val type = when (index % 10) {
                0 -> TestDataBuilders.registration().asVIPAttendee().withUserDetails("Executive $index", "exec$index@company.com")
                1 -> TestDataBuilders.registration().asSpeaker().withUserDetails("Speaker $index", "speaker$index@tech.com")
                2 -> TestDataBuilders.registration().asVolunteer().withUserDetails("Volunteer $index", "volunteer$index@event.com")
                3,4 -> TestDataBuilders.registration().asEarlyBird().withUserDetails("EarlyBird $index", "early$index@dev.com")
                else -> TestDataBuilders.registration().asConferenceAttendee().withUserDetails("Developer $index", "dev$index@company.com")
            }
            type.build()
        }
    }
    
    private fun createConferenceResources(): List<Resource> {
        return listOf(
            TestDataBuilders.resource().asConferenceRoom().withName("Main Auditorium").withCapacity(500).build(),
            TestDataBuilders.resource().asRoom().withName("Workshop Room A").withCapacity(40).build(),
            TestDataBuilders.resource().asRoom().withName("Workshop Room B").withCapacity(50).build(),
            TestDataBuilders.resource().asRoom().withName("Breakout Room 1").withCapacity(25).build(),
            TestDataBuilders.resource().asEquipment().withName("4K Projector System").build(),
            TestDataBuilders.resource().asTechnology().withName("Live Stream Setup").build(),
            TestDataBuilders.resource().asCatering().withName("Conference Lunch Service").withCapacity(500).build(),
            TestDataBuilders.resource().asStaff().withName("AV Technical Support").build()
        )
    }
    
    private fun createConferenceResourceBookings(resources: List<Resource>, sessions: List<Session>): List<SessionResource> {
        return sessions.flatMap { session ->
            // Each session needs a room and basic AV
            listOf(
                SessionResource().apply {
                    this.session = session
                    this.resource = resources.find { it.type == ResourceType.ROOM }
                    this.quantityNeeded = 1
                    this.quantityAllocated = 1
                    this.status = ResourceBookingStatus.ALLOCATED
                    this.bookingStart = session.startTime
                    this.bookingEnd = session.endTime
                },
                SessionResource().apply {
                    this.session = session
                    this.resource = resources.find { it.type == ResourceType.EQUIPMENT }
                    this.quantityNeeded = 1
                    this.quantityAllocated = 1
                    this.status = ResourceBookingStatus.ALLOCATED
                    this.bookingStart = session.startTime
                    this.bookingEnd = session.endTime
                }
            )
        }
    }
    
    // Additional helper methods would be implemented here for other demo scenarios...
    
    private fun createSingleWorkshop(
        name: String, 
        description: String, 
        startTime: LocalDateTime,
        prerequisites: String,
        materials: String
    ): EventDemo {
        val event = TestDataBuilders.event()
            .asWorkshop()
            .withName(name)
            .withDescription(description)
            .withCapacity(25)
            .withDuration(startTime, 6)
            .asPublished()
            .build()
        
        val session = TestDataBuilders.session()
            .withEvent(event)
            .asWorkshop()
            .withTitle(name)
            .withDuration(startTime.plusMinutes(30), 300) // 5 hours of workshop
            .withCapacity(25)
            .withPrerequisites(prerequisites)
            .withMaterials(materials)
            .build()
        
        val registrations = (1..20).map { index ->
            TestDataBuilders.registration()
                .asWorkshopParticipant()
                .withUserDetails("Participant $index", "participant$index@workshop.com")
                .build()
        }
        
        return EventDemo(event, listOf(session), registrations)
    }
    
    private fun createTechMeetup(name: String, date: LocalDateTime, topic: String, sponsor: String): EventDemo {
        val event = TestDataBuilders.event()
            .asMeetup()
            .withName(name)
            .withDescription("$topic\n\nSponsored by: $sponsor\n\nNetworking and pizza provided!")
            .withCapacity(60)
            .withLocation("Tech Hub", "San Francisco", "CA")
            .withDuration(date, 3)
            .asPublished()
            .withTags("meetup", "networking", "tech")
            .build()
        
        val sessions = listOf(
            TestDataBuilders.session()
                .withEvent(event)
                .asPresentation()
                .withTitle(topic)
                .withDuration(date.plusMinutes(30), 45)
                .withCapacity(60)
                .build()
        )
        
        val registrations = (1..45).map { index ->
            TestDataBuilders.registration()
                .asRegistered()
                .withUserDetails("Meetup Member $index", "member$index@meetup.com")
                .build()
        }
        
        return EventDemo(event, sessions, registrations)
    }
    
    // Placeholder implementations for other helper methods...
    private fun createStartupPitchRegistrations(count: Int): List<Registration> = 
        TestFixtures.generateRegistrations(count)
    
    private fun createGlobalSummitSessions(event: Event, startTime: LocalDateTime): List<Session> = 
        TestDataBuilders.multipleSessions(8).map { it.withEvent(event).build() }
    
    private fun createGlobalRegistrations(count: Int): List<Registration> = 
        TestFixtures.generateRegistrations(count)
        
    private fun createCorporateTrainingRegistrations(count: Int): List<Registration> =
        TestFixtures.generateRegistrations(count)
        
    private fun createHackathonSessions(event: Event, startTime: LocalDateTime): List<Session> =
        TestDataBuilders.multipleSessions(6).map { it.withEvent(event).build() }
        
    private fun createHackathonRegistrations(count: Int): List<Registration> =
        TestFixtures.generateRegistrations(count)
}

// ================================================================================
// Data Classes for Demo Scenarios
// ================================================================================

/**
 * Complete conference demo with all associated data.
 */
data class ConferenceDemo(
    val event: Event,
    val sessions: List<Session>,
    val registrations: List<Registration>,
    val resources: List<Resource>,
    val resourceBookings: List<SessionResource>
)

/**
 * General event demo scenario.
 */
data class EventDemo(
    val event: Event,
    val sessions: List<Session>,
    val registrations: List<Registration>
)