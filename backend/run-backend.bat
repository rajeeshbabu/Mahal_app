@echo off
echo Starting Mahal Backend Server...
echo.

REM Check if Maven is available
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed or not in PATH.
    echo.
    echo Please install Maven or add it to your PATH.
    echo Download from: https://maven.apache.org/download.cgi
    echo.
    pause
    exit /b 1
)

REM Check if pom.xml exists
if not exist pom.xml (
    echo ERROR: pom.xml not found in backend directory.
    echo.
    pause
    exit /b 1
)

echo Building and starting Spring Boot application...
echo.
mvn spring-boot:run

pause

