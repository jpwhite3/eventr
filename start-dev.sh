#!/bin/bash

# Set process group for proper cleanup
set -m

# Global variables for PIDs
BACKEND_PID=""
FRONTEND_PID=""
FRONTEND_PGID=""

# Function to clean up resources
cleanup() {
    echo -e "\nShutting down services..."
    
    # Kill frontend process and its children
    if [ ! -z "$FRONTEND_PID" ]; then
        echo "Stopping frontend server (PID: $FRONTEND_PID)..."
        # Kill the entire process group to get all child processes
        kill -TERM -$FRONTEND_PGID 2>/dev/null || true
        sleep 2
        # Force kill if still running
        kill -KILL -$FRONTEND_PGID 2>/dev/null || true
    fi
    
    # Kill any remaining npm/node processes for this project
    echo "Cleaning up any remaining frontend processes..."
    pkill -f "react-scripts start" 2>/dev/null || true
    pkill -f "PORT=3002" 2>/dev/null || true
    
    # Kill backend process if it exists
    if [ ! -z "$BACKEND_PID" ]; then
        echo "Stopping backend server (PID: $BACKEND_PID)..."
        kill -TERM $BACKEND_PID 2>/dev/null || true
        sleep 2
        kill -KILL $BACKEND_PID 2>/dev/null || true
    fi
    
    # Kill any remaining Maven/Java processes for this project
    echo "Cleaning up any remaining backend processes..."
    pkill -f "spring-boot:run.*eventr" 2>/dev/null || true
    pkill -f "EventrApplication" 2>/dev/null || true
    
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
    
    # Kill any test processes that might still be running
    echo "Stopping any running test processes..."
    pkill -f "mvn.*test" 2>/dev/null || true
    
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
    if ! docker run -d --name eventr-mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog; then
        echo "Warning: Could not start MailHog container"
    fi
fi

# Start backend server in the background with dev profile (skip tests)
echo "Starting backend server with dev profile (skipping tests)..."
./mvnw spring-boot:run -Pbackend -Dspring.profiles.active=dev -DskipTests &
BACKEND_PID=$!

# Check if backend started successfully
if ! ps -p $BACKEND_PID > /dev/null 2>&1; then
    echo "Error: Backend server failed to start"
    cleanup
fi

# Wait for backend to start
echo "Waiting for backend server to start..."
sleep 15

# Check if backend is still running after startup wait
if ! ps -p $BACKEND_PID > /dev/null 2>&1; then
    echo "Error: Backend server died during startup"
    cleanup
fi

# Start frontend server
echo "Starting frontend server..."
cd frontend || {
    echo "Error: Could not change to frontend directory"
    cleanup
}

# Start npm in background to capture PID and process group
npm start &
FRONTEND_PID=$!
FRONTEND_PGID=$(ps -o pgid= -p $FRONTEND_PID | tr -d ' ')

# Check if frontend started successfully
sleep 3
if ! ps -p $FRONTEND_PID > /dev/null 2>&1; then
    echo "Error: Frontend server failed to start"
    cleanup
fi

echo "Both servers are running:"
echo "  - Backend: http://localhost:8080 (PID: $BACKEND_PID)"
echo "  - Frontend: http://localhost:3002 (PID: $FRONTEND_PID)"
echo "  - MailHog UI: http://localhost:8025"
echo ""
echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
echo "║                        🚀 DEVELOPMENT LOGIN INFO 🚀                            ║"
echo "╠═══════════════════════════════════════════════════════════════════════════════╣"
echo "║                                                                               ║"
echo "║  👤 TEST USERS AVAILABLE FOR LOGIN:                                           ║"
echo "║                                                                               ║"
echo "║  📧 Admin User:                                                               ║"
echo "║     Email:    admin@eventr.dev                                                ║"
echo "║     Password: DevPassword123                                                  ║"
echo "║     Role:     ADMIN                                                           ║"
echo "║                                                                               ║"
echo "║  📧 Organizer User:                                                           ║"
echo "║     Email:    organizer@eventr.dev                                            ║"
echo "║     Password: DevPassword123                                                  ║"
echo "║     Role:     ORGANIZER                                                       ║"
echo "║                                                                               ║"
echo "║  📧 Regular User:                                                             ║"
echo "║     Email:    user@eventr.dev                                                 ║"
echo "║     Password: DevPassword123                                                  ║"
echo "║     Role:     ATTENDEE                                                        ║"
echo "║                                                                               ║"
echo "║  🌐 Login URL: http://localhost:3002/login                                    ║"
echo "║                                                                               ║"
echo "║  ⚠️  These credentials are for DEVELOPMENT ONLY!                              ║"
echo "║     Do not use in production environments.                                    ║"
echo "║                                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
echo ""
echo "Press Ctrl+C to stop all services"

# Wait for either process to finish or be interrupted
wait $FRONTEND_PID
