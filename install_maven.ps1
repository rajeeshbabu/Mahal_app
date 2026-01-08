# Maven Installation Script for Windows
Write-Host "Installing Apache Maven..." -ForegroundColor Green

# Set Maven version and installation directory
$MAVEN_VERSION = "3.9.5"
$MAVEN_HOME = "$env:USERPROFILE\maven"
$MAVEN_ZIP = "$env:TEMP\apache-maven-$MAVEN_VERSION-bin.zip"
# Try multiple mirror URLs
$MAVEN_URLS = @(
    "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip",
    "https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip",
    "https://downloads.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip"
)

# Create Maven directory if it doesn't exist
if (-not (Test-Path $MAVEN_HOME)) {
    New-Item -ItemType Directory -Path $MAVEN_HOME | Out-Null
}

# Download Maven
Write-Host "Downloading Maven $MAVEN_VERSION..." -ForegroundColor Yellow
$downloadSuccess = $false
foreach ($url in $MAVEN_URLS) {
    try {
        Write-Host "Trying: $url" -ForegroundColor Gray
        Invoke-WebRequest -Uri $url -OutFile $MAVEN_ZIP -UseBasicParsing
        Write-Host "Download completed!" -ForegroundColor Green
        $downloadSuccess = $true
        break
    } catch {
        Write-Host "Failed: $($_.Exception.Message)" -ForegroundColor Gray
        continue
    }
}

if (-not $downloadSuccess) {
    Write-Host "Error: Could not download Maven from any mirror." -ForegroundColor Red
    Write-Host "Please download manually from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
    exit 1
}

# Extract Maven
Write-Host "Extracting Maven..." -ForegroundColor Yellow
try {
    Expand-Archive -Path $MAVEN_ZIP -DestinationPath $MAVEN_HOME -Force
    $extractedFolder = Get-ChildItem -Path $MAVEN_HOME -Directory | Where-Object { $_.Name -like "apache-maven-*" } | Select-Object -First 1
    
    if ($extractedFolder) {
        # Move contents to MAVEN_HOME
        Get-ChildItem -Path $extractedFolder.FullName | Move-Item -Destination $MAVEN_HOME -Force
        Remove-Item -Path $extractedFolder.FullName -Force
    }
    Write-Host "Extraction completed!" -ForegroundColor Green
} catch {
    Write-Host "Error extracting Maven: $_" -ForegroundColor Red
    exit 1
}

# Clean up zip file
Remove-Item -Path $MAVEN_ZIP -Force -ErrorAction SilentlyContinue

# Set environment variables for current session
$MAVEN_BIN = "$MAVEN_HOME\bin"
$env:MAVEN_HOME = $MAVEN_HOME
$env:PATH = "$MAVEN_BIN;$env:PATH"

# Add to user PATH permanently
Write-Host "Setting up environment variables..." -ForegroundColor Yellow
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*$MAVEN_BIN*") {
    [Environment]::SetEnvironmentVariable("Path", "$currentPath;$MAVEN_BIN", "User")
    Write-Host "Added Maven to PATH permanently!" -ForegroundColor Green
} else {
    Write-Host "Maven already in PATH!" -ForegroundColor Green
}

# Set MAVEN_HOME permanently
[Environment]::SetEnvironmentVariable("MAVEN_HOME", $MAVEN_HOME, "User")

# Verify installation
Write-Host "`nVerifying installation..." -ForegroundColor Yellow
& "$MAVEN_BIN\mvn.cmd" -version
if ($LASTEXITCODE -eq 0) {
    Write-Host "`nMaven installed successfully!" -ForegroundColor Green
    Write-Host "Maven location: $MAVEN_HOME" -ForegroundColor Cyan
    Write-Host "`nNote: You may need to restart your terminal or IDE for PATH changes to take effect." -ForegroundColor Yellow
} else {
    Write-Host "`nMaven installation completed, but verification failed." -ForegroundColor Yellow
    Write-Host "Please restart your terminal and run 'mvn -version' to verify." -ForegroundColor Yellow
}

