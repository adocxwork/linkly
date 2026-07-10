#!/bin/bash

echo "🚀 Starting Linkly Services..."

# Kill child processes if the script is terminated
trap 'kill 0' SIGINT

# Start the Spring Boot backend in the background
echo "☕ Starting Spring Boot backend..."
./mvnw spring-boot:run &
BACKEND_PID=$!

# Wait a brief moment to let Maven start up
sleep 2

# Start the React frontend in the background
echo "⚛️ Starting React frontend..."
cd frontend || exit
npm run dev &
FRONTEND_PID=$!

echo "✅ Both services are starting up!"
echo "   - Backend: http://localhost:8080"
echo "   - Frontend: http://localhost:5173"
echo "Press Ctrl+C to stop both servers."

# Wait indefinitely until interrupted
wait
