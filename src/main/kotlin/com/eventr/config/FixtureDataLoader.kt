package com.eventr.config

import com.eventr.model.*
import com.eventr.repository.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
@Profile("dev")
class FixtureDataLoader(
    private val eventRepository: EventRepository,
    private val eventInstanceRepository: EventInstanceRepository,
    private val sessionRepository: SessionRepository,
    private val registrationRepository: RegistrationRepository,
    private val sessionRegistrationRepository: SessionRegistrationRepository,
    private val resourceRepository: ResourceRepository,
    private val checkInRepository: CheckInRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) : CommandLineRunner {
    
    private val logger = LoggerFactory.getLogger(FixtureDataLoader::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }
    
    override fun run(vararg args: String?) {
        logger.info("Loading fixture data for development environment...")
        
        try {
            // Skip if data already exists to avoid conflicts
            if (eventRepository.count() > 0) {
                logger.info("Fixture data already loaded, skipping...")
                displayDevelopmentLoginInfo()
                return
            }
            
            // Clear existing data
            clearExistingData()
            
            // Load fixtures in order of dependencies
            loadUsers()
            loadEvents()
            loadEventInstances()
            loadSessions()
            loadRegistrations()
            loadSessionRegistrations()
            loadResources()
            loadCheckIns()
            
            // Display development login info
            displayDevelopmentLoginInfo()
            
            logger.info("Fixture data loading completed successfully!")
        } catch (e: Exception) {
            logger.error("Error loading fixture data: ${e.message}", e)
        }
    }
    
    private fun clearExistingData() {
        logger.info("Clearing existing data...")
        checkInRepository.deleteAll()
        sessionRegistrationRepository.deleteAll()
        registrationRepository.deleteAll()
        resourceRepository.deleteAll()
        sessionRepository.deleteAll()
        eventInstanceRepository.deleteAll()
        eventRepository.deleteAll()
        userRepository.deleteAll()
        logger.info("Existing data cleared")
    }
    
    private fun loadEvents() {
        logger.info("Loading events...")
        val resource = ClassPathResource("fixtures/events.json")
        val events = objectMapper.readTree(resource.inputStream)
        
        events.forEach { eventNode ->
            val event = Event(
                id = UUID.fromString(eventNode["id"].asText()),
                name = eventNode["name"]?.asText(),
                description = eventNode["description"]?.asText(),
                status = EventStatus.valueOf(eventNode["status"].asText()),
                eventType = EventType.valueOf(eventNode["eventType"].asText()),
                category = EventCategory.valueOf(eventNode["category"].asText()),
                bannerImageUrl = eventNode["bannerImageUrl"]?.asText(),
                thumbnailImageUrl = eventNode["thumbnailImageUrl"]?.asText(),
                tags = eventNode["tags"]?.map { it.asText() }?.toMutableList(),
                capacity = eventNode["capacity"]?.asInt(),
                waitlistEnabled = eventNode["waitlistEnabled"]?.asBoolean(),
                venueName = eventNode["venueName"]?.asText(),
                address = eventNode["address"]?.asText(),
                city = eventNode["city"]?.asText(),
                state = eventNode["state"]?.asText(),
                zipCode = eventNode["zipCode"]?.asText(),
                country = eventNode["country"]?.asText(),
                virtualUrl = eventNode["virtualUrl"]?.asText(),
                accessCode = eventNode["accessCode"]?.asText(),
                requiresApproval = eventNode["requiresApproval"]?.asBoolean() ?: false,
                maxRegistrations = eventNode["maxRegistrations"]?.asInt(),
                organizerName = eventNode["organizerName"]?.asText(),
                organizerEmail = eventNode["organizerEmail"]?.asText(),
                organizerPhone = eventNode["organizerPhone"]?.asText(),
                organizerWebsite = eventNode["organizerWebsite"]?.asText(),
                startDateTime = eventNode["startDateTime"]?.asText()?.let { LocalDateTime.parse(it) },
                endDateTime = eventNode["endDateTime"]?.asText()?.let { LocalDateTime.parse(it) },
                timezone = eventNode["timezone"]?.asText(),
                agenda = eventNode["agenda"]?.asText(),
                isMultiSession = eventNode["isMultiSession"]?.asBoolean() ?: false,
                allowSessionSelection = eventNode["allowSessionSelection"]?.asBoolean() ?: false
            )
            eventRepository.save(event)
        }
        logger.info("Loaded ${events.size()} events")
    }
    
    private fun loadEventInstances() {
        logger.info("Loading event instances...")
        val resource = ClassPathResource("fixtures/event-instances.json")
        val instances = objectMapper.readTree(resource.inputStream)
        
        instances.forEach { instanceNode ->
            val eventId = UUID.fromString(instanceNode["eventId"].asText())
            val event = eventRepository.findById(eventId).orElse(null)
            
            if (event != null) {
                val instance = EventInstance(
                    id = UUID.fromString(instanceNode["id"].asText()),
                    event = event,
                    dateTime = LocalDateTime.parse(instanceNode["dateTime"].asText()),
                    location = instanceNode["location"]?.asText()
                )
                eventInstanceRepository.save(instance)
            } else {
                logger.warn("Event not found for instance: $eventId")
            }
        }
        logger.info("Loaded ${instances.size()} event instances")
    }
    
    private fun loadSessions() {
        logger.info("Loading sessions...")
        val resource = ClassPathResource("fixtures/sessions.json")
        val sessions = objectMapper.readTree(resource.inputStream)
        
        sessions.forEach { sessionNode ->
            val eventId = UUID.fromString(sessionNode["eventId"].asText())
            val event = eventRepository.findById(eventId).orElse(null)
            
            if (event != null) {
                val session = Session(
                    id = UUID.fromString(sessionNode["id"].asText()),
                    event = event,
                    title = sessionNode["title"].asText(),
                    description = sessionNode["description"]?.asText(),
                    type = SessionType.valueOf(sessionNode["type"].asText()),
                    startTime = sessionNode["startTime"]?.asText()?.let { LocalDateTime.parse(it) },
                    endTime = sessionNode["endTime"]?.asText()?.let { LocalDateTime.parse(it) },
                    location = sessionNode["location"]?.asText(),
                    room = sessionNode["room"]?.asText(),
                    building = sessionNode["building"]?.asText(),
                    capacity = sessionNode["capacity"]?.asInt(),
                    isRegistrationRequired = sessionNode["isRegistrationRequired"]?.asBoolean() ?: true,
                    isWaitlistEnabled = sessionNode["isWaitlistEnabled"]?.asBoolean() ?: true,
                    presenter = sessionNode["presenter"]?.asText(),
                    presenterTitle = sessionNode["presenterTitle"]?.asText(),
                    presenterBio = sessionNode["presenterBio"]?.asText(),
                    presenterEmail = sessionNode["presenterEmail"]?.asText(),
                    materialUrl = sessionNode["materialUrl"]?.asText(),
                    recordingUrl = sessionNode["recordingUrl"]?.asText(),
                    slidesUrl = sessionNode["slidesUrl"]?.asText(),
                    prerequisites = sessionNode["prerequisites"]?.asText(),
                    targetAudience = sessionNode["targetAudience"]?.asText(),
                    difficultyLevel = sessionNode["difficultyLevel"]?.asText(),
                    tags = sessionNode["tags"]?.map { it.asText() }?.toMutableList()
                )
                sessionRepository.save(session)
            } else {
                logger.warn("Event not found for session: $eventId")
            }
        }
        logger.info("Loaded ${sessions.size()} sessions")
    }
    
    private fun loadRegistrations() {
        logger.info("Loading registrations...")
        val resource = ClassPathResource("fixtures/registrations.json")
        val registrations = objectMapper.readTree(resource.inputStream)
        
        registrations.forEach { registrationNode ->
            val eventInstanceId = UUID.fromString(registrationNode["eventInstanceId"].asText())
            val eventInstance = eventInstanceRepository.findById(eventInstanceId).orElse(null)
            
            if (eventInstance != null) {
                val registration = Registration(
                    id = UUID.fromString(registrationNode["id"].asText()),
                    eventInstance = eventInstance,
                    userEmail = registrationNode["userEmail"]?.asText(),
                    userName = registrationNode["userName"]?.asText(),
                    status = RegistrationStatus.valueOf(registrationNode["status"].asText()),
                    checkedIn = registrationNode["checkedIn"]?.asBoolean() ?: false,
                    formData = registrationNode["formData"]?.asText()
                )
                registrationRepository.save(registration)
            } else {
                logger.warn("Event instance not found for registration: $eventInstanceId")
            }
        }
        logger.info("Loaded ${registrations.size()} registrations")
    }
    
    private fun loadSessionRegistrations() {
        logger.info("Loading session registrations...")
        val resource = ClassPathResource("fixtures/session-registrations.json")
        val sessionRegistrations = objectMapper.readTree(resource.inputStream)
        
        sessionRegistrations.forEach { sessionRegNode ->
            val sessionId = UUID.fromString(sessionRegNode["sessionId"].asText())
            val registrationId = UUID.fromString(sessionRegNode["registrationId"].asText())
            val session = sessionRepository.findById(sessionId).orElse(null)
            val registration = registrationRepository.findById(registrationId).orElse(null)
            
            if (session != null && registration != null) {
                val sessionRegistration = SessionRegistration(
                    id = UUID.fromString(sessionRegNode["id"].asText()),
                    session = session,
                    registration = registration,
                    status = SessionRegistrationStatus.valueOf(sessionRegNode["status"].asText()),
                    registeredAt = LocalDateTime.parse(sessionRegNode["registeredAt"].asText()),
                    checkedInAt = sessionRegNode["checkedInAt"]?.asText()?.let { LocalDateTime.parse(it) },
                    cancelledAt = sessionRegNode["cancelledAt"]?.asText()?.let { LocalDateTime.parse(it) },
                    waitlistPosition = sessionRegNode["waitlistPosition"]?.asInt(),
                    waitlistRegisteredAt = sessionRegNode["waitlistRegisteredAt"]?.asText()?.let { LocalDateTime.parse(it) },
                    notes = sessionRegNode["notes"]?.asText(),
                    rating = sessionRegNode["rating"]?.asInt(),
                    feedback = sessionRegNode["feedback"]?.asText(),
                    attendanceVerified = sessionRegNode["attendanceVerified"]?.asBoolean() ?: false,
                    verificationMethod = sessionRegNode["verificationMethod"]?.asText()
                )
                sessionRegistrationRepository.save(sessionRegistration)
            } else {
                if (session == null) logger.warn("Session not found: $sessionId")
                if (registration == null) logger.warn("Registration not found: $registrationId")
            }
        }
        logger.info("Loaded ${sessionRegistrations.size()} session registrations")
    }
    
    private fun loadResources() {
        logger.info("Loading resources...")
        val resource = ClassPathResource("fixtures/resources.json")
        val resources = objectMapper.readTree(resource.inputStream)
        
        resources.forEach { resourceNode ->
            val resourceEntity = Resource(
                id = UUID.fromString(resourceNode["id"].asText()),
                name = resourceNode["name"].asText(),
                description = resourceNode["description"]?.asText(),
                type = ResourceType.valueOf(resourceNode["type"].asText()),
                status = ResourceStatus.valueOf(resourceNode["status"].asText()),
                capacity = resourceNode["capacity"]?.asInt(),
                location = resourceNode["location"]?.asText(),
                floor = resourceNode["floor"]?.asText(),
                building = resourceNode["building"]?.asText(),
                specifications = resourceNode["specifications"]?.asText(),
                serialNumber = resourceNode["serialNumber"]?.asText(),
                model = resourceNode["model"]?.asText(),
                manufacturer = resourceNode["manufacturer"]?.asText(),
                isBookable = resourceNode["isBookable"]?.asBoolean() ?: true,
                requiresApproval = resourceNode["requiresApproval"]?.asBoolean() ?: false,
                bookingLeadTimeHours = resourceNode["bookingLeadTimeHours"]?.asInt() ?: 0,
                maxBookingDurationHours = resourceNode["maxBookingDurationHours"]?.asInt(),
                hourlyRate = resourceNode["hourlyRate"]?.asText()?.let { BigDecimal(it) },
                dailyRate = resourceNode["dailyRate"]?.asText()?.let { BigDecimal(it) },
                setupCost = resourceNode["setupCost"]?.asText()?.let { BigDecimal(it) },
                cleanupCost = resourceNode["cleanupCost"]?.asText()?.let { BigDecimal(it) },
                lastMaintenanceDate = resourceNode["lastMaintenanceDate"]?.asText()?.let { LocalDateTime.parse(it) },
                nextMaintenanceDate = resourceNode["nextMaintenanceDate"]?.asText()?.let { LocalDateTime.parse(it) },
                maintenanceNotes = resourceNode["maintenanceNotes"]?.asText(),
                contactPerson = resourceNode["contactPerson"]?.asText(),
                contactEmail = resourceNode["contactEmail"]?.asText(),
                contactPhone = resourceNode["contactPhone"]?.asText(),
                departmentOwner = resourceNode["departmentOwner"]?.asText(),
                totalUsageHours = resourceNode["totalUsageHours"]?.asInt() ?: 0,
                usageThisMonth = resourceNode["usageThisMonth"]?.asInt() ?: 0,
                lastUsedAt = resourceNode["lastUsedAt"]?.asText()?.let { LocalDateTime.parse(it) },
                tags = resourceNode["tags"]?.asText(),
                category = resourceNode["category"]?.asText(),
                isActive = resourceNode["isActive"]?.asBoolean() ?: true
            )
            resourceRepository.save(resourceEntity)
        }
        logger.info("Loaded ${resources.size()} resources")
    }
    
    private fun loadCheckIns() {
        logger.info("Loading check-ins...")
        val resource = ClassPathResource("fixtures/check-ins.json")
        val checkIns = objectMapper.readTree(resource.inputStream)
        
        checkIns.forEach { checkInNode ->
            val registrationId = UUID.fromString(checkInNode["registrationId"].asText())
            val registration = registrationRepository.findById(registrationId).orElse(null)
            
            if (registration != null) {
                val sessionId = checkInNode["sessionId"]?.asText()?.let { UUID.fromString(it) }
                val session = sessionId?.let { sessionRepository.findById(it).orElse(null) }
                
                val checkIn = CheckIn(
                    id = UUID.fromString(checkInNode["id"].asText()),
                    registration = registration,
                    session = session,
                    type = CheckInType.valueOf(checkInNode["type"].asText()),
                    method = CheckInMethod.valueOf(checkInNode["method"].asText()),
                    checkedInAt = LocalDateTime.parse(checkInNode["checkedInAt"].asText()),
                    checkedInBy = checkInNode["checkedInBy"]?.asText(),
                    deviceId = checkInNode["deviceId"]?.asText(),
                    deviceName = checkInNode["deviceName"]?.asText(),
                    ipAddress = checkInNode["ipAddress"]?.asText(),
                    userAgent = checkInNode["userAgent"]?.asText(),
                    location = checkInNode["location"]?.asText(),
                    verificationCode = checkInNode["verificationCode"]?.asText(),
                    qrCodeUsed = checkInNode["qrCodeUsed"]?.asText(),
                    isVerified = checkInNode["isVerified"]?.asBoolean() ?: true,
                    notes = checkInNode["notes"]?.asText(),
                    metadata = checkInNode["metadata"]?.asText(),
                    isSynced = checkInNode["isSynced"]?.asBoolean() ?: true,
                    syncedAt = checkInNode["syncedAt"]?.asText()?.let { LocalDateTime.parse(it) }
                )
                checkInRepository.save(checkIn)
            } else {
                logger.warn("Registration not found for check-in: $registrationId")
            }
        }
        logger.info("Loaded ${checkIns.size()} check-ins")
    }
    
    private fun loadUsers() {
        logger.info("Loading users...")
        val resource = ClassPathResource("fixtures/users.json")
        val users = objectMapper.readTree(resource.inputStream)
        
        users.forEach { userNode ->
            val userId = UUID.fromString(userNode["id"].asText())
            val email = userNode["email"].asText()
            
            // Check if user already exists by email
            val existingUser = userRepository.findByEmail(email)
            
            if (existingUser == null) {
                // Create new user without setting ID (let JPA generate it)
                val user = User().apply {
                    this.email = email
                    firstName = userNode["firstName"].asText()
                    lastName = userNode["lastName"].asText()
                    passwordHash = passwordEncoder.encode(userNode["password"].asText())
                    phone = userNode["phone"]?.asText()
                    company = userNode["company"]?.asText()
                    jobTitle = userNode["jobTitle"]?.asText()
                    bio = userNode["bio"]?.asText()
                    profileImageUrl = userNode["profileImageUrl"]?.asText()
                    role = UserRole.valueOf(userNode["role"].asText())
                    status = UserStatus.valueOf(userNode["status"].asText())
                    emailVerified = userNode["emailVerified"]?.asBoolean() ?: true
                    timezone = userNode["timezone"]?.asText() ?: "UTC"
                    language = userNode["language"]?.asText() ?: "en"
                    marketingEmails = userNode["marketingEmails"]?.asBoolean() ?: true
                    eventReminders = userNode["eventReminders"]?.asBoolean() ?: true
                    weeklyDigest = userNode["weeklyDigest"]?.asBoolean() ?: true
                    createdAt = userNode["createdAt"]?.asText()?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
                    lastLoginAt = userNode["lastLoginAt"]?.asText()?.let { LocalDateTime.parse(it) }
                }
                userRepository.save(user)
                logger.debug("Created new user: ${email}")
            } else {
                logger.debug("User already exists: ${email}")
            }
        }
        logger.info("Loaded ${users.size()} users")
    }
    
    private fun displayDevelopmentLoginInfo() {
        logger.info("╔═══════════════════════════════════════════════════════════════════════════════╗")
        logger.info("║                        🚀 DEVELOPMENT LOGIN INFO 🚀                          ║")
        logger.info("╠═══════════════════════════════════════════════════════════════════════════════╣")
        logger.info("║                                                                               ║")
        logger.info("║  👤 TEST USERS AVAILABLE FOR LOGIN:                                          ║")
        logger.info("║                                                                               ║")
        logger.info("║  📧 Admin User:                                                              ║")
        logger.info("║     Email:    admin@eventr.dev                                               ║")
        logger.info("║     Password: DevPassword123                                                 ║")
        logger.info("║     Role:     ADMIN                                                          ║")
        logger.info("║                                                                               ║")
        logger.info("║  📧 Organizer User:                                                          ║")
        logger.info("║     Email:    organizer@eventr.dev                                           ║")
        logger.info("║     Password: DevPassword123                                                 ║")
        logger.info("║     Role:     ORGANIZER                                                      ║")
        logger.info("║                                                                               ║")
        logger.info("║  📧 Regular User:                                                            ║")
        logger.info("║     Email:    user@eventr.dev                                                ║")
        logger.info("║     Password: DevPassword123                                                 ║")
        logger.info("║     Role:     ATTENDEE                                                       ║")
        logger.info("║                                                                               ║")
        logger.info("║  🌐 Login URL: http://localhost:3002/login                                   ║")
        logger.info("║                                                                               ║")
        logger.info("║  ⚠️  These credentials are for DEVELOPMENT ONLY!                            ║")
        logger.info("║     Do not use in production environments.                                   ║")
        logger.info("║                                                                               ║")
        logger.info("╚═══════════════════════════════════════════════════════════════════════════════╝")
    }
}