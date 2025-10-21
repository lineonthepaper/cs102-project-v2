#!/bin/bash
# Run the frontend with proper environment variables

# Get the project root directory (parent of scripts)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

# Load env vars from env file (strip leading/trailing spaces)
while IFS='=' read -r key value; do
  [[ "$key" =~ ^[[:space:]]*# ]] && continue
  [[ -z "$key" ]] && continue
  key="${key#"${key%%[![:space:]]*}"}"
  key="${key%"${key##*[![:space:]]}"}"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  [[ -z "$key" ]] && continue
  export "$key=$value"
done < ./env

# Map to Vite expected vars
export VITE_SUPABASE_URL="${APP_SUPABASE_URL:-$SUPABASE_URL}"
export VITE_SUPABASE_ANON_KEY="${APP_SUPABASE_KEY}"
export VITE_API_BASE_URL="http://localhost:8080"

echo "============================================"
echo "Frontend environment:"
echo "VITE_SUPABASE_URL=${VITE_SUPABASE_URL}"
echo "VITE_API_BASE_URL=${VITE_API_BASE_URL}"
echo "============================================"
echo ""

cd frontend
npm run dev
