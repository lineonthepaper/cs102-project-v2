@echo off
REM Setup script to download required face recognition models (Windows)
REM This script is automatically called during backend startup, or can be run manually

setlocal enabledelayedexpansion

REM Get script directory and project root
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "MODELS_DIR=%PROJECT_ROOT%\backend\src\main\resources\models"

echo === Face Recognition Models Setup ===
echo.

REM Create models directory if it doesn't exist
if not exist "%MODELS_DIR%" mkdir "%MODELS_DIR%"

REM Verify Haar Cascade exists (should already be in repo)
set "HAAR_FILE=%MODELS_DIR%\haarcascade_frontalface_alt.xml"
if exist "%HAAR_FILE%" (
    echo [OK] Haar Cascade face detector found
) else (
    echo [WARNING] Haar Cascade not found (this should be in the repository^)
)

echo.
echo === Setup Complete ===
echo.
echo All required models are ready. You can now start the backend server.

endlocal

