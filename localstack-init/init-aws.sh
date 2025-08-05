#!/bin/bash
set -e

echo "Initializing LocalStack AWS resources..."

# Create S3 bucket
echo "Creating S3 bucket: eventr-dev"
awslocal s3 mb s3://eventr-dev

# Create DynamoDB table
echo "Creating DynamoDB table: eventr-dev-table"
awslocal dynamodb create-table \
    --table-name eventr-dev-table \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

echo "AWS resources initialized successfully!"
