package com.eventr.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * WebSocket configuration with secure CORS policy.
 * 
 * Security Improvements:
 * - Restricts WebSocket connections to specific allowed origins
 * - Environment-specific CORS configuration
 * - Prevents WebSocket hijacking from malicious sites
 * 
 * CORS Configuration:
 * - Development: Allows localhost origins for frontend development
 * - Production: Restricts to specific domain origins only
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    /**
     * Allowed origins for WebSocket connections.
     * Configured per environment for security.
     * 
     * Development: Multiple localhost ports for frontend dev servers
     * Production: Specific production domain(s) only
     */
    @Value("\${cors.allowed-origins:http://localhost:3000}")
    private lateinit var allowedOrigins: String

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // Enable a simple memory-based message broker to carry the messages back to the client
        config.enableSimpleBroker("/topic", "/queue")
        // Set prefix for messages that are bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // Register the "/ws" endpoint for WebSocket connections
        // SECURITY FIX: Replace wildcard "*" with specific allowed origins
        val originsArray = allowedOrigins.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toTypedArray()
            
        registry.addEndpoint("/ws")
            .setAllowedOrigins(*originsArray)  // ✅ Secure: Only specific origins allowed
            .withSockJS()
    }
}