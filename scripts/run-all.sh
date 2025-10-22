#!/bin/bash
# Run both backend and frontend concurrently

# Get the project root directory (parent of scripts)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$PROJECT_ROOT"

echo "Starting backend and frontend..."
echo ""

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "Shutting down..."
    kill 0
}

trap cleanup EXIT

# Start backend
"$SCRIPT_DIR/run-backend.sh" &
BACKEND_PID=$!

# Wait a bit for backend to start
sleep 5

# Start frontend
"$SCRIPT_DIR/run-frontend.sh" &
FRONTEND_PID=$!

# Wait for both processes
wait
