@echo off
setlocal enabledelayedexpansion
REM Run the backend with proper environment variables

REM Get the project root directory (parent of scripts)
set "PROJECT_ROOT=%~dp0\.."
cd /d "%PROJECT_ROOT%"

REM Load env vars from env file - set all non-comment variables
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("env") do (
    set "%%a=%%b"
)

REM Map Supabase DB vars to Spring Boot expected vars
REM If using pooler, username must be in format postgres.PROJECT_REF
echo %SUPABASE_DB_HOST% | findstr /i "pooler.supabase.com" >nul
if errorlevel 1 (
    REM Direct connection
    set "DATABASE_USERNAME=!SUPABASE_DB_USERNAME!"
    set "DATABASE_URL=jdbc:postgresql://!SUPABASE_DB_HOST!:!SUPABASE_DB_PORT!/!SUPABASE_DB_NAME!?sslmode=require"
) else (
    REM Pooler connection - extract project ref from Supabase URL
    REM Remove https:// prefix using delayed expansion
    set "TEMP_URL=!APP_SUPABASE_URL:https://=!"
    REM Extract subdomain (everything before first dot)
    for /f "tokens=1 delims=." %%a in ("!TEMP_URL!") do set "PROJECT_REF=%%a"
    set "DATABASE_USERNAME=!SUPABASE_DB_USERNAME!.!PROJECT_REF!"
    set "DATABASE_URL=jdbc:postgresql://!SUPABASE_DB_HOST!:!SUPABASE_DB_PORT!/!SUPABASE_DB_NAME!?sslmode=require&preferQueryMode=simple"
)

set "DATABASE_PASSWORD=!SUPABASE_DB_PASSWORD!"

REM Map Supabase API vars
set "SUPABASE_URL=!APP_SUPABASE_URL!"
set "SUPABASE_SERVICE_ROLE_KEY=!APP_SUPABASE_SERVICE_KEY!"

echo ============================================
echo Environment variables:
echo DATABASE_URL=!DATABASE_URL!
echo DATABASE_USERNAME=!DATABASE_USERNAME!
echo SUPABASE_URL=!SUPABASE_URL!
echo ============================================
echo.

cd /d "%PROJECT_ROOT%\backend"
call gradlew.bat bootRun