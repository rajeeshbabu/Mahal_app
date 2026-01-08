@echo off
echo Opening backend folder in VS Code...
cd /d "%~dp0backend"
code .
echo.
echo VS Code should open with backend as workspace root.
echo If errors persist, press Ctrl+Shift+P and run:
echo "Java: Clean Java Language Server Workspace"
pause

