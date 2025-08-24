package com.eventr.service.webhook

/**
 * Interface for HTTP client operations
 * Follows Dependency Inversion Principle by abstracting HTTP operations
 */
interface HttpClient {
    
    /**
     * Send HTTP POST request with payload
     */
    fun post(
        url: String,
        payload: String,
        headers: Map<String, String> = emptyMap(),
        timeoutSeconds: Int = 30
    ): HttpResponse
}

/**
 * HTTP response data class
 */
data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap(),
    val responseTimeMs: Long = 0,
    val isSuccess: Boolean = statusCode in 200..299
)