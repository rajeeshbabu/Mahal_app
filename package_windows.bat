@echo off
setlocal

echo ==================================================
echo  Mahal App Packaging Script
echo ==================================================

REM Check Requirements
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java not found in PATH.
    exit /b 1
)

where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found in PATH.
    exit /b 1
)

echo [1/4] Building Backend...
cd backend
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Backend build failed.
    cd ..
    exit /b 1
)
cd ..

echo [2/4] Building Frontend...
cd frontend
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Frontend build failed.
    cd ..
    exit /b 1
)
cd ..

echo [3/4] Preparing Input Directory...
REM Clear existing files to avoid Access Denied errors
taskkill /F /IM MahalApp.exe /T >nul 2>&1
taskkill /F /IM MahalApp-1.0.0.exe /T >nul 2>&1
taskkill /F /IM MahalApp-1.0.0.exe /T >nul 2>&1

set INPUT_DIR=dist_input
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"

if exist "output\MahalApp-1.0.0.exe" del /F /Q "output\MahalApp-1.0.0.exe"
if exist "output\MahalApp-1.0.0.msi" del /F /Q "output\MahalApp-1.0.0.msi"
if exist "output\MahalApp-1.0.0.exe" del /F /Q "output\MahalApp-1.0.0.exe"
if exist "output\MahalApp-1.0.0.msi" del /F /Q "output\MahalApp-1.0.0.msi"

REM Copy Dependencies, Main Jar, and Icon
copy "frontend\supabase.properties" "%INPUT_DIR%\"
copy "frontend\target\libs\*.jar" "%INPUT_DIR%\"
copy "frontend\target\mahal-frontend-1.0.0.jar" "%INPUT_DIR%\"
copy "frontend\Image\icon.ico" "%INPUT_DIR%\"

REM Create a default config file in valid location if needed
REM But for jpackage, we might want to just let the user handle it.
REM For now we rely on Env vars or internal defaults.

echo [4/4] Running jpackage...
REM Define Variables
set APP_NAME=MahalApp
set MAIN_JAR=mahal-frontend-1.0.0.jar
set MAIN_CLASS=com.mahal.MahalApplication
set APP_VERSION=1.0.0
set VENDOR="Mahal Team"

REM Step 4a: Create App Image (for local use)
jpackage ^
  --type app-image ^
  --input %INPUT_DIR% ^
  --name %APP_NAME% ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --app-version %APP_VERSION% ^
  --vendor %VENDOR% ^
  --dest output ^
  --icon %INPUT_DIR%\icon.ico ^
  --win-console

REM Step 4b: Create Standalone EXE Installer for distribution
REM Note: This requires WiX Toolset to be installed on the system
echo Creating EXE Installer...
jpackage ^
  --type exe ^
  --input %INPUT_DIR% ^
  --name %APP_NAME% ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --app-version %APP_VERSION% ^
  --vendor %VENDOR% ^
  --dest output ^
  --icon %INPUT_DIR%\icon.ico ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --win-console

if %errorlevel% neq 0 (
    echo [WARNING] EXE Installation failed. Ensure WiX Toolset is installed: https://wixtoolset.org/releases/
)

echo.
echo ==================================================
echo  SUCCESS! App Image created in output/%APP_NAME%
echo ==================================================
echo To test, run: output\%APP_NAME%\%APP_NAME%.exe
echo.
