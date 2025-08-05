package com.eventr.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
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
import javax.sql.DataSource

/**
 * Development configuration that uses Testcontainers to provide backing services
 * This allows for a consistent environment between development and testing
 */
@Configuration
@Profile("dev")
class DevConfig {
    
    companion object {
        private val postgreSQLContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:14.1")
            .withDatabaseName("eventr_dev")
            .withUsername("dev")
            .withPassword("dev")
        
        private val localStackContainer: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:2.3.0"))
            .withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.DYNAMODB)
        
        init {
            // Start containers
            postgreSQLContainer.start()
            localStackContainer.start()
            
            // Create DynamoDB table for form definitions
            createDynamoDbTable()
            
            // Register JVM shutdown hook
            Runtime.getRuntime().addShutdownHook(Thread {
                postgreSQLContainer.stop()
                localStackContainer.stop()
            })
        }
        
        private fun createDynamoDbTable() {
            val dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.accessKey, localStackContainer.secretKey)
                    )
                )
                .region(Region.of(localStackContainer.region))
                .build()
            
            try {
                // First, try to delete the table if it exists (to ensure a clean state)
                try {
                    dynamoDbClient.deleteTable { it.tableName("event-form-definitions") }
                    // Wait for table to be deleted
                    var tableDeleted = false
                    var attempts = 0
                    while (!tableDeleted && attempts < 10) {
                        try {
                            dynamoDbClient.describeTable { it.tableName("event-form-definitions") }
                            Thread.sleep(500) // Wait before checking again
                            attempts++
                        } catch (e: Exception) {
                            // Table doesn't exist anymore
                            tableDeleted = true
                        }
                    }
                } catch (e: Exception) {
                    // Table doesn't exist, which is fine
                    println("Table doesn't exist yet or couldn't be deleted: ${e.message}")
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
                println("Error creating DynamoDB table: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    @Bean
    @Primary
    fun dataSource(): DataSource {
        return DriverManagerDataSource().apply {
            setDriverClassName("org.postgresql.Driver")
            url = postgreSQLContainer.jdbcUrl
            username = postgreSQLContainer.username
            password = postgreSQLContainer.password
        }
    }
    
    @Bean
    @Primary
    fun s3Client(): S3Client {
        return S3Client.builder()
            .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(localStackContainer.accessKey, localStackContainer.secretKey)
                )
            )
            .region(Region.of(localStackContainer.region))
            .build()
    }
    
    @Bean
    @Primary
    fun dynamoDbClient(): DynamoDbClient {
        return DynamoDbClient.builder()
            .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(localStackContainer.accessKey, localStackContainer.secretKey)
                )
            )
            .region(Region.of(localStackContainer.region))
            .build()
    }
}
