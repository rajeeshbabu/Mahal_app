# PowerShell script to download OpenHTMLToPDF and dependencies
# Run this script from the project root directory

$libDir = "lib"
if (-not (Test-Path $libDir)) { 
    New-Item -ItemType Directory -Path $libDir 
    Write-Host "Created lib directory"
}

Write-Host "Downloading PDF library JAR files..."
Write-Host ""

$jars = @(
    @{
        Url = "https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.10/openhtmltopdf-pdfbox-1.0.10.jar"
        Name = "openhtmltopdf-pdfbox-1.0.10.jar"
    },
    @{
        Url = "https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-core/1.0.10/openhtmltopdf-core-1.0.10.jar"
        Name = "openhtmltopdf-core-1.0.10.jar"
    },
    @{
        Url = "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
        Name = "slf4j-api-1.7.36.jar"
    },
    @{
        Url = "https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/2.0.27/pdfbox-2.0.27.jar"
        Name = "pdfbox-2.0.27.jar"
    },
    @{
        Url = "https://repo1.maven.org/maven2/org/apache/pdfbox/fontbox/2.0.27/fontbox-2.0.27.jar"
        Name = "fontbox-2.0.27.jar"
    },
    @{
        Url = "https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar"
        Name = "commons-logging-1.2.jar"
    },
    @{
        Url = "https://repo1.maven.org/maven2/org/apache/pdfbox/xmpbox/2.0.27/xmpbox-2.0.27.jar"
        Name = "xmpbox-2.0.27.jar"
    }
)

$successCount = 0
$failCount = 0

foreach ($jar in $jars) {
    $filePath = Join-Path $libDir $jar.Name
    
    # Skip if file already exists
    if (Test-Path $filePath) {
        Write-Host "Skipping $($jar.Name) - already exists" -ForegroundColor Yellow
        continue
    }
    
    try {
        Write-Host "Downloading $($jar.Name)..." -NoNewline
        Invoke-WebRequest -Uri $jar.Url -OutFile $filePath -UseBasicParsing
        Write-Host " OK" -ForegroundColor Green
        $successCount++
    }
    catch {
        Write-Host " FAILED" -ForegroundColor Red
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        $failCount++
    }
}

Write-Host ""
if ($failCount -eq 0) {
    Write-Host "All JAR files downloaded successfully!" -ForegroundColor Green
    Write-Host "Total: $successCount files" -ForegroundColor Green
} else {
    Write-Host "Download completed with errors." -ForegroundColor Yellow
    Write-Host "Success: $successCount, Failed: $failCount" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Rebuild your project in your IDE"
Write-Host "2. Make sure the lib folder is included in your classpath"
Write-Host "3. Test PDF generation by creating a new certificate"

