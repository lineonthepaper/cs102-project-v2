@echo off
REM Run the backend with environment variables from env file

cd /d "%~dp0\.."

echo Loading environment variables...

REM Read env file and set variables (skip lines starting with # or empty)
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("env") do (
    if "%%a"=="APP_SUPABASE_URL" set "SUPABASE_URL=%%b"
    if "%%a"=="APP_SUPABASE_KEY" set "SUPABASE_ANON_KEY=%%b"
    if "%%a"=="APP_SUPABASE_SERVICE_KEY" set "SUPABASE_SERVICE_ROLE_KEY=%%b"
    if "%%a"=="SUPABASE_DB_HOST" set "DB_HOST=%%b"
    if "%%a"=="SUPABASE_DB_PORT" set "DB_PORT=%%b"
    if "%%a"=="SUPABASE_DB_NAME" set "DB_NAME=%%b"
    if "%%a"=="SUPABASE_DB_USERNAME" set "DB_USER=%%b"
    if "%%a"=="SUPABASE_DB_PASSWORD" set "DB_PASS=%%b"
    if "%%a"=="JWT_SECRET" set "JWT_SECRET=%%b"
)

REM Extract project ref for pooler username
for /f "tokens=3 delims=:/" %%a in ("%SUPABASE_URL%") do set "PROJECT_REF=%%a"
for /f "tokens=1 delims=." %%a in ("%PROJECT_REF%") do set "PROJECT_REF=%%a"

REM Set database username for pooler
if "%DB_HOST:pooler=%"=="%DB_HOST%" (
    set "DATABASE_USERNAME=%DB_USER%"
) else (
    set "DATABASE_USERNAME=%DB_USER%.%PROJECT_REF%"
)

set "DATABASE_URL=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%?sslmode=require"
set "DATABASE_PASSWORD=%DB_PASS%"

echo ============================================
echo DATABASE_URL=%DATABASE_URL%
echo DATABASE_USERNAME=%DATABASE_USERNAME%
echo SUPABASE_URL=%SUPABASE_URL%
echo ============================================
echo.

cd backend
call gradlew.bat bootRun

