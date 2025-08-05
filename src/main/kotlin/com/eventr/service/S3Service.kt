package com.eventr.service

import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream

@Service
class S3Service(private val s3Client: S3Client) {
    
    private val bucketName = "eventr-images"
    
    fun uploadFile(key: String, inputStream: InputStream): String {
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()
        
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, -1))
        
        return s3Client.utilities().getUrl { builder -> 
            builder.bucket(bucketName).key(key) 
        }.toExternalForm()
    }
}
