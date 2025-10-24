#!/bin/bash

# Load environment variables from env file
if [ -f "env" ]; then
    set -a
    source env
    set +a
fi

# Navigate to backend directory
cd backend

# Run the Spring Boot application
# Environment variables are now passed via build.gradle.kts bootRun configuration
./gradlew bootRun

