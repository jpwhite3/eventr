package com.eventr.service

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest

@Service
open class DynamoDbService(protected val dynamoDbClient: DynamoDbClient) {
    
    private val tableName = "event-form-definitions"
    
    open fun saveFormDefinition(eventId: String, formData: String) {
        val item = mapOf(
            "eventId" to AttributeValue.builder().s(eventId).build(),
            "formData" to AttributeValue.builder().s(formData).build()
        )
        
        val request = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .build()
        
        dynamoDbClient.putItem(request)
    }
    
    open fun getFormDefinition(eventId: String): String? {
        val key = mapOf(
            "eventId" to AttributeValue.builder().s(eventId).build()
        )
        
        val request = GetItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build()
        
        val response = dynamoDbClient.getItem(request)
        return if (response.hasItem()) {
            response.item()["formData"]?.s()
        } else {
            null
        }
    }
}
