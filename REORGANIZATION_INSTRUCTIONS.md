# Frontend Reorganization Instructions

## Overview
This reorganization moves all frontend (JavaFX) files into a `frontend/` folder, creating a clean separation:
- `backend/` - Spring Boot backend (Maven project)
- `frontend/` - JavaFX frontend (Eclipse/Java project)
- Root - Shared files, workspace config, documentation

## Why Reorganize?

This fixes the VS Code package error you've been experiencing. The issue occurs because:
- Root has `.project` with source root: `src`
- Backend has Maven with source root: `src/main/java`
- VS Code gets confused about which source root to use

By separating frontend into its own folder, each project has a clear workspace root.

## Steps to Reorganize

### 1. Backup (Optional but Recommended)
```powershell
# If using git, commit your changes first
git add .
git commit -m "Before frontend reorganization"
```

### 2. Run the Reorganization Script
```powershell
.\reorganize-frontend.ps1
```

This script will:
- Create `frontend/` directory
- Move all frontend files and folders
- Fix `.classpath` (removes backend reference)
- Show summary of moved items

### 3. Verify the Move
Check that these are now in `frontend/`:
- ✅ `src/` - Source code
- ✅ `lib/` - Libraries
- ✅ `bin/` - Compiled classes
- ✅ `run.bat`, `run.ps1` - Run scripts
- ✅ `.project`, `.classpath` - Project files
- ✅ All other frontend files

### 4. Test Frontend
Option A - Run from frontend folder:
```powershell
cd frontend
.\run.bat
```

Option B - Run from root (using helper script):
```powershell
.\frontend-run.bat
```

### 5. Open in VS Code

**Option 1: Open Backend Separately (Recommended)**
```powershell
cd backend
code .
```
Then clean Java Language Server Workspace (`Ctrl+Shift+P` → "Java: Clean Java Language Server Workspace")

**Option 2: Use Multi-Root Workspace**
- Double-click `mahal-workspace.code-workspace`
- This opens both frontend and backend in one window
- Each has its own correct source root

**Option 3: Open Frontend Separately**
```powershell
cd frontend
code .
```

## Files Moved

### Source & Build
- `src/` → `frontend/src/`
- `lib/` → `frontend/lib/`
- `bin/` → `frontend/bin/`
- `out/` → `frontend/out/`

### Configuration
- `.project` → `frontend/.project`
- `.classpath` → `frontend/.classpath` (backend reference removed)
- `.vscode/` → `frontend/.vscode/`
- `mahal.db` → `frontend/mahal.db`
- `supabase.properties` → `frontend/supabase.properties`

### Resources
- `fonts/` → `frontend/fonts/`
- `templates/` → `frontend/templates/`
- `uploads/` → `frontend/uploads/`
- `certificates/` → `frontend/certificates/`

### Scripts & Docs
- `run.bat`, `run.ps1` → `frontend/`
- Frontend documentation files → `frontend/`

## Files Kept in Root

- `backend/` - Backend project (unchanged)
- `docs/` - Shared documentation
- `sql/` - SQL scripts
- `data/` - May contain both frontend and backend data
- `mahal-workspace.code-workspace` - Updated workspace file
- `SyncInitialData.java`, `SyncNow.java` → Moved to `frontend/src/`

## Expected Results

After reorganization:
- ✅ VS Code package errors should be resolved
- ✅ Each project (frontend/backend) has clear source root
- ✅ Can open projects separately or together
- ✅ Build scripts work correctly
- ✅ No broken file paths

## Troubleshooting

**If VS Code still shows errors:**
1. Close VS Code completely
2. Open the specific folder (`backend/` or `frontend/`)
3. Clean Java Language Server Workspace
4. Wait for indexing to complete

**If run scripts don't work:**
- Scripts inside `frontend/` use relative paths (already correct)
- Scripts in root use `frontend-run.bat` / `frontend-run.ps1`

**If file paths are broken:**
- Check that database files (`mahal.db`) are in `frontend/`
- Check that config files (`supabase.properties`) are in `frontend/`
- Java code should use relative paths, which should still work

## Next Steps After Reorganization

1. Test frontend compilation and running
2. Test backend compilation and running
3. Verify VS Code opens without package errors
4. Update any hardcoded paths if needed (though relative paths should work)
5. Commit changes to git if everything works

