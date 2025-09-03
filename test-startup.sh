#!/bin/bash

echo "Testing application startup..."

# Start the application in the background
./mvnw spring-boot:run -DskipTests &
APP_PID=$!

# Wait for startup (max 45 seconds)
for i in {1..45}; do
    sleep 1
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Application started successfully!"
        kill $APP_PID
        exit 0
    fi
    echo -n "."
done

echo ""
echo "❌ Application failed to start within 45 seconds"
kill $APP_PID
exit 1