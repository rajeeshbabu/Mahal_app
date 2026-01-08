@echo off
echo ========================================
echo Fixing VS Code Package Error
echo ========================================
echo.

cd /d "%~dp0"

echo Step 1: Closing VS Code workspace files that might interfere...
echo (You may need to close VS Code manually if files are locked)
timeout /t 2 /nobreak >nul

echo.
echo Step 2: Removing VS Code Java extension cache...
if exist "%APPDATA%\Code\User\workspaceStorage" (
    echo Workspace cache found (may contain multiple projects)
    echo Note: This would delete ALL VS Code workspace caches
    echo Skipping for safety - manual cleanup may be needed
)

echo.
echo Step 3: Regenerating Maven project files...
set "PATH=%USERPROFILE%\maven\bin;%PATH%"
call mvn clean compile -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven compilation failed
    pause
    exit /b 1
)

echo.
echo Step 4: Regenerating Eclipse project files...
call mvn eclipse:eclipse -q

echo.
echo ========================================
echo Manual Steps Required:
echo ========================================
echo.
echo 1. Close VS Code/Cursor completely
echo.
echo 2. Delete these folders (if they exist):
echo    - %APPDATA%\Code\Cache
echo    - %APPDATA%\Code\CachedExtensions
echo    OR for Cursor:
echo    - %APPDATA%\Cursor\Cache
echo    - %APPDATA%\Cursor\CachedExtensions
echo.
echo 3. In the backend folder, delete (if exists):
echo    - .settings folder
echo    - .metadata folder (if exists)
echo.
echo 4. Reopen VS Code/Cursor in the backend folder
echo.
echo 5. When prompted, allow Java extension to configure workspace
echo.
echo 6. Wait for Java extension to finish indexing (check status bar)
echo.
echo ========================================
echo.
pause


