#!/bin/bash

# Run CASCADE DELETE migration
# This script adds ON DELETE CASCADE to foreign keys referencing users table

cd "$(dirname "$0")/.."

# Source environment variables
if [ -f "env" ]; then
    export $(grep -v '^#' env | grep -v '^$' | xargs)
fi

echo "Running CASCADE DELETE migration..."
echo "Database: $DATABASE_URL"

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "ERROR: psql command not found"
    echo "Please install PostgreSQL client tools or run the SQL manually in Supabase SQL Editor"
    echo ""
    echo "SQL file location: database/add-cascade-delete.sql"
    exit 1
fi

# Run migration
psql "$DATABASE_URL" -f database/add-cascade-delete.sql

if [ $? -eq 0 ]; then
    echo "✅ Migration completed successfully!"
    echo ""
    echo "CASCADE DELETE is now enabled for:"
    echo "  - attendance_records (user_id)"
    echo "  - section_enrollments (user_id)"
    echo "  - section_assignments (user_id)"
    echo "  - ta_assignments (user_id)"
    echo ""
    echo "You can now delete students and their related records will be automatically deleted."
else
    echo "❌ Migration failed"
    echo "Please run the SQL manually in Supabase SQL Editor"
    echo "File: database/add-cascade-delete.sql"
    exit 1
fi

