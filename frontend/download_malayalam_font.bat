@echo off
echo Downloading Noto Sans Malayalam font...
echo.

REM Create fonts directory if it doesn't exist
if not exist "fonts" mkdir fonts

REM Download using PowerShell
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/google/fonts/raw/main/ofl/notosansmalayalam/NotoSansMalayalam%5Bwdth%2Cwght%5D.ttf' -OutFile 'fonts\NotoSansMalayalam-Regular.ttf'}"

if exist "fonts\NotoSansMalayalam-Regular.ttf" (
    echo.
    echo SUCCESS! Font downloaded to fonts\NotoSansMalayalam-Regular.ttf
    echo You can now generate PDFs with Malayalam content.
) else (
    echo.
    echo FAILED to download font automatically.
    echo.
    echo Please manually download from:
    echo https://fonts.google.com/noto/specimen/Noto+Sans+Malayalam
    echo.
    echo Then extract and copy NotoSansMalayalam-Regular.ttf to the fonts folder.
)

pause

