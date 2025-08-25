package com.eventr.controller

import com.eventr.service.WebSocketEventService
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class WebSocketController(
    private val webSocketEventService: WebSocketEventService
) {
    
    @MessageMapping("/events/{eventId}/subscribe")
    @SendTo("/topic/events/{eventId}/updates")
    fun subscribeToEvent(@DestinationVariable eventId: String, @Payload message: Map<String, Any>): Map<String, Any> {
        return mapOf(
            "type" to "SUBSCRIPTION_CONFIRMED",
            "eventId" to eventId,
            "message" to "Successfully subscribed to event updates",
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    @MessageMapping("/events/subscribe-all")
    @SendTo("/topic/events/updates")
    fun subscribeToAllEvents(@Payload message: Map<String, Any>): Map<String, Any> {
        return mapOf(
            "type" to "GLOBAL_SUBSCRIPTION_CONFIRMED",
            "message" to "Successfully subscribed to all event updates",
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    @MessageMapping("/ping")
    @SendTo("/topic/system/ping")
    fun ping(@Payload message: Map<String, Any>): Map<String, Any> {
        return mapOf(
            "type" to "PONG",
            "timestamp" to System.currentTimeMillis(),
            "originalMessage" to message
        )
    }
}