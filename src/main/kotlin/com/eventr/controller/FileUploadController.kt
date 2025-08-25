package com.eventr.controller

import com.eventr.service.FileUploadService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003"])
class FileUploadController(
    @Autowired private val fileUploadService: FileUploadService
) {

    @PostMapping("/upload/event-banner")
    fun uploadEventBanner(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        return try {
            val imageUrl = fileUploadService.uploadEventImage(file, "banner")
            ResponseEntity.ok(mapOf("url" to imageUrl, "message" to "Banner uploaded successfully"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Upload failed")))
        }
    }

    @PostMapping("/upload/event-thumbnail")
    fun uploadEventThumbnail(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        return try {
            val imageUrl = fileUploadService.uploadEventImage(file, "thumbnail")
            ResponseEntity.ok(mapOf("url" to imageUrl, "message" to "Thumbnail uploaded successfully"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Upload failed")))
        }
    }

    @PostMapping("/upload/event-image")
    fun uploadEventImage(@RequestParam("file") file: MultipartFile, @RequestParam("type") type: String): ResponseEntity<Map<String, String>> {
        return try {
            val imageUrl = fileUploadService.uploadEventImage(file, type)
            ResponseEntity.ok(mapOf("url" to imageUrl, "message" to "Image uploaded successfully"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Upload failed")))
        }
    }

    @DeleteMapping("/delete")
    fun deleteFile(@RequestParam("url") fileUrl: String): ResponseEntity<Map<String, String>> {
        return try {
            fileUploadService.deleteFile(fileUrl)
            ResponseEntity.ok(mapOf("message" to "File deleted successfully"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Delete failed")))
        }
    }
}