#!/bin/bash
# Starts backend (port 8080) and frontend (port 5173) for local development.
# Backend settings are read from .env (copy .env.example to .env first).

trap 'kill 0' SIGINT

if [ -f .env ]; then
  set -a; . ./.env; set +a
else
  echo "⚠️  No .env file found. Copy .env.example to .env and set JWT_SECRET at least."
fi

echo "☕ Starting Spring Boot backend on http://localhost:8080 ..."
./mvnw spring-boot:run &

echo "⚛️  Starting React frontend on http://localhost:5173 ..."
(cd frontend && npm run dev) &

echo "Press Ctrl+C to stop both."
wait
