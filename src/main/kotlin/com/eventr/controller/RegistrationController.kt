package com.eventr.controller

import com.eventr.dto.RegistrationCreateDto
import com.eventr.dto.RegistrationDto
import com.eventr.model.Registration
import com.eventr.model.RegistrationStatus
import com.eventr.repository.EventInstanceRepository
import com.eventr.repository.RegistrationRepository
import com.eventr.repository.UserRepository
import com.eventr.service.EmailService
import com.eventr.service.WebSocketEventService
import org.springframework.beans.BeanUtils
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/registrations")
class RegistrationController(
    private val registrationRepository: RegistrationRepository,
    private val eventInstanceRepository: EventInstanceRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val webSocketEventService: WebSocketEventService
) {

    @PostMapping
    fun createRegistration(@RequestBody registrationCreateDto: RegistrationCreateDto): RegistrationDto {
        val eventInstance = registrationCreateDto.eventInstanceId?.let { 
            eventInstanceRepository.findById(it).orElseThrow() 
        } ?: throw IllegalArgumentException("Event instance ID is required")
        
        val registration = Registration().apply {
            this.eventInstance = eventInstance
            
            // Handle user authentication approach
            if (registrationCreateDto.userId != null) {
                user = userRepository.findById(registrationCreateDto.userId!!).orElseThrow {
                    IllegalArgumentException("User not found")
                }
                userEmail = user?.email
                userName = "${user?.firstName} ${user?.lastName}"
            } else {
                // Backward compatibility with email/name approach
                userEmail = registrationCreateDto.userEmail
                userName = registrationCreateDto.userName
            }
            
            formData = registrationCreateDto.formData
            status = RegistrationStatus.REGISTERED
        }
        
        val savedRegistration = registrationRepository.save(registration)
        
        try {
            emailService.sendRegistrationConfirmation(savedRegistration)
        } catch (e: Exception) {
            // Log the exception, but don't block the registration process
            System.err.println("Failed to send confirmation email: ${e.message}")
        }
        
        // Broadcast real-time registration update
        eventInstance.event?.id?.let { eventId ->
            webSocketEventService.broadcastRegistrationUpdate(
                eventId, 
                "NEW", 
                mapOf(
                    "registrationId" to savedRegistration.id.toString(),
                    "userName" to (savedRegistration.userName ?: "Anonymous"),
                    "userEmail" to (savedRegistration.userEmail ?: "")
                )
            )
        }
        
        return RegistrationDto().apply {
            BeanUtils.copyProperties(savedRegistration, this)
            eventInstanceId = savedRegistration.eventInstance?.id
        }
    }
    
    @GetMapping("/user/{email}")
    fun getRegistrationsByUserEmail(@PathVariable email: String): List<RegistrationDto> {
        return registrationRepository.findByUserEmail(email).map { registration ->
            RegistrationDto().apply {
                BeanUtils.copyProperties(registration, this)
                eventInstanceId = registration.eventInstance?.id
            }
        }
    }
    
    @GetMapping("/user/id/{userId}")
    fun getRegistrationsByUserId(@PathVariable userId: UUID): List<RegistrationDto> {
        return registrationRepository.findByUserId(userId).map { registration ->
            RegistrationDto().apply {
                BeanUtils.copyProperties(registration, this)
                eventInstanceId = registration.eventInstance?.id
            }
        }
    }
    
    @PutMapping("/{registrationId}/cancel")
    fun cancelRegistration(
        @PathVariable registrationId: UUID,
        @RequestParam(required = false) reason: String = ""
    ): RegistrationDto {
        val registration = registrationRepository.findById(registrationId).orElseThrow()
        registration.status = RegistrationStatus.CANCELLED
        val cancelledRegistration = registrationRepository.save(registration)
        
        // Send cancellation email notification
        try {
            emailService.sendCancellationNotification(cancelledRegistration, reason)
        } catch (e: Exception) {
            // Log the exception, but don't block the cancellation process
            System.err.println("Failed to send cancellation email: ${e.message}")
        }
        
        // Broadcast real-time cancellation update
        cancelledRegistration.eventInstance?.event?.id?.let { eventId ->
            webSocketEventService.broadcastRegistrationUpdate(
                eventId, 
                "CANCELLED", 
                mapOf(
                    "registrationId" to cancelledRegistration.id.toString(),
                    "userName" to (cancelledRegistration.userName ?: "Anonymous"),
                    "reason" to reason
                )
            )
        }
        
        return RegistrationDto().apply {
            BeanUtils.copyProperties(cancelledRegistration, this)
            eventInstanceId = cancelledRegistration.eventInstance?.id
        }
    }
}
