package com.eventr.service

import com.eventr.repository.EventRepository
import com.eventr.repository.RegistrationRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class EmailReminderService(
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository,
    private val emailService: EmailService
) {
    
    // Run every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    fun sendDailyReminders() {
        sendRemindersForDaysAhead(1)  // 1 day reminder
        sendRemindersForDaysAhead(3)  // 3 day reminder
        sendRemindersForDaysAhead(7)  // 1 week reminder
    }
    
    private fun sendRemindersForDaysAhead(daysAhead: Int) {
        val now = ZonedDateTime.now()
        val targetDate = now.plusDays(daysAhead.toLong())
        
        // Find events starting in exactly N days
        val upcomingEvents = eventRepository.findAll().filter { event ->
            event.startDateTime?.let { startDateTime ->
                val eventStart = startDateTime.atZone(ZoneId.systemDefault())
                val daysDifference = ChronoUnit.DAYS.between(now.toLocalDate(), eventStart.toLocalDate())
                daysDifference == daysAhead.toLong()
            } ?: false
        }
        
        upcomingEvents.forEach { event ->
            val registrations = registrationRepository.findByEventId(event.id!!)
            
            registrations.forEach { registration ->
                try {
                    emailService.sendEventReminder(registration, daysAhead)
                } catch (e: Exception) {
                    // Log error but continue processing other registrations
                    println("Failed to send reminder email to ${registration.userEmail}: ${e.message}")
                }
            }
        }
    }
    
    // Manual reminder trigger for admin use
    fun sendManualReminder(eventId: java.util.UUID, daysUntilEvent: Int) {
        val event = eventRepository.findById(eventId).orElseThrow {
            IllegalArgumentException("Event not found")
        }
        
        val registrations = registrationRepository.findByEventId(eventId)
        
        registrations.forEach { registration ->
            try {
                emailService.sendEventReminder(registration, daysUntilEvent)
            } catch (e: Exception) {
                println("Failed to send manual reminder to ${registration.userEmail}: ${e.message}")
            }
        }
    }
    
    // Send update notifications to all attendees of an event
    fun sendEventUpdateNotification(eventId: java.util.UUID, updateMessage: String) {
        val event = eventRepository.findById(eventId).orElseThrow {
            IllegalArgumentException("Event not found")
        }
        
        val registrations = registrationRepository.findByEventId(eventId)
        
        try {
            emailService.sendEventUpdate(event, updateMessage, registrations.toList())
        } catch (e: Exception) {
            println("Failed to send event update notifications: ${e.message}")
            throw e
        }
    }
}