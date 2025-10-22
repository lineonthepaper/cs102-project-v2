@echo off
REM Run the frontend with environment variables from env file

cd /d "%~dp0\.."

echo Loading environment variables...

REM Read env file and set variables
for /f "usebackq tokens=1,* delims==" %%a in ("env") do (
    set "line=%%a"
    if not "!line:~0,1!"=="#" (
        if not "%%a"=="" (
            if "%%a"=="APP_SUPABASE_URL" set "VITE_SUPABASE_URL=%%b"
            if "%%a"=="APP_SUPABASE_KEY" set "VITE_SUPABASE_ANON_KEY=%%b"
        )
    )
)

set "VITE_API_BASE_URL=http://localhost:8080"

echo ============================================
echo VITE_SUPABASE_URL=%VITE_SUPABASE_URL%
echo VITE_API_BASE_URL=%VITE_API_BASE_URL%
echo ============================================
echo.

cd frontend
call npm run dev

