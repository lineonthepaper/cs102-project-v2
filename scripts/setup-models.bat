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

REM Check and download facenet.onnx (ArcFace ResNet100)
set "MODEL_FILE=%MODELS_DIR%\facenet.onnx"
set "MODEL_URL=https://github.com/onnx/models/raw/main/validated/vision/body_analysis/arcface/model/arcfaceresnet100-8.onnx"
set "EXPECTED_SIZE_MB=249"
set "DOWNLOAD_NEEDED=0"

if exist "%MODEL_FILE%" (
    REM Check file size (rough check - Windows doesn't have easy MB check)
    for %%A in ("%MODEL_FILE%") do set "FILE_SIZE=%%~zA"
    set /a FILE_SIZE_MB=!FILE_SIZE! / 1048576
    
    if !FILE_SIZE_MB! geq 200 (
        echo [OK] ArcFace model found (!FILE_SIZE_MB! MB^)
    ) else (
        echo [WARNING] Model file exists but seems incomplete (!FILE_SIZE_MB! MB, expected ~%EXPECTED_SIZE_MB% MB^)
        echo Downloading fresh copy...
        del /f "%MODEL_FILE%" 2>nul
        set "DOWNLOAD_NEEDED=1"
    )
) else (
    echo [WARNING] ArcFace model not found
    set "DOWNLOAD_NEEDED=1"
)

if !DOWNLOAD_NEEDED! equ 1 (
    echo Downloading ArcFace ResNet100 model (~%EXPECTED_SIZE_MB% MB^)...
    echo This may take a few minutes depending on your internet connection.
    echo.
    
    REM Try PowerShell download (available on Windows 7+)
    powershell -Command "try { $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '%MODEL_URL%' -OutFile '%MODEL_FILE%' -UseBasicParsing; if (Test-Path '%MODEL_FILE%') { $size = (Get-Item '%MODEL_FILE%').Length / 1MB; if ($size -ge 200) { Write-Host '[OK] Successfully downloaded ArcFace model (' -NoNewline; Write-Host ([math]::Round($size, 1)) -NoNewline; Write-Host ' MB)' } else { Write-Host '[ERROR] Download seems incomplete. Please run this script again.' -ForegroundColor Red; Remove-Item '%MODEL_FILE%' -ErrorAction SilentlyContinue; exit 1 } } else { Write-Host '[ERROR] Download failed.' -ForegroundColor Red; exit 1 } } catch { Write-Host '[ERROR] Download failed: ' -NoNewline -ForegroundColor Red; Write-Host $_.Exception.Message; exit 1 }"
    
    if errorlevel 1 (
        echo.
        echo [ERROR] Download failed. Please check your internet connection and try again.
        echo.
        echo Manual download:
        echo   1. Visit: %MODEL_URL%
        echo   2. Download the file
        echo   3. Save it as: %MODEL_FILE%
        exit /b 1
    )
)

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

