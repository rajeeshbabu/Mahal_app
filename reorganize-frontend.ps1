# Reorganize Frontend into Separate Folder
# This script moves all frontend files to frontend/ directory

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Reorganizing Frontend into frontend/ folder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$rootPath = $PSScriptRoot
$frontendPath = Join-Path $rootPath "frontend"

# Create frontend directory
Write-Host "Creating frontend/ directory..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $frontendPath -Force | Out-Null

# List of frontend files/folders to move
$frontendItems = @(
    "src",
    "lib",
    "bin",
    "out",
    "fonts",
    "templates",
    "uploads",
    "certificates",
    "mahal.db",
    "supabase.properties",
    "supabase.properties.example",
    "run.bat",
    "run.ps1",
    "download_malayalam_font.bat",
    "download-pdf-libs.ps1",
    ".project",
    ".classpath",
    ".vscode"
)

# Frontend documentation files
$frontendDocs = @(
    "README.md",
    "README_MAHAL.md",
    "FIX_BUILD_PATH.md",
    "FONT_SETUP_INSTRUCTIONS.md",
    "PDF_LIBRARY_SETUP.md",
    "SUBSCRIPTION_SETUP.md",
    "RAZORPAY_INTEGRATION_SUMMARY.md"
)

# Frontend text files (compile outputs, errors)
$frontendTextFiles = @(
    "errors.txt",
    "error_inv.txt",
    "compile_output.txt",
    "dashboard_debug.txt"
)

# Frontend sync utilities (move to frontend/src/)
$frontendSyncFiles = @(
    "SyncInitialData.java",
    "SyncNow.java"
)

Write-Host "Moving frontend files and folders..." -ForegroundColor Yellow

# Move main items
foreach ($item in $frontendItems) {
    $source = Join-Path $rootPath $item
    $dest = Join-Path $frontendPath $item
    
    if (Test-Path $source) {
        Write-Host "  Moving: $item" -ForegroundColor Gray
        Move-Item -Path $source -Destination $dest -Force
    }
}

# Move documentation
foreach ($doc in $frontendDocs) {
    $source = Join-Path $rootPath $doc
    if (Test-Path $source) {
        Write-Host "  Moving: $doc" -ForegroundColor Gray
        Move-Item -Path $source -Destination (Join-Path $frontendPath $doc) -Force
    }
}

# Move text files
foreach ($txt in $frontendTextFiles) {
    $source = Join-Path $rootPath $txt
    if (Test-Path $source) {
        Write-Host "  Moving: $txt" -ForegroundColor Gray
        Move-Item -Path $source -Destination (Join-Path $frontendPath $txt) -Force
    }
}

# Move sync utilities to frontend/src/
foreach ($syncFile in $frontendSyncFiles) {
    $source = Join-Path $rootPath $syncFile
    if (Test-Path $source) {
        Write-Host "  Moving: $syncFile -> frontend/src/" -ForegroundColor Gray
        $dest = Join-Path $frontendPath "src\$syncFile"
        Move-Item -Path $source -Destination $dest -Force
    }
}

# Fix .classpath - remove backend reference and fix paths
$classpathFile = Join-Path $frontendPath ".classpath"
if (Test-Path $classpathFile) {
    Write-Host ""
    Write-Host "Fixing .classpath file..." -ForegroundColor Yellow
    $classpathContent = Get-Content $classpathFile -Raw
    # Remove the backend reference line
    $classpathContent = $classpathContent -replace '\s*<classpathentry kind="src" path="backend/src/main/java/com/mahal"/>', ''
    Set-Content -Path $classpathFile -Value $classpathContent
    Write-Host "  Removed backend source reference from .classpath" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Reorganization Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Review the moved files in frontend/ folder"
Write-Host "2. Update run.bat and run.ps1 in frontend/ (paths should be correct)"
Write-Host "3. Update mahal-workspace.code-workspace (change frontend path from '.' to 'frontend')"
Write-Host "4. Close and reopen VS Code in the new structure"
Write-Host "5. Open backend folder separately, or use the multi-root workspace"
Write-Host ""
Write-Host "To run frontend from root, use: frontend-run.bat or frontend-run.ps1" -ForegroundColor Cyan
Write-Host ""

