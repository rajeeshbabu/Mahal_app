# Permanent Razorpay API Keys Setup - Windows

You have **3 options** to set environment variables permanently. Choose the one you prefer:

---

## Option 1: GUI Method (Easiest - Recommended) ⭐

This is the simplest method - no scripts needed!

### Steps:

1. **Open System Properties:**
   - Press `Windows Key + R`
   - Type: `sysdm.cpl`
   - Press Enter
   
   OR
   
   - Right-click **This PC** (or **My Computer**) → **Properties**
   - Click **Advanced system settings** (on the left)

2. **Open Environment Variables:**
   - Click **Environment Variables...** button

3. **Add User Variables:**
   - Under **User variables** (top section), click **New...**
   
   - **Variable 1:**
     - **Variable name:** `RAZORPAY_KEY_ID`
     - **Variable value:** `rzp_test_RxJksqriVHTcTu` (your actual key)
     - Click **OK**
   
   - **Variable 2:**
     - Click **New...** again
     - **Variable name:** `RAZORPAY_KEY_SECRET`
     - **Variable value:** `1bF6nphPQ174RoEAiE8qAOye` (your actual secret)
     - Click **OK**

4. **Apply Changes:**
   - Click **OK** on all dialogs

5. **Restart Your IDE:**
   - Close IntelliJ IDEA (or your IDE) completely
   - Reopen it
   - Now run your Spring Boot application - it should work!

**Done!** The variables are now set permanently. You won't need to set them again.

---

## Option 2: PowerShell Script (One-Time Run)

1. **Edit the script:**
   - Open `setup-razorpay-keys-permanent.ps1`
   - Make sure lines 28-29 have your actual keys (already done if you edited the other script)

2. **Run the script:**
   - Right-click `setup-razorpay-keys-permanent.ps1` → **Run with PowerShell**
   - OR open PowerShell and run: `.\setup-razorpay-keys-permanent.ps1`

3. **Restart your IDE:**
   - Close and reopen IntelliJ IDEA (or your IDE)
   - The variables will now be available

**Done!** No need to run the script again.

---

## Option 3: PowerShell Command (One-Time)

Open PowerShell and run these commands (replace with your actual keys):

```powershell
[System.Environment]::SetEnvironmentVariable("RAZORPAY_KEY_ID", "rzp_test_RxJksqriVHTcTu", [System.EnvironmentVariableTarget]::User)
[System.Environment]::SetEnvironmentVariable("RAZORPAY_KEY_SECRET", "1bF6nphPQ174RoEAiE8qAOye", [System.EnvironmentVariableTarget]::User)
```

Then **restart your IDE**.

---

## After Setting Variables Permanently

### For IDE (IntelliJ IDEA, Eclipse, etc.):
1. **Restart the IDE completely** (close and reopen)
2. Run your Spring Boot application - it should work!

### For Command Line:
- Open a **new** PowerShell/Command Prompt window
- Run: `mvn spring-boot:run`
- It will work automatically!

### Verify Variables Are Set:

**PowerShell:**
```powershell
echo $env:RAZORPAY_KEY_ID
echo $env:RAZORPAY_KEY_SECRET
```

**Command Prompt:**
```cmd
echo %RAZORPAY_KEY_ID%
echo %RAZORPAY_KEY_SECRET%
```

If you see your keys (not blank), they're set correctly!

---

## Troubleshooting

### Issue: IDE Still Can't Find Variables
**Solution:** 
- Close IDE completely (not just the window, fully quit the application)
- Reopen IDE
- Try again

If still not working, set variables in IDE's Run Configuration (see `IDE_SETUP_INSTRUCTIONS.md`)

### Issue: "Access Denied" When Running Script
**Solution:**
- Right-click PowerShell → **Run as Administrator**
- Run the script again

### Issue: Variables Not Showing in New Terminal
**Solution:**
- Close the terminal window completely
- Open a **new** terminal window
- Variables will be available

---

## Summary

✅ **Best Option:** Use GUI Method (Option 1) - it's the simplest and most reliable

✅ **After Setup:** Restart your IDE, then you're done - no need to run scripts again!

✅ **One-Time Setup:** Once set, these variables persist across reboots and sessions


