package com.eventr.controller

import com.eventr.service.FileStorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("/api/files")
class FileController(private val fileStorageService: FileStorageService) {
    
    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        return try {
            val fileUrl = fileStorageService.uploadFile(file)
            ResponseEntity.ok(fileUrl)
        } catch (e: IOException) {
            ResponseEntity.status(500).body("Failed to upload file: ${e.message}")
        }
    }
}
