package com.eventr.controller

import com.eventr.service.S3Service
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("/api/files")
class FileUploadController(private val s3Service: S3Service) {
    
    @PostMapping("/upload-s3")
    @Throws(IOException::class)
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val fileUrl = s3Service.uploadFile(file.originalFilename ?: "unknown", file.inputStream)
        return ResponseEntity.ok(fileUrl)
    }
}
