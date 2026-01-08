# ========================================
# Razorpay API Keys Setup - PERMANENT (One-Time Setup)
# ========================================
# 
# This script sets Razorpay API keys as PERMANENT environment variables.
# Run this ONCE and the keys will be available in all future sessions.
# You won't need to run this script again (unless you want to change the keys).
#
# To use:
#   1. Open this file in a text editor
#   2. Replace YOUR_KEY_ID_HERE with your actual Razorpay Key ID (line 28)
#   3. Replace YOUR_KEY_SECRET_HERE with your actual Razorpay Key Secret (line 29)
#   4. Save the file
#   5. Right-click this file -> Run with PowerShell
#      OR open PowerShell as Administrator and run: .\setup-razorpay-keys-permanent.ps1
#
# After running this script ONCE, you can start your application normally from IDE or command line.
# No need to run this script again!
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Razorpay API Keys - PERMANENT Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ========================================
# SET YOUR RAZORPAY API KEYS HERE
# ========================================
# Replace the values below with your actual keys from https://dashboard.razorpay.com/
$RAZORPAY_KEY_ID = "rzp_live_RyeLO5Rr01KCrF"
$RAZORPAY_KEY_SECRET = "2cLoC4MnppY7eRKJRxLrKLUA"

# Optional: Webhook Secret (recommended for production)
$RAZORPAY_WEBHOOK_SECRET = "MahalLiveSecure2026"

# ========================================
# Validate that keys were set (check for placeholder)
# ========================================
if ($RAZORPAY_KEY_ID -eq "YOUR_KEY_ID_HERE") {
    Write-Host "[ERROR] Please edit this script and replace YOUR_KEY_ID_HERE with your actual Razorpay Key ID" -ForegroundColor Red
    Write-Host ""
    Write-Host "Get your keys from: https://dashboard.razorpay.com/ -> Settings -> API Keys" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

if ($RAZORPAY_KEY_SECRET -eq "YOUR_KEY_SECRET_HERE") {
    Write-Host "[ERROR] Please edit this script and replace YOUR_KEY_SECRET_HERE with your actual Razorpay Key Secret" -ForegroundColor Red
    Write-Host ""
    Write-Host "Get your keys from: https://dashboard.razorpay.com/ -> Settings -> API Keys" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# ========================================
# Set environment variables PERMANENTLY
# ========================================
Write-Host "Setting environment variables permanently..." -ForegroundColor Yellow

try {
    # Set for current user (permanent - available in all future sessions)
    [System.Environment]::SetEnvironmentVariable("RAZORPAY_KEY_ID", $RAZORPAY_KEY_ID, [System.EnvironmentVariableTarget]::User)
    [System.Environment]::SetEnvironmentVariable("RAZORPAY_KEY_SECRET", $RAZORPAY_KEY_SECRET, [System.EnvironmentVariableTarget]::User)
    
    if ($RAZORPAY_WEBHOOK_SECRET -and $RAZORPAY_WEBHOOK_SECRET -ne "YOUR_WEBHOOK_SECRET_HERE") {
        [System.Environment]::SetEnvironmentVariable("RAZORPAY_WEBHOOK_SECRET", $RAZORPAY_WEBHOOK_SECRET, [System.EnvironmentVariableTarget]::User)
        $env:RAZORPAY_WEBHOOK_SECRET = $RAZORPAY_WEBHOOK_SECRET
    }
    
    # Also set for current session (so they're available immediately)
    $env:RAZORPAY_KEY_ID = $RAZORPAY_KEY_ID
    $env:RAZORPAY_KEY_SECRET = $RAZORPAY_KEY_SECRET
    
    Write-Host "[SUCCESS] Environment variables set PERMANENTLY!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Key ID: $RAZORPAY_KEY_ID" -ForegroundColor Cyan
    Write-Host "Key Secret: **************** (hidden for security)" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Important Notes:" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "[OK] Variables are now set PERMANENTLY for your user account" -ForegroundColor Green
    Write-Host "[OK] They will be available in ALL future PowerShell/Command Prompt windows" -ForegroundColor Green
    Write-Host "[OK] You can now start your Spring Boot application from IDE or command line" -ForegroundColor Green
    Write-Host ""
    Write-Host "[NOTE] If you're using IntelliJ IDEA or another IDE:" -ForegroundColor Yellow
    Write-Host "       You may need to RESTART the IDE for it to pick up the new environment variables" -ForegroundColor Yellow
    Write-Host "       OR set them in the IDE's Run Configuration (check IDE_SETUP_INSTRUCTIONS.md)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "[OK] No need to run this script again unless you want to change the keys!" -ForegroundColor Green
    Write-Host ""
}
catch {
    Write-Host "[ERROR] Failed to set environment variables: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Try running PowerShell as Administrator:" -ForegroundColor Yellow
    Write-Host "  1. Right-click PowerShell" -ForegroundColor Yellow
    Write-Host "  2. Select 'Run as Administrator'" -ForegroundColor Yellow
    Write-Host "  3. Navigate to this folder and run the script again" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Read-Host "Press Enter to exit"

