package com.eventr.controller

import com.eventr.dto.AttendanceDto
import com.eventr.repository.RegistrationRepository
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/attendance")
class AttendanceController(private val registrationRepository: RegistrationRepository) {
    
    @GetMapping("/{eventId}")
    fun getAttendance(@PathVariable eventId: UUID, @RequestParam(required = false) name: String?): List<AttendanceDto> {
        return registrationRepository.findByEventIdAndUserName(eventId, name).map { registration ->
            AttendanceDto(
                registrationId = registration.id,
                userName = registration.userName,
                userEmail = registration.userEmail,
                checkedIn = registration.checkedIn
            )
        }
    }
    
    @PutMapping("/{registrationId}/checkin")
    fun checkIn(@PathVariable registrationId: UUID): AttendanceDto {
        val registration = registrationRepository.findById(registrationId).orElseThrow()
        registration.checkedIn = true
        val checkedInRegistration = registrationRepository.save(registration)
        
        return AttendanceDto(
            registrationId = checkedInRegistration.id,
            userName = checkedInRegistration.userName,
            userEmail = checkedInRegistration.userEmail,
            checkedIn = checkedInRegistration.checkedIn
        )
    }
}
