#!/bin/bash
# Build script for appointment-service (skips tests)

echo "Building appointment-service..."
mvn clean install -DskipTests

echo ""
echo "Build complete! Now you can run:"
echo "mvn spring-boot:run"
