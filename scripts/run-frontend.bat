@echo off
REM Run the frontend with proper environment variables

REM Get the project root directory (parent of scripts)
set "PROJECT_ROOT=%~dp0\.."
cd /d "%PROJECT_ROOT%"

REM Load env vars from env file - set all non-comment variables
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("env") do (
    set "%%a=%%b"
)

REM Map to Vite expected vars
if not defined VITE_SUPABASE_URL set "VITE_SUPABASE_URL=%APP_SUPABASE_URL%"
if defined SUPABASE_URL if not defined VITE_SUPABASE_URL set "VITE_SUPABASE_URL=%SUPABASE_URL%"
set "VITE_SUPABASE_ANON_KEY=%APP_SUPABASE_KEY%"
set "VITE_API_BASE_URL=http://localhost:8080"

echo ============================================
echo Frontend environment:
echo VITE_SUPABASE_URL=%VITE_SUPABASE_URL%
echo VITE_API_BASE_URL=%VITE_API_BASE_URL%
echo ============================================
echo.

cd /d "%PROJECT_ROOT%\frontend"
echo Installing dependencies...
call npm install
echo Starting frontend...
call npm run dev
