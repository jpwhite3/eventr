package com.eventr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.s3.S3Client
import org.mockito.Mockito

@TestConfiguration
@Profile("test")
class TestConfig {
    
    @Bean
    @Primary
    fun s3Client(): S3Client {
        return Mockito.mock(S3Client::class.java)
    }
    
    @Bean
    @Primary
    fun dynamoDbClient(): DynamoDbClient {
        return Mockito.mock(DynamoDbClient::class.java)
    }
}