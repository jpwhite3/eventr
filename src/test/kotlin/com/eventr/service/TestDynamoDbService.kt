package com.eventr.service

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import java.util.concurrent.ConcurrentHashMap

/**
 * Test implementation of DynamoDbService that uses an in-memory map instead of actual DynamoDB
 * This avoids issues with LocalStack table creation timing and configuration
 */
@Service
@Primary
@Profile("test")
class TestDynamoDbService(dynamoDbClient: DynamoDbClient) : DynamoDbService(dynamoDbClient) {
    
    // In-memory storage for form definitions
    private val formDefinitions = ConcurrentHashMap<String, String>()
    
    override fun saveFormDefinition(eventId: String, formData: String) {
        // Store in memory
        formDefinitions[eventId] = formData
        
        // Also try to store in DynamoDB (but don't fail if it doesn't work)
        try {
            val item = mapOf(
                "eventId" to AttributeValue.builder().s(eventId).build(),
                "formData" to AttributeValue.builder().s(formData).build()
            )
            
            val request = PutItemRequest.builder()
                .tableName("event-form-definitions")
                .item(item)
                .build()
            
            dynamoDbClient.putItem(request)
        } catch (e: Exception) {
            // Log but don't fail the test
            println("Warning: Could not save to DynamoDB: ${e.message}")
        }
    }
    
    override fun getFormDefinition(eventId: String): String? {
        // First try in-memory storage
        val inMemoryData = formDefinitions[eventId]
        if (inMemoryData != null) {
            return inMemoryData
        }
        
        // Fall back to DynamoDB if needed
        try {
            val key = mapOf(
                "eventId" to AttributeValue.builder().s(eventId).build()
            )
            
            val request = GetItemRequest.builder()
                .tableName("event-form-definitions")
                .key(key)
                .build()
            
            val response = dynamoDbClient.getItem(request)
            return if (response.hasItem()) {
                response.item()["formData"]?.s()
            } else {
                null
            }
        } catch (e: Exception) {
            // Log but don't fail the test
            println("Warning: Could not retrieve from DynamoDB: ${e.message}")
            return null
        }
    }
}
