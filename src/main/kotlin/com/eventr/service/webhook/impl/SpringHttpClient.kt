package com.eventr.service.webhook.impl

import com.eventr.service.webhook.HttpClient
import com.eventr.service.webhook.HttpResponse
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.time.Duration
import kotlin.system.measureTimeMillis

@Service
class SpringHttpClient(
    private val restTemplate: RestTemplate = RestTemplate()
) : HttpClient {
    
    private val logger = LoggerFactory.getLogger(SpringHttpClient::class.java)
    
    override fun post(
        url: String,
        payload: String,
        headers: Map<String, String>,
        timeoutSeconds: Int
    ): HttpResponse {
        logger.debug("Sending POST request to: {}", url)
        
        val httpHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            headers.forEach { (key, value) ->
                set(key, value)
            }
        }
        
        val entity = HttpEntity(payload, httpHeaders)
        
        return try {
            var httpResponse: ResponseEntity<String>
            val responseTime = measureTimeMillis {
                httpResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String::class.java
                )
            }
            
            HttpResponse(
                statusCode = httpResponse.statusCode.value(),
                body = httpResponse.body ?: "",
                headers = httpResponse.headers.toSingleValueMap(),
                responseTimeMs = responseTime,
                isSuccess = httpResponse.statusCode.is2xxSuccessful
            ).also {
                logger.debug("Received response from {}: status={}, time={}ms", 
                    url, it.statusCode, it.responseTimeMs)
            }
            
        } catch (e: RestClientException) {
            logger.error("HTTP request failed for URL: {}", url, e)
            HttpResponse(
                statusCode = 0,
                body = e.message ?: "HTTP request failed",
                responseTimeMs = 0,
                isSuccess = false
            )
        } catch (e: Exception) {
            logger.error("Unexpected error during HTTP request to: {}", url, e)
            HttpResponse(
                statusCode = 0,
                body = e.message ?: "Unexpected error",
                responseTimeMs = 0,
                isSuccess = false
            )
        }
    }
}