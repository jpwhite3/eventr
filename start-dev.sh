#!/bin/bash
set -e

echo "Starting development environment with Testcontainers..."

# Start MailHog only (PostgreSQL and LocalStack will be managed by Testcontainers)
echo "Starting MailHog for email testing..."
docker run -d --name eventr-mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog

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

# Cleanup function to kill backend process when script is terminated
cleanup() {
    echo "Shutting down servers..."
    kill $BACKEND_PID
    exit 0
}

# Register the cleanup function to be called on exit
trap cleanup EXIT

# Wait for frontend to finish (this will keep the script running)
wait
