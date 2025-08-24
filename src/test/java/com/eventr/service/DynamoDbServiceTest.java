package com.eventr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DynamoDb Service Tests")
public class DynamoDbServiceTest {

    @Mock
    private DynamoDbClient mockDynamoDbClient;
    
    private DynamoDbService dynamoDbService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dynamoDbService = new DynamoDbService(mockDynamoDbClient);
    }

    @Test
    @DisplayName("Should save form definition successfully")
    void shouldSaveFormDefinitionSuccessfully() {
        // Given
        String eventId = "event-123";
        String formData = "{\"fields\":[{\"type\":\"text\",\"label\":\"Name\"}]}";
        
        PutItemResponse mockResponse = PutItemResponse.builder().build();
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(mockResponse);

        // When
        assertDoesNotThrow(() -> dynamoDbService.saveFormDefinition(eventId, formData));

        // Then
        verify(mockDynamoDbClient).putItem(argThat(request -> {
            PutItemRequest putRequest = (PutItemRequest) request;
            return putRequest.tableName().equals("event-form-definitions") &&
                   putRequest.item().get("eventId").s().equals(eventId) &&
                   putRequest.item().get("formData").s().equals(formData);
        }));
    }

    @Test
    @DisplayName("Should get existing form definition successfully")
    void shouldGetExistingFormDefinitionSuccessfully() {
        // Given
        String eventId = "event-123";
        String expectedFormData = "{\"fields\":[{\"type\":\"text\",\"label\":\"Name\"}]}";
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventId", AttributeValue.builder().s(eventId).build());
        item.put("formData", AttributeValue.builder().s(expectedFormData).build());
        
        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(item)
                .build();
        
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // When
        String result = dynamoDbService.getFormDefinition(eventId);

        // Then
        assertEquals(expectedFormData, result);
        verify(mockDynamoDbClient).getItem(argThat(request -> {
            GetItemRequest getRequest = (GetItemRequest) request;
            return getRequest.tableName().equals("event-form-definitions") &&
                   getRequest.key().get("eventId").s().equals(eventId);
        }));
    }

    @Test
    @DisplayName("Should return null when form definition does not exist")
    void shouldReturnNullWhenFormDefinitionDoesNotExist() {
        // Given
        String eventId = "event-456";
        
        GetItemResponse mockResponse = GetItemResponse.builder()
                .build(); // Empty response with no item
        
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // When
        String result = dynamoDbService.getFormDefinition(eventId);

        // Then
        assertNull(result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }

    @Test
    @DisplayName("Should handle DynamoDB exceptions gracefully")
    void shouldHandleDynamoDBExceptionsGracefully() {
        // Given
        String eventId = "event-123";
        String formData = "{\"fields\":[]}";
        
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(DynamoDbException.builder()
                        .message("DynamoDB error")
                        .build());

        // When & Then
        DynamoDbException exception = assertThrows(DynamoDbException.class, () ->
                dynamoDbService.saveFormDefinition(eventId, formData)
        );
        
        assertEquals("DynamoDB error", exception.getMessage());
        verify(mockDynamoDbClient).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("Should handle empty form data")
    void shouldHandleEmptyFormData() {
        // Given
        String eventId = "event-789";
        String formData = "";
        
        PutItemResponse mockResponse = PutItemResponse.builder().build();
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(mockResponse);

        // When
        assertDoesNotThrow(() -> dynamoDbService.saveFormDefinition(eventId, formData));

        // Then
        verify(mockDynamoDbClient).putItem(argThat(request -> {
            PutItemRequest putRequest = (PutItemRequest) request;
            return putRequest.item().get("formData").s().equals("");
        }));
    }

    @Test
    @DisplayName("Should handle complex form data with special characters")
    void shouldHandleComplexFormDataWithSpecialCharacters() {
        // Given
        String eventId = "event-special";
        String formData = "{\"fields\":[{\"type\":\"text\",\"label\":\"Name with 'quotes' & \\\"escapes\\\"\"}]}";
        
        PutItemResponse mockResponse = PutItemResponse.builder().build();
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(mockResponse);

        // When
        assertDoesNotThrow(() -> dynamoDbService.saveFormDefinition(eventId, formData));

        // Then
        verify(mockDynamoDbClient).putItem(argThat(request -> {
            PutItemRequest putRequest = (PutItemRequest) request;
            return putRequest.item().get("formData").s().equals(formData);
        }));
    }

    @Test
    @DisplayName("Should retrieve form data with special characters")
    void shouldRetrieveFormDataWithSpecialCharacters() {
        // Given
        String eventId = "event-special";
        String expectedFormData = "{\"fields\":[{\"type\":\"email\",\"validation\":{\"required\":true}}]}";
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventId", AttributeValue.builder().s(eventId).build());
        item.put("formData", AttributeValue.builder().s(expectedFormData).build());
        
        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(item)
                .build();
        
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // When
        String result = dynamoDbService.getFormDefinition(eventId);

        // Then
        assertEquals(expectedFormData, result);
    }
}