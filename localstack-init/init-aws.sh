#!/bin/bash
set -e

echo "Initializing LocalStack AWS resources..."

# Create S3 bucket
echo "Creating S3 bucket: eventr-dev"
awslocal s3 mb s3://eventr-dev

# Create DynamoDB table for event form definitions
echo "Creating DynamoDB table: event-form-definitions"
awslocal dynamodb create-table \
    --table-name event-form-definitions \
    --attribute-definitions AttributeName=eventId,AttributeType=S \
    --key-schema AttributeName=eventId,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

echo "AWS resources initialized successfully!"
