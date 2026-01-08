
Write-Host "Compiling JavaFX Mahal Management System..." -ForegroundColor Cyan
cd src

$files = Get-ChildItem -Path "com\mahal" -Recurse -Filter "*.java" | ForEach-Object { 
    $_.FullName.Replace((Get-Location).Path + "\", "") 
}

javac -cp "..\lib\*" -d ..\bin $files

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit $LASTEXITCODE
}

Write-Host "Compilation successful!" -ForegroundColor Green
Write-Host ""
Write-Host "Running application..." -ForegroundColor Cyan
cd ..

$classpath = "lib\sqlite-jdbc.jar;lib\jbcrypt.jar;lib\org.json.jar;lib\javafx.base.jar;lib\javafx.controls.jar;lib\javafx.fxml.jar;lib\javafx.graphics.jar;lib\javafx.media.jar;lib\javafx.swing.jar;lib\javafx.web.jar;bin"
java -cp $classpath com.mahal.MahalApplication

Read-Host "Press Enter to exit"

