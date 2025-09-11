package com.eventr.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.net.URL
import java.util.*

@Service
class FileUploadService(
    @Autowired private val s3Client: S3Client
) {
    @Value("\${aws.s3.bucket-name}")
    private lateinit var bucketName: String

    private val allowedImageTypes = setOf(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    )
    
    private val maxFileSize = 10 * 1024 * 1024 // 10MB

    fun uploadEventImage(file: MultipartFile, type: String): String {
        validateFile(file)
        
        val fileName = generateFileName(file.originalFilename ?: "image", type)
        val contentType = file.contentType ?: "application/octet-stream"
        
        try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("events/images/$fileName")
                .contentType(contentType)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .metadata(mapOf(
                    "original-filename" to (file.originalFilename ?: "unknown"),
                    "upload-type" to type,
                    "upload-timestamp" to System.currentTimeMillis().toString()
                ))
                .build()

            val requestBody = RequestBody.fromInputStream(file.inputStream, file.size)
            s3Client.putObject(putObjectRequest, requestBody)

            return getPublicUrl(fileName)
        } catch (e: Exception) {
            throw RuntimeException("Failed to upload file: ${e.message}", e)
        }
    }

    fun deleteFile(fileUrl: String) {
        try {
            val fileName = extractFileNameFromUrl(fileUrl)
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key("events/images/$fileName")
                .build()
            
            s3Client.deleteObject(deleteObjectRequest)
        } catch (e: Exception) {
            throw RuntimeException("Failed to delete file: ${e.message}", e)
        }
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw IllegalArgumentException("File is empty")
        }
        
        if (file.size > maxFileSize) {
            throw IllegalArgumentException("File size exceeds maximum allowed size of ${maxFileSize / (1024 * 1024)}MB")
        }
        
        val contentType = file.contentType
        if (contentType == null || !allowedImageTypes.contains(contentType.lowercase())) {
            throw IllegalArgumentException("Invalid file type. Allowed types: ${allowedImageTypes.joinToString(", ")}")
        }
    }

    private fun generateFileName(originalFileName: String, type: String): String {
        val extension = originalFileName.substringAfterLast(".", "")
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        return "${type}_${timestamp}_${uuid}.${extension}"
    }

    private fun getPublicUrl(fileName: String): String {
        return try {
            val getUrlRequest = GetUrlRequest.builder()
                .bucket(bucketName)
                .key("events/images/$fileName")
                .build()
            
            s3Client.utilities().getUrl(getUrlRequest).toString()
        } catch (e: Exception) {
            // Fallback to constructing URL manually
            "https://$bucketName.s3.amazonaws.com/events/images/$fileName"
        }
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val urlObj = java.net.URI.create(url).toURL()
            val path = urlObj.path
            path.substringAfterLast("/")
        } catch (e: Exception) {
            url.substringAfterLast("/")
        }
    }

    // Local development fallback - stores files locally when S3 is not available
    fun uploadEventImageLocal(file: MultipartFile, type: String): String {
        validateFile(file)
        
        val uploadsDir = java.io.File("uploads/events/images")
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs()
        }
        
        val fileName = generateFileName(file.originalFilename ?: "image", type)
        val targetFile = java.io.File(uploadsDir, fileName)
        
        try {
            file.transferTo(targetFile)
            return "/uploads/events/images/$fileName"
        } catch (e: Exception) {
            throw RuntimeException("Failed to save file locally: ${e.message}", e)
        }
    }
}