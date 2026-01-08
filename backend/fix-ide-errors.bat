@echo off
echo Fixing IDE errors for Mahal Backend...
echo.

cd /d "%~dp0"

echo Step 1: Setting Maven path...
set "PATH=%USERPROFILE%\maven\bin;%PATH%"

echo Step 2: Downloading Maven dependencies...
call mvn dependency:resolve -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to download dependencies
    pause
    exit /b 1
)

echo Step 3: Compiling project...
call mvn clean compile -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile project
    pause
    exit /b 1
)

echo Step 4: Regenerating Eclipse project files...
call mvn eclipse:eclipse -q

echo.
echo ========================================
echo IDE Fix Complete!
echo ========================================
echo.
echo For VS Code:
echo   1. Press Ctrl+Shift+P
echo   2. Type "Java: Clean Java Language Server Workspace"
echo   3. Select it and restart
echo.
echo For Eclipse:
echo   1. Right-click project
echo   2. Select "Maven" -^> "Update Project..."
echo   3. Click OK
echo.
pause



