package com.eventr.service.webhook.impl

import com.eventr.service.webhook.WebhookSignatureService
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class WebhookSignatureServiceImpl : WebhookSignatureService {
    
    private val algorithm = "HmacSHA256"
    private val secureRandom = SecureRandom()
    
    override fun generateSignature(payload: String, secret: String): String {
        val mac = Mac.getInstance(algorithm)
        val secretKeySpec = SecretKeySpec(secret.toByteArray(), algorithm)
        mac.init(secretKeySpec)
        
        val hash = mac.doFinal(payload.toByteArray())
        return "sha256=" + Base64.getEncoder().encodeToString(hash)
    }
    
    override fun validateSignature(payload: String, signature: String, secret: String): Boolean {
        val expectedSignature = generateSignature(payload, secret)
        return MessageDigest.isEqual(
            signature.toByteArray(),
            expectedSignature.toByteArray()
        )
    }
    
    override fun generateSecret(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}