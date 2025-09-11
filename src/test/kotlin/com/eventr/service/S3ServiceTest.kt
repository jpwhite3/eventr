package com.eventr.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Utilities
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.net.URI

@DisplayName("S3 Service Tests")
class S3ServiceTest {

    private val mockS3Client = mock(S3Client::class.java)
    private val mockS3Utilities = mock(S3Utilities::class.java)
    private val mockPutObjectResponse = mock(PutObjectResponse::class.java)
    private lateinit var s3Service: S3Service

    @BeforeEach
    fun setup() {
        s3Service = S3Service(mockS3Client)
        `when`(mockS3Client.utilities()).thenReturn(mockS3Utilities)
    }

    @Test
    @DisplayName("Should upload file successfully and return URL")
    fun shouldUploadFileSuccessfullyAndReturnUrl() {
        // Given
        val key = "test-file.jpg"
        val inputStream: InputStream = ByteArrayInputStream("test content".toByteArray())
        val expectedUrl = URI.create("https://eventr-images.s3.amazonaws.com/test-file.jpg").toURL()
        
        `when`(mockS3Client.putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java)))
            .thenReturn(mockPutObjectResponse)
        
        `when`(mockS3Utilities.getUrl(any<java.util.function.Consumer<software.amazon.awssdk.services.s3.model.GetUrlRequest.Builder>>()))
            .thenReturn(expectedUrl)

        // When
        val result = s3Service.uploadFile(key, inputStream)

        // Then
        assertEquals(expectedUrl.toExternalForm(), result)
        verify(mockS3Client).putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java))
        verify(mockS3Client).utilities()
    }

    @Test
    @DisplayName("Should create correct PutObjectRequest")
    fun shouldCreateCorrectPutObjectRequest() {
        // Given
        val key = "documents/report.pdf"
        val inputStream: InputStream = ByteArrayInputStream("pdf content".toByteArray())
        val expectedUrl = URI.create("https://eventr-images.s3.amazonaws.com/documents/report.pdf").toURL()
        
        `when`(mockS3Client.putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java)))
            .thenReturn(mockPutObjectResponse)
        
        `when`(mockS3Utilities.getUrl(any<java.util.function.Consumer<software.amazon.awssdk.services.s3.model.GetUrlRequest.Builder>>()))
            .thenReturn(expectedUrl)

        // When
        s3Service.uploadFile(key, inputStream)

        // Then
        verify(mockS3Client).putObject(argThat { request: PutObjectRequest ->
            request.bucket() == "eventr-images" && request.key() == key
        }, any(RequestBody::class.java))
    }

    @Test
    @DisplayName("Should handle different file types")
    fun shouldHandleDifferentFileTypes() {
        // Given
        val key = "images/photo.png"
        val inputStream: InputStream = ByteArrayInputStream("image data".toByteArray())
        val expectedUrl = URI.create("https://eventr-images.s3.amazonaws.com/images/photo.png").toURL()
        
        `when`(mockS3Client.putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java)))
            .thenReturn(mockPutObjectResponse)
        
        `when`(mockS3Utilities.getUrl(any<java.util.function.Consumer<software.amazon.awssdk.services.s3.model.GetUrlRequest.Builder>>()))
            .thenReturn(expectedUrl)

        // When
        val result = s3Service.uploadFile(key, inputStream)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("photo.png"))
        verify(mockS3Client).putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java))
    }

    @Test
    @DisplayName("Should handle empty file content")
    fun shouldHandleEmptyFileContent() {
        // Given
        val key = "empty-file.txt"
        val inputStream: InputStream = ByteArrayInputStream(byteArrayOf())
        val expectedUrl = URI.create("https://eventr-images.s3.amazonaws.com/empty-file.txt").toURL()
        
        `when`(mockS3Client.putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java)))
            .thenReturn(mockPutObjectResponse)
        
        `when`(mockS3Utilities.getUrl(any<java.util.function.Consumer<software.amazon.awssdk.services.s3.model.GetUrlRequest.Builder>>()))
            .thenReturn(expectedUrl)

        // When
        val result = s3Service.uploadFile(key, inputStream)

        // Then
        assertNotNull(result)
        assertEquals(expectedUrl.toExternalForm(), result)
        verify(mockS3Client).putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java))
    }

    @Test
    @DisplayName("Should use correct bucket name")
    fun shouldUseCorrectBucketName() {
        // Given
        val key = "test-key"
        val inputStream: InputStream = ByteArrayInputStream("content".toByteArray())
        val expectedUrl = URI.create("https://eventr-images.s3.amazonaws.com/test-key").toURL()
        
        `when`(mockS3Client.putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java)))
            .thenReturn(mockPutObjectResponse)
        
        `when`(mockS3Utilities.getUrl(any<java.util.function.Consumer<software.amazon.awssdk.services.s3.model.GetUrlRequest.Builder>>()))
            .thenReturn(expectedUrl)

        // When
        s3Service.uploadFile(key, inputStream)

        // Then
        verify(mockS3Client).putObject(argThat { request: PutObjectRequest ->
            request.bucket() == "eventr-images"
        }, any(RequestBody::class.java))
    }
}