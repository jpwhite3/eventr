package com.eventr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("FileUpload Service Tests")
public class FileUploadServiceTest {

    @Mock
    private S3Client mockS3Client;
    
    @Mock
    private S3Utilities mockS3Utilities;
    
    @Mock
    private MultipartFile mockMultipartFile;
    
    @Mock
    private PutObjectResponse mockPutObjectResponse;
    
    private FileUploadService fileUploadService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        fileUploadService = new FileUploadService(mockS3Client);
        
        // Set the bucket name using reflection
        ReflectionTestUtils.setField(fileUploadService, "bucketName", "test-bucket");
        
        when(mockS3Client.utilities()).thenReturn(mockS3Utilities);
    }

    @Test
    @DisplayName("Should upload event image successfully")
    void shouldUploadEventImageSuccessfully() throws Exception {
        // Given
        byte[] imageContent = "fake image content".getBytes();
        
        when(mockMultipartFile.getSize()).thenReturn((long) imageContent.length);
        when(mockMultipartFile.getContentType()).thenReturn("image/jpeg");
        when(mockMultipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockMultipartFile.isEmpty()).thenReturn(false);
        when(mockMultipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(imageContent));
        
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockPutObjectResponse);
        
        URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/events/images/test.jpg");
        when(mockS3Utilities.getUrl(any())).thenReturn(expectedUrl);

        // When
        String result = fileUploadService.uploadEventImage(mockMultipartFile, "cover");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("test-bucket"));
        verify(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should throw exception for empty file")
    void shouldThrowExceptionForEmptyFile() {
        // Given
        when(mockMultipartFile.isEmpty()).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                fileUploadService.uploadEventImage(mockMultipartFile, "cover")
        );
        
        assertEquals("File cannot be empty", exception.getMessage());
        verify(mockS3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should throw exception for file too large")
    void shouldThrowExceptionForFileTooLarge() {
        // Given
        when(mockMultipartFile.isEmpty()).thenReturn(false);
        when(mockMultipartFile.getSize()).thenReturn(15L * 1024 * 1024); // 15MB, over the 10MB limit

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                fileUploadService.uploadEventImage(mockMultipartFile, "cover")
        );
        
        assertEquals("File size exceeds maximum allowed size of 10MB", exception.getMessage());
        verify(mockS3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid file type")
    void shouldThrowExceptionForInvalidFileType() {
        // Given
        when(mockMultipartFile.isEmpty()).thenReturn(false);
        when(mockMultipartFile.getSize()).thenReturn(1024L);
        when(mockMultipartFile.getContentType()).thenReturn("text/plain");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                fileUploadService.uploadEventImage(mockMultipartFile, "cover")
        );
        
        assertEquals("Only image files are allowed (JPEG, PNG, GIF, WebP)", exception.getMessage());
        verify(mockS3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should handle null content type")
    void shouldHandleNullContentType() throws Exception {
        // Given
        byte[] imageContent = "fake image content".getBytes();
        
        when(mockMultipartFile.getSize()).thenReturn((long) imageContent.length);
        when(mockMultipartFile.getContentType()).thenReturn(null);
        when(mockMultipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockMultipartFile.isEmpty()).thenReturn(false);
        when(mockMultipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(imageContent));
        
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockPutObjectResponse);
        
        URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/events/images/test.jpg");
        when(mockS3Utilities.getUrl(any())).thenReturn(expectedUrl);

        // When & Then
        // Should throw exception because null content type is not in allowed types
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                fileUploadService.uploadEventImage(mockMultipartFile, "cover")
        );
        
        assertEquals("Only image files are allowed (JPEG, PNG, GIF, WebP)", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle upload failure")
    void shouldHandleUploadFailure() throws Exception {
        // Given
        byte[] imageContent = "fake image content".getBytes();
        
        when(mockMultipartFile.getSize()).thenReturn((long) imageContent.length);
        when(mockMultipartFile.getContentType()).thenReturn("image/jpeg");
        when(mockMultipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockMultipartFile.isEmpty()).thenReturn(false);
        when(mockMultipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(imageContent));
        
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                fileUploadService.uploadEventImage(mockMultipartFile, "cover")
        );
        
        assertTrue(exception.getMessage().contains("Failed to upload file"));
        verify(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}