package com.eventr.config.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.datasource.DriverManagerDataSource
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI
import javax.sql.DataSource

/**
 * Development configuration that uses existing Docker Compose services
 * instead of starting new Testcontainers
 */
@Configuration
@Profile("docker-compose-dev")
class DockerComposeDevConfig {
    
    @Value("\${spring.datasource.url:jdbc:postgresql://localhost:5432/eventr}")
    private lateinit var databaseUrl: String
    
    @Value("\${spring.datasource.username:eventr}")
    private lateinit var databaseUsername: String
    
    @Value("\${spring.datasource.password:eventr}")
    private lateinit var databasePassword: String
    
    @Value("\${spring.cloud.aws.s3.endpoint:http://localhost:4566}")
    private lateinit var s3Endpoint: String
    
    @Value("\${spring.cloud.aws.dynamodb.endpoint:http://localhost:4566}")
    private lateinit var dynamoDbEndpoint: String
    
    @Value("\${spring.cloud.aws.region.static:us-east-1}")
    private lateinit var awsRegion: String
    
    @Value("\${spring.cloud.aws.credentials.access-key:test}")
    private lateinit var awsAccessKey: String
    
    @Value("\${spring.cloud.aws.credentials.secret-key:test}")
    private lateinit var awsSecretKey: String
    
    @Bean
    @Primary
    fun dataSource(): DataSource {
        return DriverManagerDataSource().apply {
            setDriverClassName("org.postgresql.Driver")
            url = databaseUrl
            username = databaseUsername
            password = databasePassword
        }
    }
    
    @Bean
    @Primary
    fun s3Client(): S3Client {
        return S3Client.builder()
            .endpointOverride(URI.create(s3Endpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                )
            )
            .region(Region.of(awsRegion))
            .forcePathStyle(true)
            .build()
    }
    
    @Bean
    @Primary
    fun dynamoDbClient(): DynamoDbClient {
        val client = DynamoDbClient.builder()
            .endpointOverride(URI.create(dynamoDbEndpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                )
            )
            .region(Region.of(awsRegion))
            .build()
        
        // Create DynamoDB table if it doesn't exist
        createDynamoDbTable(client)
        
        return client
    }
    
    private fun createDynamoDbTable(dynamoDbClient: DynamoDbClient) {
        try {
            // Check if table exists first
            try {
                dynamoDbClient.describeTable { it.tableName("event-form-definitions") }
                println("DynamoDB table 'event-form-definitions' already exists")
                return
            } catch (e: Exception) {
                // Table doesn't exist, create it
                println("Creating DynamoDB table 'event-form-definitions'...")
            }
            
            // Create the event-form-definitions table
            val request = CreateTableRequest.builder()
                .tableName("event-form-definitions")
                .keySchema(KeySchemaElement.builder()
                    .attributeName("eventId")
                    .keyType(KeyType.HASH)
                    .build())
                .attributeDefinitions(AttributeDefinition.builder()
                    .attributeName("eventId")
                    .attributeType(ScalarAttributeType.S)
                    .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(5)
                    .writeCapacityUnits(5)
                    .build())
                .build()
            
            dynamoDbClient.createTable(request)
            
            // Wait for table to be active
            var tableActive = false
            var attempts = 0
            while (!tableActive && attempts < 20) {
                val tableStatus = dynamoDbClient.describeTable { it.tableName("event-form-definitions") }
                    .table()
                    .tableStatus()
                    .toString()
                
                if (tableStatus == "ACTIVE") {
                    tableActive = true
                } else {
                    Thread.sleep(500) // Wait before checking again
                    attempts++
                }
            }
            
            println("Created DynamoDB table: event-form-definitions (Status: ${if (tableActive) "ACTIVE" else "NOT ACTIVE"})")
        } catch (e: Exception) {
            println("Error managing DynamoDB table: ${e.message}")
        }
    }
}