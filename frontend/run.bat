@echo off
setlocal enabledelayedexpansion
echo Cleaning build directory...
if exist ..\bin rmdir /s /q ..\bin
mkdir ..\bin

echo Compiling JavaFX Mahal Management System...
cd src

REM Use PowerShell to find and compile all Java files
powershell -Command "$files = Get-ChildItem -Path 'com\mahal' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName.Replace((Get-Location).Path + '\', '') }; javac -cp '..\lib\*' -d ..\bin $files"
xcopy resources ..\bin\resources /E /I /Y

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)

echo Compilation successful!
echo.
echo Running application...
cd ..
java -cp "bin;lib\*" com.mahal.MahalApplication
pause
