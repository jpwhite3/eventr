package com.eventr.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * CORS configuration properties
 */
@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    var allowedOrigins: String = "",
    var allowedMethods: String = "GET,POST,PUT,DELETE,OPTIONS",
    var allowedHeaders: String = "*",
    var allowCredentials: Boolean = true,
    var maxAge: Long = 3600
)

/**
 * Web configuration for CORS and other web-related settings
 * Supports environment-specific CORS configuration via properties
 */
@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class WebConfig(
    private val corsProperties: CorsProperties,
    private val environment: Environment
) : WebMvcConfigurer {
    
    private val logger = LoggerFactory.getLogger(WebConfig::class.java)
    
    /**
     * Configure CORS based on environment-specific properties
     * Falls back to environment variables if properties are not set
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = getConfiguredOrigins()
        val methods = corsProperties.allowedMethods.split(",").map { it.trim() }.toTypedArray()
        val headers = if (corsProperties.allowedHeaders == "*") arrayOf("*") 
                     else corsProperties.allowedHeaders.split(",").map { it.trim() }.toTypedArray()
        
        logger.info("Configuring CORS with origins: ${origins.contentToString()}")
        logger.info("Allowed methods: ${methods.contentToString()}")
        logger.info("Allowed headers: ${headers.contentToString()}")
        logger.info("Allow credentials: ${corsProperties.allowCredentials}")
        
        registry.addMapping("/api/**")
            .allowedOrigins(*origins)
            .allowedMethods(*methods)
            .allowedHeaders(*headers)
            .allowCredentials(corsProperties.allowCredentials)
            .maxAge(corsProperties.maxAge)
            
        // Additional mapping for WebSocket endpoints
        registry.addMapping("/ws/**")
            .allowedOrigins(*origins)
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowCredentials(corsProperties.allowCredentials)
    }
    
    /**
     * Get configured origins with fallbacks and environment variable support
     */
    private fun getConfiguredOrigins(): Array<String> {
        // First try property configuration
        if (corsProperties.allowedOrigins.isNotBlank()) {
            return corsProperties.allowedOrigins.split(",").map { it.trim() }.toTypedArray()
        }
        
        // Fallback to environment variable
        val envOrigins = System.getenv("CORS_ALLOWED_ORIGINS")
        if (!envOrigins.isNullOrBlank()) {
            logger.info("Using CORS origins from environment variable")
            return envOrigins.split(",").map { it.trim() }.toTypedArray()
        }
        
        // Final fallback based on active profiles
        return when {
            environment.activeProfiles.contains("dev") -> arrayOf(
                "http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003",
                "http://127.0.0.1:3000", "http://127.0.0.1:3001", "http://127.0.0.1:3002", "http://127.0.0.1:3003"
            )
            environment.activeProfiles.contains("test") -> arrayOf(
                "http://localhost:3000", "http://localhost:8080"
            )
            else -> {
                logger.warn("No CORS origins configured! Using restrictive default.")
                arrayOf("https://yourdomain.com")
            }
        }
    }
}
