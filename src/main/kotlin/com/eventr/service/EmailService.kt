package com.eventr.service

import com.eventr.model.EventInstance
import com.eventr.model.Registration
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.model.property.Version
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.ZoneId
import java.util.Date

@Service
class EmailService(private val mailSender: JavaMailSender) {
    
    @Throws(MessagingException::class, IOException::class)
    fun sendRegistrationConfirmation(registration: Registration) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)
        
        registration.userEmail?.let { email ->
            helper.setTo(email)
        } ?: throw IllegalArgumentException("User email cannot be null")
        helper.setSubject("Event Registration Confirmation: ${registration.eventInstance?.event?.name}")
        helper.setText("Thank you for registering for ${registration.eventInstance?.event?.name}.\n\nPlease find the event details attached.")
        
        val icsFile = registration.eventInstance?.let { createIcsFile(it) }
        icsFile?.let {
            helper.addAttachment("event.ics", ByteArrayResource(it))
        }
        
        mailSender.send(message)
    }
    
    @Throws(IOException::class)
    private fun createIcsFile(eventInstance: EventInstance): ByteArray {
        val calendar = Calendar().apply {
            properties.add(ProdId("-//Eventr//EN"))
            properties.add(Version.VERSION_2_0)
        }
        
        val event = eventInstance.dateTime?.let { dateTime ->
            VEvent(
                net.fortuna.ical4j.model.DateTime(
                    Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
                ),
                eventInstance.event?.name
            ).apply {
                properties.add(Uid(eventInstance.id.toString()))
            }
        }
        
        event?.let { calendar.components.add(it) }
        
        val out = ByteArrayOutputStream()
        CalendarOutputter().output(calendar, out)
        
        return out.toByteArray()
    }
}
