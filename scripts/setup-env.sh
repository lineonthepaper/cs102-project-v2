#!/bin/bash
# Load environment variables from env file and export them

set -a  # automatically export all variables
source /Users/wy/Downloads/CS102\ V2/env
set +a

# Map Supabase DB vars to Spring Boot expected vars
export DATABASE_URL="jdbc:postgresql://${SUPABASE_DB_HOST}:${SUPABASE_DB_PORT}/${SUPABASE_DB_NAME}?sslmode=require"
export DATABASE_USERNAME="${SUPABASE_DB_USERNAME}"
export DATABASE_PASSWORD="${SUPABASE_DB_PASSWORD}"

# Map Supabase API vars
export SUPABASE_URL="${APP_SUPABASE_URL}"
export SUPABASE_SERVICE_ROLE_KEY="${APP_SUPABASE_SERVICE_KEY}"

echo "Environment variables loaded successfully!"
echo "DATABASE_URL=${DATABASE_URL}"
echo "DATABASE_USERNAME=${DATABASE_USERNAME}"
echo "SUPABASE_URL=${SUPABASE_URL}"

