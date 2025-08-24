#!/bin/bash

# Eventr Development Startup Script with Webhook Client
# This script starts all services needed for local development including webhook testing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

# Check if required tools are installed
command -v java >/dev/null 2>&1 || { print_error "Java is required but not installed. Please install Java 21+"; exit 1; }
command -v mvn >/dev/null 2>&1 || command -v ./mvnw >/dev/null 2>&1 || { print_error "Maven is required but not installed."; exit 1; }
command -v node >/dev/null 2>&1 || { print_error "Node.js is required but not installed. Please install Node.js 18+"; exit 1; }
command -v npm >/dev/null 2>&1 || { print_error "npm is required but not installed."; exit 1; }

print_status "🚀 Starting Eventr Development Environment with Webhook Client..."

# Stop any existing containers
print_status "Stopping any existing services..."
docker-compose -f docker-compose.yml down > /dev/null 2>&1 || true
docker-compose -f docker-compose.dev.yml down > /dev/null 2>&1 || true

# Start infrastructure services
print_status "Starting infrastructure services (PostgreSQL, Redis, LocalStack, MailHog)..."
docker-compose -f docker-compose.yml up -d

# Wait for services to be healthy
print_status "Waiting for services to be ready..."
sleep 10

# Check service health
print_status "Checking service health..."
for service in postgres redis; do
    timeout=30
    while [ $timeout -gt 0 ]; do
        if docker-compose -f docker-compose.yml exec $service echo "Service ready" > /dev/null 2>&1; then
            print_success "$service is ready"
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done
    
    if [ $timeout -le 0 ]; then
        print_warning "$service may not be ready yet, but continuing..."
    fi
done

# Install webhook client dependencies if needed
if [ ! -d "webhook-client/node_modules" ]; then
    print_status "Installing webhook client dependencies..."
    cd webhook-client
    npm install
    cd ..
fi

# Install frontend dependencies if needed
if [ ! -d "frontend/node_modules" ]; then
    print_status "Installing frontend dependencies..."
    cd frontend
    npm install
    cd ..
fi

print_success "Infrastructure services are running!"

# Show service URLs
echo ""
print_status "📋 Service URLs:"
echo "  🗄️  PostgreSQL: jdbc:postgresql://localhost:5432/eventr"
echo "  🗂️  Redis: redis://localhost:6379"
echo "  📧 MailHog UI: http://localhost:8025"
echo "  ☁️  LocalStack: http://localhost:4566"
echo ""

# Function to start a service in a new terminal
start_service_in_terminal() {
    local service_name="$1"
    local command="$2"
    local port="$3"
    
    if command -v gnome-terminal > /dev/null; then
        gnome-terminal --title="$service_name" -- bash -c "$command; read -p 'Press Enter to close...'"
    elif command -v osascript > /dev/null; then
        # macOS Terminal
        osascript -e "tell application \"Terminal\" to do script \"cd '$(pwd)' && echo 'Starting $service_name...' && $command\""
    elif command -v cmd > /dev/null; then
        # Windows
        start cmd /k "cd /d %cd% && echo Starting %service_name%... && %command%"
    else
        print_warning "Could not detect terminal. Please run the following commands manually in separate terminals:"
        echo "1. $command"
        return 1
    fi
    
    return 0
}

# Ask user how they want to start the services
echo ""
print_status "Choose how to start the application services:"
echo "1) Automatic - Start all services in new terminal windows (recommended)"
echo "2) Manual - Show commands to run in separate terminals"
echo "3) Single terminal - Start backend only in current terminal"
read -p "Enter your choice (1-3): " choice

case $choice in
    1)
        print_status "Starting services in separate terminals..."
        
        # Start backend
        if start_service_in_terminal "Eventr Backend" "./mvnw spring-boot:run -Dspring.profiles.active=dev" "8080"; then
            print_success "Backend terminal started"
        else
            print_error "Failed to start backend terminal"
        fi
        
        sleep 2
        
        # Start frontend
        if start_service_in_terminal "Eventr Frontend" "cd frontend && npm start" "3001"; then
            print_success "Frontend terminal started"
        else
            print_error "Failed to start frontend terminal"
        fi
        
        sleep 2
        
        # Start webhook client
        if start_service_in_terminal "Webhook Test Client" "cd webhook-client && EVENTR_WEBHOOK_SECRET=dev-secret-key npm start" "3002"; then
            print_success "Webhook client terminal started"
        else
            print_error "Failed to start webhook client terminal"
        fi
        
        echo ""
        print_success "🎉 All services are starting in separate terminals!"
        ;;
        
    2)
        print_status "Run these commands in separate terminals:"
        echo ""
        echo "Terminal 1 (Backend):"
        echo "  ./mvnw spring-boot:run -Dspring.profiles.active=dev"
        echo ""
        echo "Terminal 2 (Frontend):"
        echo "  cd frontend && npm start"
        echo ""
        echo "Terminal 3 (Webhook Test Client):"
        echo "  cd webhook-client && EVENTR_WEBHOOK_SECRET=dev-secret-key npm start"
        echo ""
        ;;
        
    3)
        print_status "Starting backend in current terminal..."
        print_warning "You'll need to start frontend and webhook client manually:"
        echo "  Frontend: cd frontend && npm start"
        echo "  Webhook Client: cd webhook-client && EVENTR_WEBHOOK_SECRET=dev-secret-key npm start"
        echo ""
        ./mvnw spring-boot:run -Dspring.profiles.active=dev
        ;;
        
    *)
        print_error "Invalid choice. Please run the setup again."
        exit 1
        ;;
esac

if [ "$choice" = "1" ] || [ "$choice" = "2" ]; then
    # Wait a moment for services to start
    sleep 5
    
    print_status "🔗 Application URLs (will be available shortly):"
    echo "  🌐 Frontend: http://localhost:3001"
    echo "  🔧 Backend API: http://localhost:8080/api"
    echo "  📖 API Docs: http://localhost:8080/swagger-ui.html"
    echo "  📨 Webhook Test Client: http://localhost:3002"
    echo "  🗃️  H2 Console: http://localhost:8080/h2-console"
    echo ""
    
    print_success "🎯 Development environment is ready!"
    print_status "💡 Tips:"
    echo "  • Configure webhooks to point to http://localhost:3002/webhook"
    echo "  • Use 'dev-secret-key' as your webhook secret for local testing"
    echo "  • Check the webhook client dashboard to monitor webhook deliveries"
    echo "  • Press Ctrl+C in any terminal to stop that service"
    echo ""
    
    print_status "To stop all infrastructure services later, run:"
    echo "  docker-compose -f docker-compose.yml down"
fi