package com.eventr.controller

import com.eventr.dto.RegistrationCreateDto
import com.eventr.dto.RegistrationDto
import com.eventr.model.Registration
import com.eventr.model.RegistrationStatus
import com.eventr.repository.EventInstanceRepository
import com.eventr.repository.RegistrationRepository
import com.eventr.service.EmailService
import org.springframework.beans.BeanUtils
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/registrations")
class RegistrationController(
    private val registrationRepository: RegistrationRepository,
    private val eventInstanceRepository: EventInstanceRepository,
    private val emailService: EmailService
) {

    @PostMapping
    fun createRegistration(@RequestBody registrationCreateDto: RegistrationCreateDto): RegistrationDto {
        val eventInstance = registrationCreateDto.eventInstanceId?.let { 
            eventInstanceRepository.findById(it).orElseThrow() 
        } ?: throw IllegalArgumentException("Event instance ID is required")
        
        val registration = Registration().apply {
            this.eventInstance = eventInstance
            userEmail = registrationCreateDto.userEmail
            userName = registrationCreateDto.userName
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
    
    @PutMapping("/{registrationId}/cancel")
    fun cancelRegistration(@PathVariable registrationId: UUID): RegistrationDto {
        val registration = registrationRepository.findById(registrationId).orElseThrow()
        registration.status = RegistrationStatus.CANCELLED
        val cancelledRegistration = registrationRepository.save(registration)
        
        return RegistrationDto().apply {
            BeanUtils.copyProperties(cancelledRegistration, this)
            eventInstanceId = cancelledRegistration.eventInstance?.id
        }
    }
}
