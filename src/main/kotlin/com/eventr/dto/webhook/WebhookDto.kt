package com.eventr.dto.webhook

import com.eventr.model.webhook.WebhookEventType
import com.eventr.model.webhook.WebhookStatus
import java.util.UUID

data class CreateWebhookRequest(
    val name: String,
    val url: String,
    val eventTypes: Set<WebhookEventType>,
    val maxRetries: Int = 3,
    val timeoutSeconds: Int = 30
)

data class UpdateWebhookRequest(
    val name: String? = null,
    val url: String? = null,
    val eventTypes: Set<WebhookEventType>? = null,
    val status: WebhookStatus? = null,
    val maxRetries: Int? = null,
    val timeoutSeconds: Int? = null
)

data class WebhookResponse(
    val id: UUID,
    val name: String,
    val url: String,
    val status: WebhookStatus,
    val eventTypes: Set<WebhookEventType>,
    val maxRetries: Int,
    val timeoutSeconds: Int,
    val totalDeliveries: Long,
    val successfulDeliveries: Long,
    val failedDeliveries: Long,
    val successRate: Double,
    val createdAt: String,
    val updatedAt: String,
    val lastDeliveryAt: String?,
    val lastSuccessAt: String?
)

data class WebhookDeliveryResponse(
    val id: UUID,
    val webhookId: UUID,
    val eventType: WebhookEventType,
    val status: String,
    val attemptCount: Int,
    val maxAttempts: Int,
    val responseStatus: Int?,
    val errorMessage: String?,
    val createdAt: String,
    val deliveredAt: String?,
    val nextRetryAt: String?
)

data class TestWebhookRequest(
    val eventType: WebhookEventType,
    val testData: Map<String, Any> = emptyMap()
)