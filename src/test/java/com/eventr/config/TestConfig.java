package com.eventr.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@TestConfiguration
@Profile("test")
public class TestConfig {

    private static final PostgreSQLContainer<?> postgreSQLContainer;
    private static final LocalStackContainer localStackContainer;

    static {
        // Start PostgreSQL container
        postgreSQLContainer = new PostgreSQLContainer<>("postgres:14.1")
                .withDatabaseName("eventr_test")
                .withUsername("test")
                .withPassword("test");
        postgreSQLContainer.start();

        // Start LocalStack container with S3 and DynamoDB
        localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.3.0"))
                .withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.DYNAMODB);
        localStackContainer.start();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgreSQLContainer.getJdbcUrl());
        dataSource.setUsername(postgreSQLContainer.getUsername());
        dataSource.setPassword(postgreSQLContainer.getPassword());
        return dataSource;
    }

    @Bean
    @Primary
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())
                ))
                .region(Region.of(localStackContainer.getRegion()))
                .build();
    }

    @Bean
    @Primary
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())
                ))
                .region(Region.of(localStackContainer.getRegion()))
                .build();
    }
}
