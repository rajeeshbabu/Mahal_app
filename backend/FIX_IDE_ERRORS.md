# Fixing IDE Errors in Backend

## Problem
The IDE (VS Code/Eclipse) shows errors like:
- "Cannot be resolved to a type" for Spring Boot classes
- "Package does not match expected package"
- "The import org.springframework cannot be resolved"

## Root Cause
These are **IDE configuration issues**, not actual code errors. The code compiles successfully with Maven (`mvn compile` works).

## Solution

### Automatic Fix (Recommended)
Run the fix script:
```bash
cd backend
.\fix-ide-errors.bat
```

Then follow the IDE-specific steps below.

### Manual Fix for VS Code

1. **Clean Java Language Server Workspace:**
   - Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on Mac)
   - Type: `Java: Clean Java Language Server Workspace`
   - Select it and confirm to restart

2. **Reload Window:**
   - Press `Ctrl+Shift+P`
   - Type: `Developer: Reload Window`
   - Select it

3. **Verify Maven is Recognized:**
   - Open VS Code Command Palette (`Ctrl+Shift+P`)
   - Type: `Java: Rebuild Projects`
   - Wait for it to complete

### Manual Fix for Eclipse

1. **Update Maven Project:**
   - Right-click on the `backend` project
   - Select `Maven` → `Update Project...`
   - Check "Force Update of Snapshots/Releases"
   - Click `OK`

2. **Clean and Build:**
   - Right-click on the `backend` project
   - Select `Project` → `Clean...`
   - Select the project and click `Clean`

### Verification

After applying the fix, verify:
- ✅ No red error markers in Java files
- ✅ Spring Boot imports resolve correctly
- ✅ Maven dependencies are visible in the IDE

### If Errors Persist

1. **Delete IDE Cache:**
   - VS Code: Delete `.vscode/.settings` and `.metadata` folders (if they exist)
   - Eclipse: Delete `.metadata` folder and re-import the project

2. **Re-import Project:**
   - Close the IDE
   - Delete `.classpath` and `.project` files (backup first)
   - Reopen IDE and import as Maven project

3. **Check Java Version:**
   - Ensure Java 17 or higher is configured
   - Verify `JAVA_HOME` environment variable is set correctly

## Notes

- The backend code **compiles successfully** with Maven - errors are IDE-only
- Running `mvn clean compile` confirms the code is correct
- IDE errors don't prevent the application from running
- The fix script downloads dependencies and regenerates IDE configuration files
