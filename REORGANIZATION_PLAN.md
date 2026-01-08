# Frontend Reorganization Plan

## Goal
Move all frontend (JavaFX) files into a `frontend/` folder, keeping the structure clean with:
- `backend/` - Spring Boot backend
- `frontend/` - JavaFX frontend  
- Root - Shared files and workspace config

## Files to Move to `frontend/`

### Source & Build
- `src/` → `frontend/src/`
- `lib/` → `frontend/lib/`
- `bin/` → `frontend/bin/`
- `out/` → `frontend/out/`

### Configuration & Data
- `.project` → `frontend/.project`
- `.classpath` → `frontend/.classpath` (will need to fix path references)
- `.vscode/` → `frontend/.vscode/`
- `mahal.db` → `frontend/mahal.db`
- `supabase.properties` → `frontend/supabase.properties`
- `supabase.properties.example` → `frontend/supabase.properties.example`

### Resources
- `fonts/` → `frontend/fonts/`
- `templates/` → `frontend/templates/`
- `uploads/` → `frontend/uploads/`
- `certificates/` → `frontend/certificates/`

### Scripts
- `run.bat` → `frontend/run.bat` (paths will be updated)
- `run.ps1` → `frontend/run.ps1` (paths will be updated)
- `download_malayalam_font.bat` → `frontend/download_malayalam_font.bat`
- `download-pdf-libs.ps1` → `frontend/download-pdf-libs.ps1`

### Documentation
- `README.md` → `frontend/README.md`
- `README_MAHAL.md` → `frontend/README_MAHAL.md`
- `FIX_BUILD_PATH.md` → `frontend/FIX_BUILD_PATH.md`
- `FONT_SETUP_INSTRUCTIONS.md` → `frontend/FONT_SETUP_INSTRUCTIONS.md`
- `PDF_LIBRARY_SETUP.md` → `frontend/PDF_LIBRARY_SETUP.md`
- `SUBSCRIPTION_SETUP.md` → `frontend/SUBSCRIPTION_SETUP.md`
- `RAZORPAY_INTEGRATION_SUMMARY.md` → `frontend/RAZORPAY_INTEGRATION_SUMMARY.md`

### Output Files
- `errors.txt` → `frontend/errors.txt`
- `error_inv.txt` → `frontend/error_inv.txt`
- `compile_output.txt` → `frontend/compile_output.txt`
- `dashboard_debug.txt` → `frontend/dashboard_debug.txt`

## Files to Keep in Root

- `backend/` - Backend project (stays as-is)
- `docs/` - Shared documentation
- `sql/` - SQL scripts (could be shared or frontend-specific)
- `data/` - Database files (might need to decide backend vs frontend)
- `mahal-workspace.code-workspace` - Workspace file (will update)
- `SyncInitialData.java`, `SyncNow.java` - Utility scripts (keep in root or move?)

## After Moving - Updates Needed

1. **Update `.classpath` in frontend:**
   - Remove line: `<classpathentry kind="src" path="backend/src/main/java/com/mahal"/>`
   - All paths are now relative to `frontend/` folder

2. **Update `run.bat` and `run.ps1`:**
   - Paths now relative to `frontend/` folder
   - Or create new scripts in root that reference `frontend/`

3. **Update workspace file:**
   - Change frontend path from `"."` to `"frontend"`

4. **Update any hardcoded paths in Java code:**
   - Check for references to `mahal.db`, `supabase.properties`, etc.
   - May need to use relative paths or update file locations

## Running the Reorganization

1. **Backup first!** (or commit to git)
2. Run: `.\reorganize-frontend.ps1`
3. Review moved files
4. Update scripts and configs as needed
5. Test compilation and running

## Verification

After reorganization:
- ✅ Frontend compiles from `frontend/` folder
- ✅ Backend still works from `backend/` folder  
- ✅ VS Code opens correctly (use multi-root workspace or open folders separately)
- ✅ No broken file paths
- ✅ Package errors in VS Code should be resolved

