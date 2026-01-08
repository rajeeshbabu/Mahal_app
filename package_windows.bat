@echo off
setlocal

echo ==================================================
echo  Mahal App Packaging Script
echo ==================================================

REM Check Requirements
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found in PATH.
    exit /b 1
)

mvn -version >nul 2>&1
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
set INPUT_DIR=dist_input
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"

REM Copy Dependencies and Main Jar
copy "supabase.properties" "%INPUT_DIR%\"
copy "frontend\target\libs\*.jar" "%INPUT_DIR%\"
copy "frontend\target\mahal-frontend-1.0.0.jar" "%INPUT_DIR%\"

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

REM Note: win-console is useful for debugging (shows stdout). Change to win-dir-chooser or remove for production.
REM Using --win-console for initial testing so we can see Spring Boot logs.
jpackage ^
  --type app-image ^
  --input %INPUT_DIR% ^
  --name %APP_NAME% ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --app-version %APP_VERSION% ^
  --vendor %VENDOR% ^
  --dest output ^
  --win-console

if %errorlevel% neq 0 (
    echo [ERROR] jpackage failed.
    exit /b 1
)

echo.
echo ==================================================
echo  SUCCESS! App Image created in output/%APP_NAME%
echo ==================================================
echo To test, run: output\%APP_NAME%\%APP_NAME%.exe
echo.
pause
