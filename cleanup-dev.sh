#!/bin/bash

echo "Cleaning up Eventr development environment..."

# Kill any running frontend processes
echo "Stopping frontend processes..."
pkill -f "react-scripts start" 2>/dev/null || true
pkill -f "PORT=3002" 2>/dev/null || true
pkill -f "frontend.*npm start" 2>/dev/null || true

# Kill any running backend processes
echo "Stopping backend processes..."
pkill -f "spring-boot:run.*eventr" 2>/dev/null || true
pkill -f "EventrApplication" 2>/dev/null || true
pkill -f "mvnw.*spring-boot:run" 2>/dev/null || true

# Stop and remove MailHog container
echo "Stopping MailHog container..."
docker stop eventr-mailhog 2>/dev/null || true
docker rm eventr-mailhog 2>/dev/null || true

# Clean up Testcontainers
echo "Cleaning up Testcontainers..."
TESTCONTAINER_IDS=$(docker ps -q --filter "label=org.testcontainers=true" 2>/dev/null || true)
if [ ! -z "$TESTCONTAINER_IDS" ]; then
    docker stop $TESTCONTAINER_IDS 2>/dev/null || true
    docker rm $TESTCONTAINER_IDS 2>/dev/null || true
fi

# Kill any test processes
echo "Stopping test processes..."
pkill -f "mvn.*test" 2>/dev/null || true

echo "Cleanup complete!"