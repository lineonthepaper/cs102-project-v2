#!/bin/bash
# Run the backend with proper environment variables

# Get the project root directory (parent of scripts)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

# Load env vars from env file (strip leading/trailing spaces)
while IFS='=' read -r key value; do
  # Skip comments and empty lines
  [[ "$key" =~ ^[[:space:]]*# ]] && continue
  [[ -z "$key" ]] && continue
  # Trim whitespace using parameter expansion
  key="${key#"${key%%[![:space:]]*}"}"    # remove leading whitespace
  key="${key%"${key##*[![:space:]]}"}"    # remove trailing whitespace
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  [[ -z "$key" ]] && continue
  export "$key=$value"
done < ./env

# Map Supabase DB vars to Spring Boot expected vars
export DATABASE_URL="jdbc:postgresql://${SUPABASE_DB_HOST}:${SUPABASE_DB_PORT}/${SUPABASE_DB_NAME}?sslmode=require"
export DATABASE_USERNAME="${SUPABASE_DB_USERNAME}"
export DATABASE_PASSWORD="${SUPABASE_DB_PASSWORD}"

# Map Supabase API vars
export SUPABASE_URL="${APP_SUPABASE_URL}"
export SUPABASE_SERVICE_ROLE_KEY="${APP_SUPABASE_SERVICE_KEY}"

echo "============================================"
echo "Environment variables:"
echo "DATABASE_URL=${DATABASE_URL}"
echo "DATABASE_USERNAME=${DATABASE_USERNAME}"
echo "SUPABASE_URL=${SUPABASE_URL}"
echo "============================================"
echo ""

cd backend
./gradlew bootRun
