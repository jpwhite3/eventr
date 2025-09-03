package com.eventr.controller

import com.eventr.service.FileUploadService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.multipart.MultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(FileUploadController::class)
@AutoConfigureMockMvc(addFilters = false)
class FileUploadControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var fileUploadService: FileUploadService

    @Test
    fun shouldUploadEventBanner() {
        val mockFile = MockMultipartFile(
            "file",
            "banner.jpg",
            "image/jpeg",
            "test image content".toByteArray()
        )
        
        val expectedUrl = "https://bucket.s3.amazonaws.com/events/images/banner_123456_abc123.jpg"
        `when`(fileUploadService.uploadEventImage(any(), eq("banner"))).thenReturn(expectedUrl)

        mockMvc.perform(
            multipart("/api/files/upload/event-banner")
                .file(mockFile)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.url").value(expectedUrl))
            .andExpect(jsonPath("$.message").value("Banner uploaded successfully"))

        verify(fileUploadService).uploadEventImage(any(), eq("banner"))
    }

    @Test
    fun shouldUploadEventThumbnail() {
        val mockFile = MockMultipartFile(
            "file",
            "thumbnail.png",
            "image/png",
            "test image content".toByteArray()
        )
        
        val expectedUrl = "https://bucket.s3.amazonaws.com/events/images/thumbnail_123456_def456.png"
        `when`(fileUploadService.uploadEventImage(any(), eq("thumbnail"))).thenReturn(expectedUrl)

        mockMvc.perform(
            multipart("/api/files/upload/event-thumbnail")
                .file(mockFile)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.url").value(expectedUrl))
            .andExpect(jsonPath("$.message").value("Thumbnail uploaded successfully"))

        verify(fileUploadService).uploadEventImage(any(), eq("thumbnail"))
    }

    @Test
    fun shouldHandleUploadError() {
        val mockFile = MockMultipartFile(
            "file",
            "invalid.txt",
            "text/plain",
            "not an image".toByteArray()
        )
        
        `when`(fileUploadService.uploadEventImage(any(), any()))
            .thenThrow(IllegalArgumentException("Invalid file type"))

        mockMvc.perform(
            multipart("/api/files/upload/event-banner")
                .file(mockFile)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid file type"))

        verify(fileUploadService).uploadEventImage(any(), eq("banner"))
    }

    @Test
    fun shouldUploadEventImageWithType() {
        val mockFile = MockMultipartFile(
            "file",
            "custom.jpg",
            "image/jpeg",
            "test image content".toByteArray()
        )
        
        val expectedUrl = "https://bucket.s3.amazonaws.com/events/images/custom_123456_ghi789.jpg"
        `when`(fileUploadService.uploadEventImage(any(), eq("custom"))).thenReturn(expectedUrl)

        mockMvc.perform(
            multipart("/api/files/upload/event-image")
                .file(mockFile)
                .param("type", "custom")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.url").value(expectedUrl))
            .andExpect(jsonPath("$.message").value("Image uploaded successfully"))

        verify(fileUploadService).uploadEventImage(any(), eq("custom"))
    }

    @Test
    fun shouldDeleteFile() {
        val fileUrl = "https://bucket.s3.amazonaws.com/events/images/banner_123456_abc123.jpg"
        doNothing().`when`(fileUploadService).deleteFile(fileUrl)

        mockMvc.perform(
            delete("/api/files/delete")
                .param("url", fileUrl)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("File deleted successfully"))

        verify(fileUploadService).deleteFile(fileUrl)
    }

    @Test
    fun shouldHandleDeleteError() {
        val fileUrl = "https://bucket.s3.amazonaws.com/events/images/nonexistent.jpg"
        doThrow(RuntimeException("File not found")).`when`(fileUploadService).deleteFile(fileUrl)

        mockMvc.perform(
            delete("/api/files/delete")
                .param("url", fileUrl)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("File not found"))

        verify(fileUploadService).deleteFile(fileUrl)
    }
}