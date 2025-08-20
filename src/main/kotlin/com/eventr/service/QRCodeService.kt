package com.eventr.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class QRCodeService {

    @Value("\${app.base-url:http://localhost:3000}")
    private lateinit var baseUrl: String
    
    @Value("\${app.qr-secret:your-secure-secret-key-change-in-production}")
    private lateinit var qrSecret: String

    private val qrCodeWriter = QRCodeWriter()
    
    data class QRCodeData(
        val content: String,
        val imageBytes: ByteArray,
        val expiresAt: LocalDateTime?
    )

    /**
     * Generate QR code for event check-in
     */
    fun generateEventCheckInQR(eventId: String, userId: String, expiresAt: LocalDateTime? = null): QRCodeData {
        val timestamp = System.currentTimeMillis()
        val signature = generateSignature("event:$eventId:$userId:$timestamp")
        val content = "$baseUrl/checkin/event/$eventId?user=$userId&t=$timestamp&sig=$signature"
        
        val imageBytes = generateQRCodeImage(content, 300, 300)
        return QRCodeData(content, imageBytes, expiresAt)
    }

    /**
     * Generate QR code for session check-in
     */
    fun generateSessionCheckInQR(sessionId: String, userId: String, expiresAt: LocalDateTime? = null): QRCodeData {
        val timestamp = System.currentTimeMillis()
        val signature = generateSignature("session:$sessionId:$userId:$timestamp")
        val content = "$baseUrl/checkin/session/$sessionId?user=$userId&t=$timestamp&sig=$signature"
        
        val imageBytes = generateQRCodeImage(content, 300, 300)
        return QRCodeData(content, imageBytes, expiresAt)
    }

    /**
     * Generate QR code for staff check-in interface (no specific user)
     */
    fun generateStaffCheckInQR(eventId: String, sessionId: String? = null): QRCodeData {
        val timestamp = System.currentTimeMillis()
        val identifier = if (sessionId != null) "session:$sessionId" else "event:$eventId"
        val signature = generateSignature("staff:$identifier:$timestamp")
        
        val content = if (sessionId != null) {
            "$baseUrl/staff/checkin/session/$sessionId?t=$timestamp&sig=$signature"
        } else {
            "$baseUrl/staff/checkin/event/$eventId?t=$timestamp&sig=$signature"
        }
        
        val imageBytes = generateQRCodeImage(content, 400, 400, withLogo = true)
        return QRCodeData(content, imageBytes, null)
    }

    /**
     * Generate QR code for attendee badge
     */
    fun generateAttendeeBadge(eventId: String, userId: String, userName: String): QRCodeData {
        val timestamp = System.currentTimeMillis()
        val signature = generateSignature("badge:$eventId:$userId:$timestamp")
        val content = "$baseUrl/badge/$eventId/$userId?t=$timestamp&sig=$signature"
        
        val imageBytes = generateBadgeQRCode(content, userName, 200, 200)
        return QRCodeData(content, imageBytes, null)
    }

    /**
     * Validate QR code signature
     */
    fun validateQRSignature(type: String, identifier: String, userId: String?, timestamp: String, signature: String): Boolean {
        val expectedPayload = if (userId != null) {
            "$type:$identifier:$userId:$timestamp"
        } else {
            "$type:$identifier:$timestamp"
        }
        
        val expectedSignature = generateSignature(expectedPayload)
        return signature == expectedSignature && !isExpired(timestamp.toLong())
    }

    /**
     * Generate bulk QR codes for event attendees
     */
    fun generateBulkAttendeeQRCodes(eventId: String, attendees: List<Pair<String, String>>): Map<String, QRCodeData> {
        return attendees.associate { (userId, userName) ->
            userId to generateAttendeeBadge(eventId, userId, userName)
        }
    }

    private fun generateQRCodeImage(content: String, width: Int, height: Int, withLogo: Boolean = false): ByteArray {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )

        val bitMatrix: BitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix)

        // Add logo if requested
        if (withLogo) {
            addLogoToQRCode(bufferedImage)
        }

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "PNG", outputStream)
        return outputStream.toByteArray()
    }

    private fun generateBadgeQRCode(content: String, userName: String, width: Int, height: Int): ByteArray {
        val qrSize = minOf(width, height)
        val totalHeight = qrSize + 40 // Extra space for name

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )

        val bitMatrix: BitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)
        val qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix)

        // Create larger image with space for text
        val finalImage = BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB)
        val graphics: Graphics2D = finalImage.createGraphics()

        // White background
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, width, totalHeight)

        // Draw QR code
        graphics.drawImage(qrImage, (width - qrSize) / 2, 0, null)

        // Draw name
        graphics.color = Color.BLACK
        graphics.font = graphics.font.deriveFont(12f)
        val fontMetrics = graphics.fontMetrics
        val textWidth = fontMetrics.stringWidth(userName)
        val textX = (width - textWidth) / 2
        val textY = qrSize + 25

        graphics.drawString(userName, textX, textY)
        graphics.dispose()

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(finalImage, "PNG", outputStream)
        return outputStream.toByteArray()
    }

    private fun addLogoToQRCode(qrImage: BufferedImage) {
        val graphics: Graphics2D = qrImage.createGraphics()
        val logoSize = qrImage.width / 8
        val logoX = (qrImage.width - logoSize) / 2
        val logoY = (qrImage.height - logoSize) / 2

        // Add a simple logo placeholder (white square with border)
        graphics.color = Color.WHITE
        graphics.fillRect(logoX, logoY, logoSize, logoSize)
        graphics.color = Color.BLACK
        graphics.drawRect(logoX, logoY, logoSize, logoSize)

        // Add "E" for Eventr
        graphics.font = graphics.font.deriveFont((logoSize * 0.6).toFloat())
        val fontMetrics = graphics.fontMetrics
        val textWidth = fontMetrics.stringWidth("E")
        val textHeight = fontMetrics.ascent
        val textX = logoX + (logoSize - textWidth) / 2
        val textY = logoY + (logoSize + textHeight) / 2 - fontMetrics.descent

        graphics.drawString("E", textX, textY)
        graphics.dispose()
    }

    private fun generateSignature(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(qrSecret.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(payload.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).take(16)
    }

    private fun isExpired(timestamp: Long, expirationHours: Long = 24): Boolean {
        val now = System.currentTimeMillis()
        val expirationTime = timestamp + (expirationHours * 60 * 60 * 1000)
        return now > expirationTime
    }

    /**
     * Generate QR code for event analytics (staff-only)
     */
    fun generateAnalyticsQR(eventId: String): QRCodeData {
        val timestamp = System.currentTimeMillis()
        val signature = generateSignature("analytics:$eventId:$timestamp")
        val content = "$baseUrl/analytics/event/$eventId?t=$timestamp&sig=$signature"
        
        val imageBytes = generateQRCodeImage(content, 300, 300, withLogo = true)
        return QRCodeData(content, imageBytes, null)
    }

    /**
     * Generate check-in summary QR for session completion
     */
    fun generateSessionSummaryQR(sessionId: String): QRCodeData {
        val timestamp = System.currentTimeMillis()
        val signature = generateSignature("summary:$sessionId:$timestamp")
        val content = "$baseUrl/session/$sessionId/summary?t=$timestamp&sig=$signature"
        
        val imageBytes = generateQRCodeImage(content, 300, 300)
        return QRCodeData(content, imageBytes, null)
    }
}