@echo off
REM ========================================
REM Razorpay API Keys Setup Script (Windows)
REM ========================================
REM 
REM This script sets Razorpay API keys as environment variables for the current session.
REM After running this script, start your Spring Boot application in the SAME command prompt.
REM
REM To use:
REM   1. Open this file in a text editor
REM   2. Replace YOUR_KEY_ID_HERE with your actual Razorpay Key ID
REM   3. Replace YOUR_KEY_SECRET_HERE with your actual Razorpay Key Secret
REM   4. Save the file
REM   5. Double-click this file or run it from command prompt
REM   6. In the same command prompt window, start your Spring Boot application
REM
REM ========================================

echo ========================================
echo Razorpay API Keys Setup
echo ========================================
echo.

REM ========================================
REM SET YOUR RAZORPAY API KEYS HERE
REM ========================================
REM Replace the values below with your actual keys from https://dashboard.razorpay.com/
set RAZORPAY_KEY_ID=rzp_live_RyeLO5Rr01KCrF
set RAZORPAY_KEY_SECRET=2cLoC4MnppY7eRKJRxLrKLUA

REM Optional: Webhook Secret (recommended for production)
REM set RAZORPAY_WEBHOOK_SECRET=YOUR_WEBHOOK_SECRET_HERE

REM ========================================
REM Validate that keys were set (check for placeholder)
REM ========================================
if "%RAZORPAY_KEY_ID%"=="YOUR_KEY_ID_HERE" (
    echo [ERROR] Please edit this script and replace YOUR_KEY_ID_HERE with your actual Razorpay Key ID
    echo.
    echo Get your keys from: https://dashboard.razorpay.com/ -^> Settings -^> API Keys
    echo.
    pause
    exit /b 1
)

if "%RAZORPAY_KEY_SECRET%"=="YOUR_KEY_SECRET_HERE" (
    echo [ERROR] Please edit this script and replace YOUR_KEY_SECRET_HERE with your actual Razorpay Key Secret
    echo.
    echo Get your keys from: https://dashboard.razorpay.com/ -^> Settings -^> API Keys
    echo.
    pause
    exit /b 1
)

REM ========================================
REM Set environment variables for current session
REM ========================================
setx RAZORPAY_KEY_ID "%RAZORPAY_KEY_ID%" >nul 2>&1
setx RAZORPAY_KEY_SECRET "%RAZORPAY_KEY_SECRET%" >nul 2>&1

REM Also set in current session (setx doesn't affect current session)
set RAZORPAY_KEY_ID=%RAZORPAY_KEY_ID%
set RAZORPAY_KEY_SECRET=%RAZORPAY_KEY_SECRET%

echo [SUCCESS] Razorpay API keys have been set for this session
echo.
echo Key ID: %RAZORPAY_KEY_ID%
echo Key Secret: **************** (hidden for security)
echo.
echo Note: To use these keys, run your Spring Boot application in THIS command prompt window.
echo       For permanent setup, environment variables were also saved to your user profile.
echo.
echo ========================================
echo Next Steps:
echo ========================================
echo 1. Keep this command prompt window open
echo 2. Navigate to your backend directory (if not already there)
echo 3. Run: mvn spring-boot:run
echo    OR: java -jar target/your-app.jar
echo.
pause

