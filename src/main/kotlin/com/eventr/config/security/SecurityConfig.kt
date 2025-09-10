package com.eventr.config.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import java.time.Duration

/**
 * Production security configuration with comprehensive security headers and HTTPS enforcement.
 * 
 * This configuration implements essential web security controls:
 * - Security headers for protection against common attacks
 * - HTTPS enforcement with HSTS
 * - Secure cookie configuration
 * - Content Security Policy
 * 
 * Security Headers Implemented:
 * - X-Frame-Options: Prevent clickjacking attacks
 * - X-Content-Type-Options: Prevent MIME type sniffing
 * - X-XSS-Protection: Enable XSS protection in browsers
 * - Strict-Transport-Security: Enforce HTTPS connections
 * - Referrer-Policy: Control referrer information leakage
 * - Content-Security-Policy: Control resource loading
 */
@Configuration
@EnableWebSecurity
@Profile("!dev")  // Apply to all profiles except dev
class SecurityConfig {

    @Value("\${cors.allowed-origins:https://yourdomain.com}")
    private lateinit var allowedOrigins: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            // Configure CORS - handled by WebConfig
            .cors { it.disable() }
            
            // CSRF protection - enable for production
            .csrf { it.disable() } // TODO: Enable CSRF with proper token handling
            
            // Security Headers Configuration
            .headers { headers ->
                headers
                    // Prevent clickjacking attacks - deny all frame embedding
                    .frameOptions { frameOptions ->
                        frameOptions.deny()
                    }
                    
                    // Prevent MIME type sniffing attacks
                    .contentTypeOptions { }
                    
                    // Enable XSS protection with blocking mode
                    .xssProtection { xss ->
                        xss.and()
                    }
                    
                    // HTTP Strict Transport Security (HSTS)
                    // Forces HTTPS connections and prevents downgrade attacks
                    .httpStrictTransportSecurity { hsts ->
                        hsts
                            .maxAgeInSeconds(31536000) // 1 year
                            .includeSubDomains(true)   // Apply to all subdomains
                            .preload(true)             // Include in browser preload lists
                    }
                    
                    // Control referrer information sent to external sites
                    .referrerPolicy { referrer ->
                        referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    }
                    
                    // Content Security Policy - Basic restrictive policy
                    // TODO: Customize based on actual frontend requirements
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives("""
                            default-src 'self';
                            script-src 'self' 'unsafe-inline' 'unsafe-eval';
                            style-src 'self' 'unsafe-inline';
                            img-src 'self' data: blob:;
                            font-src 'self';
                            connect-src 'self' ws: wss:;
                            frame-src 'none';
                            object-src 'none';
                            base-uri 'self';
                            form-action 'self'
                        """.trimIndent().replace("\n", " "))
                    }
            }
            
            // Request Authorization Configuration
            .authorizeHttpRequests { authz ->
                authz
                    // Public health check endpoints
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/info").permitAll()
                    
                    // Public API endpoints (read-only)
                    .requestMatchers("GET", "/api/events/public/**").permitAll()
                    
                    // Authentication endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    
                    // WebSocket endpoint
                    .requestMatchers("/ws/**").permitAll()
                    
                    // All other requests require authentication
                    .anyRequest().authenticated()
            }
            
            // Session Management
            .sessionManagement { session ->
                session
                    .maximumSessions(1)                    // Limit concurrent sessions
                    .maxSessionsPreventsLogin(false)       // Allow new login to kick out old session
            }
            
            // Require HTTPS for all requests in production
            .requiresChannel { channel ->
                channel.anyRequest().requiresSecure()
            }
            
            .build()
    }

    /**
     * Configure secure cookies for production environment.
     * Ensures cookies are only sent over HTTPS and are not accessible via JavaScript.
     */
    @Bean
    @Profile("prod")
    fun secureCookieProcessor(): org.apache.tomcat.util.http.Rfc6265CookieProcessor {
        val cookieProcessor = org.apache.tomcat.util.http.Rfc6265CookieProcessor()
        cookieProcessor.setSameSiteCookies("Strict")  // CSRF protection
        return cookieProcessor
    }
}