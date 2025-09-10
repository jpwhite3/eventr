package com.eventr.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Development-only security configuration that allows public access to GET endpoints
 * This enables the frontend to work without authentication during development
 */
@Configuration
@EnableWebSecurity
@Profile("dev")
class DevSecurityConfig {

    @Bean
    fun devSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .cors { it.disable() } // CORS is handled by WebConfig
            .csrf { it.disable() } // Disable CSRF for development
            
            // Basic security headers for development (less restrictive than production)
            .headers { headers ->
                headers
                    // Basic XSS protection
                    .xssProtection { xss ->
                        xss.and()
                    }
                    // Prevent MIME sniffing
                    .contentTypeOptions { }
                    // Allow frames for development tools
                    .frameOptions { frameOptions ->
                        frameOptions.sameOrigin()
                    }
            }
            
            .authorizeHttpRequests { authz ->
                authz
                    // Allow all GET requests for development
                    .requestMatchers("GET", "/api/events/**").permitAll()
                    .requestMatchers("GET", "/api/sessions/**").permitAll()
                    .requestMatchers("GET", "/api/registrations/**").permitAll()
                    .requestMatchers("GET", "/api/analytics/**").permitAll()
                    .requestMatchers("GET", "/api/resources/**").permitAll()
                    .requestMatchers("GET", "/api/mock/**").permitAll()
                    .requestMatchers("GET", "/api/capacity/**").permitAll()
                    .requestMatchers("GET", "/api/checkin/stats/**").permitAll()
                    
                    // Allow specific working POST endpoints for development
                    .requestMatchers("POST", "/api/events/*/publish").permitAll()
                    .requestMatchers("POST", "/api/events").permitAll()
                    .requestMatchers("PUT", "/api/events/**").permitAll()
                    .requestMatchers("DELETE", "/api/events/**").permitAll()
                    
                    // Allow auth endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    
                    // Allow actuator for development
                    .requestMatchers("/actuator/**").permitAll()
                    
                    // Require authentication for all other requests
                    .anyRequest().authenticated()
            }
            .build()
    }
}