package com.eventr.service

import io.awspring.cloud.s3.S3Template
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.time.Duration
import java.util.UUID

@Service
class FileStorageService(private val s3Template: S3Template) {
    
    @Value("\${aws.s3.bucket-name}")
    private lateinit var bucketName: String
    
    @Throws(IOException::class)
    fun uploadFile(file: MultipartFile): String {
        val fileName = "${UUID.randomUUID()}_${file.originalFilename}"
        s3Template.upload(bucketName, fileName, file.inputStream)
        return s3Template.createSignedGetURL(bucketName, fileName, Duration.ofHours(1)).toString()
    }
}
