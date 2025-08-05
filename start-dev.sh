#!/bin/bash

# Function to clean up resources
cleanup() {
    echo "\nShutting down services..."
    
    # Kill backend process if it exists
    if [ ! -z "$BACKEND_PID" ]; then
        echo "Stopping backend server..."
        kill $BACKEND_PID 2>/dev/null || true
    fi
    
    # Stop and remove MailHog container if it exists
    if docker ps -q --filter "name=eventr-mailhog" | grep -q .; then
        echo "Stopping MailHog container..."
        docker stop eventr-mailhog 2>/dev/null || true
        docker rm eventr-mailhog 2>/dev/null || true
    fi
    
    # Find and kill any Testcontainers that might be running
    echo "Cleaning up any Testcontainers..."
    TESTCONTAINER_IDS=$(docker ps -q --filter "label=org.testcontainers=true" 2>/dev/null || true)
    if [ ! -z "$TESTCONTAINER_IDS" ]; then
        docker stop $TESTCONTAINER_IDS 2>/dev/null || true
        docker rm $TESTCONTAINER_IDS 2>/dev/null || true
    fi
    
    echo "Cleanup complete."
    exit 0
}

# Register the cleanup function for various signals
trap cleanup EXIT INT TERM

echo "Starting development environment with Testcontainers..."

# Check if MailHog is already running
if docker ps -q --filter "name=eventr-mailhog" | grep -q .; then
    echo "MailHog is already running."
else
    echo "Starting MailHog for email testing..."
    docker run -d --name eventr-mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
fi

# Start backend server in the background with dev profile
echo "Starting backend server with Testcontainers..."
./mvnw spring-boot:run -Pbackend -Dspring.profiles.active=dev &
BACKEND_PID=$!

# Wait for backend to start
echo "Waiting for backend server to start..."
sleep 20

# Start frontend server
echo "Starting frontend server..."
cd frontend
npm start

# Wait for frontend to finish (this will keep the script running)
# The cleanup function registered at the beginning will handle shutdown
wait
