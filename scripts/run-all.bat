@echo off
REM Run both backend and frontend

cd /d "%~dp0"

echo Starting backend and frontend...
echo.
echo Press Ctrl+C to stop both services
echo.

REM Start backend in new window
start "Backend" cmd /k "run-backend.bat"

REM Wait a few seconds for backend to start
timeout /t 5 /nobreak

REM Start frontend in new window
start "Frontend" cmd /k "run-frontend.bat"

echo.
echo Both services started in separate windows
echo Backend: http://localhost:8080
echo Frontend: http://localhost:5173
echo.
echo Close the terminal windows to stop the services
pause

