# ========================================
# Razorpay API Keys Setup Script (Windows PowerShell)
# ========================================
# 
# ⚠️  NOTE: This script sets variables for CURRENT SESSION ONLY
# 
# For PERMANENT setup (set once, use forever):
#   - Use: setup-razorpay-keys-permanent.ps1 (RECOMMENDED)
#   - OR use GUI method (see PERMANENT_SETUP_WINDOWS.md)
#
# This script is for TEMPORARY setup (variables lost when window closes)
# Run this script, then start your Spring Boot application in the SAME window
#
# To use:
#   1. Open this file in a text editor (Notepad, VS Code, etc.)
#   2. Replace YOUR_KEY_ID_HERE with your actual Razorpay Key ID
#   3. Replace YOUR_KEY_SECRET_HERE with your actual Razorpay Key Secret
#   4. Save the file
#   5. Right-click this file -> Run with PowerShell
#      OR open PowerShell and run: .\setup-razorpay-keys.ps1
#   6. Start your Spring Boot application in the SAME PowerShell window
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Razorpay API Keys Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ========================================
# SET YOUR RAZORPAY API KEYS HERE
# ========================================
# Replace the values below with your actual keys from https://dashboard.razorpay.com/
$env:RAZORPAY_KEY_ID = "rzp_live_RyeLO5Rr01KCrF"
$env:RAZORPAY_KEY_SECRET = "2cLoC4MnppY7eRKJRxLrKLUA"

# Optional: Webhook Secret (recommended for production)
# $env:RAZORPAY_WEBHOOK_SECRET = "YOUR_WEBHOOK_SECRET_HERE"

# ========================================
# Validate that keys were set (check for placeholder)
# ========================================
if ($env:RAZORPAY_KEY_ID -eq "YOUR_KEY_ID_HERE") {
    Write-Host "[ERROR] Please edit this script and replace YOUR_KEY_ID_HERE with your actual Razorpay Key ID" -ForegroundColor Red
    Write-Host ""
    Write-Host "Get your keys from: https://dashboard.razorpay.com/ -> Settings -> API Keys" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

if ($env:RAZORPAY_KEY_SECRET -eq "YOUR_KEY_SECRET_HERE") {
    Write-Host "[ERROR] Please edit this script and replace YOUR_KEY_SECRET_HERE with your actual Razorpay Key Secret" -ForegroundColor Red
    Write-Host ""
    Write-Host "Get your keys from: https://dashboard.razorpay.com/ -> Settings -> API Keys" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# ========================================
# Set environment variables
# ========================================
# Values are already set above, now save them permanently

# For user profile (permanent - available in new sessions)
[System.Environment]::SetEnvironmentVariable("RAZORPAY_KEY_ID", $env:RAZORPAY_KEY_ID, [System.EnvironmentVariableTarget]::User)
[System.Environment]::SetEnvironmentVariable("RAZORPAY_KEY_SECRET", $env:RAZORPAY_KEY_SECRET, [System.EnvironmentVariableTarget]::User)

Write-Host "[SUCCESS] Razorpay API keys have been set!" -ForegroundColor Green
Write-Host ""
Write-Host "Key ID: $env:RAZORPAY_KEY_ID" -ForegroundColor Cyan
Write-Host "Key Secret: **************** (hidden for security)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: Environment variables are set for:" -ForegroundColor Yellow
Write-Host "  - Current PowerShell session (available now)" -ForegroundColor Yellow
Write-Host "  - Your user profile (available in new sessions)" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "1. Keep this PowerShell window open" -ForegroundColor White
Write-Host "2. Navigate to your backend directory:" -ForegroundColor White
Write-Host "   cd backend" -ForegroundColor Gray
Write-Host "3. Run your Spring Boot application:" -ForegroundColor White
Write-Host "   mvn spring-boot:run" -ForegroundColor Gray
Write-Host "   OR" -ForegroundColor Gray
Write-Host "   java -jar target/your-app.jar" -ForegroundColor Gray
Write-Host ""
Read-Host "Press Enter to continue"

