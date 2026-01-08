# Razorpay API Keys Setup - Windows Guide

## ⭐ Recommended: Permanent Setup (Set Once, Use Forever)

**Don't want to run scripts every time?** Use permanent setup:

👉 **See: `PERMANENT_SETUP_WINDOWS.md`** for the easiest GUI method (recommended!)

OR use: `setup-razorpay-keys-permanent.ps1` (one-time script, no need to run again)

---

## Quick Setup (Choose One Method - Temporary/Session-Based)

### Method 1: Using PowerShell Script (Recommended)

1. **Open the setup script:**
   - Navigate to `backend` folder
   - Open `setup-razorpay-keys.ps1` in a text editor (Notepad, VS Code, etc.)

2. **Edit the script:**
   - Replace `YOUR_KEY_ID_HERE` with your actual Razorpay Key ID
   - Replace `YOUR_KEY_SECRET_HERE` with your actual Razorpay Key Secret
   - Get your keys from: https://dashboard.razorpay.com/ → Settings → API Keys

3. **Run the script:**
   - Right-click `setup-razorpay-keys.ps1` → **Run with PowerShell**
   - OR open PowerShell in the `backend` folder and run:
     ```powershell
     .\setup-razorpay-keys.ps1
     ```

4. **Start your application:**
   - In the **same PowerShell window**, run:
     ```powershell
     mvn spring-boot:run
     ```

---

### Method 2: Using Batch Script

1. **Open the setup script:**
   - Navigate to `backend` folder
   - Open `setup-razorpay-keys.bat` in a text editor

2. **Edit the script:**
   - Replace `YOUR_KEY_ID_HERE` with your actual Razorpay Key ID
   - Replace `YOUR_KEY_SECRET_HERE` with your actual Razorpay Key Secret

3. **Run the script:**
   - Double-click `setup-razorpay-keys.bat`
   - OR open Command Prompt in `backend` folder and run:
     ```cmd
     setup-razorpay-keys.bat
     ```

4. **Start your application:**
   - In the **same Command Prompt window**, run:
     ```cmd
     mvn spring-boot:run
     ```

---

### Method 3: Manual Setup (One-Time)

#### Option A: PowerShell (Current Session Only)

Open PowerShell and run:
```powershell
$env:RAZORPAY_KEY_ID="rzp_test_xxxxxxxxxxxxx"
$env:RAZORPAY_KEY_SECRET="your_secret_key_here"
```

**Note:** These variables only last for the current PowerShell session. Close the window and you'll need to set them again.

#### Option B: PowerShell (Permanent - User Profile)

Open PowerShell as Administrator and run:
```powershell
[System.Environment]::SetEnvironmentVariable("RAZORPAY_KEY_ID", "rzp_test_xxxxxxxxxxxxx", [System.EnvironmentVariableTarget]::User)
[System.Environment]::SetEnvironmentVariable("RAZORPAY_KEY_SECRET", "your_secret_key_here", [System.EnvironmentVariableTarget]::User)
```

**Note:** You may need to restart your IDE/terminal for changes to take effect.

#### Option C: System Properties (Permanent - GUI Method)

1. Right-click **This PC** (or **My Computer**) → **Properties**
2. Click **Advanced system settings** (on the left)
3. Click **Environment Variables** button
4. Under **User variables** (or **System variables**), click **New**
5. Add these two variables:
   - **Variable name:** `RAZORPAY_KEY_ID`
   - **Variable value:** `rzp_test_xxxxxxxxxxxxx` (your actual key)
   - Click **OK**
6. Click **New** again:
   - **Variable name:** `RAZORPAY_KEY_SECRET`
   - **Variable value:** `your_secret_key_here` (your actual secret)
   - Click **OK**
7. Click **OK** on all dialogs
8. **Restart your IDE/terminal** for changes to take effect

---

### Method 4: Command Prompt (Current Session Only)

Open Command Prompt and run:
```cmd
set RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxxx
set RAZORPAY_KEY_SECRET=your_secret_key_here
```

**Note:** These variables only last for the current Command Prompt session.

---

## Verify Your Setup

### PowerShell
```powershell
echo $env:RAZORPAY_KEY_ID
echo $env:RAZORPAY_KEY_SECRET
```

### Command Prompt
```cmd
echo %RAZORPAY_KEY_ID%
echo %RAZORPAY_KEY_SECRET%
```

If you see your keys (or values other than blank), the setup is correct!

---

## Getting Your Razorpay API Keys

1. Go to [Razorpay Dashboard](https://dashboard.razorpay.com/)
2. Log in to your account
3. Navigate to **Settings** → **API Keys**
4. You'll see:
   - **Key ID** (e.g., `rzp_test_xxxxxxxxxxxxx` for test mode)
   - **Key Secret** (click "Reveal" to see it - usually 20-32 characters)
5. Copy both values

**Important:**
- Use **Test Mode** keys (`rzp_test_...`) for development/testing
- Use **Live Mode** keys (`rzp_live_...`) for production only
- Never share your keys or commit them to version control

---

## Setting Webhook Secret (Optional but Recommended)

For webhook signature verification:

### PowerShell
```powershell
$env:RAZORPAY_WEBHOOK_SECRET="your_webhook_secret_from_dashboard"
```

Or permanently:
```powershell
[System.Environment]::SetEnvironmentVariable("RAZORPAY_WEBHOOK_SECRET", "your_webhook_secret", [System.EnvironmentVariableTarget]::User)
```

### Command Prompt
```cmd
set RAZORPAY_WEBHOOK_SECRET=your_webhook_secret_from_dashboard
```

Get your webhook secret from: Razorpay Dashboard → **Settings** → **Webhooks** → Copy the secret

---

## Troubleshooting

### Issue: "Could not resolve placeholder 'razorpay.key.id'"
**Solution:** Environment variables are not set. Run one of the setup methods above.

### Issue: "Invalid Razorpay credentials"
**Solution:** 
- Verify your keys are correct from Razorpay dashboard
- Make sure you're using test keys with test mode (or live keys with live mode)
- Check for extra spaces when copying keys

### Issue: Variables not working after setting them
**Solution:**
- If using current session method, make sure you start the app in the **same** terminal/PowerShell window
- If using permanent method, **restart your IDE/terminal** after setting variables
- Verify variables with `echo $env:RAZORPAY_KEY_ID` (PowerShell) or `echo %RAZORPAY_KEY_ID%` (Command Prompt)

### Issue: PowerShell script execution is disabled
**Solution:**
Run PowerShell as Administrator and execute:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

## Security Reminders

✅ **DO:**
- Use environment variables (never hardcode keys in files)
- Use test keys for development
- Rotate keys if exposed
- Keep keys secret

❌ **DON'T:**
- Commit keys to git/version control
- Share keys in emails/messages
- Use production keys for testing
- Hardcode keys in source code

---

## Next Steps

After setting up the keys:
1. Start your Spring Boot backend: `mvn spring-boot:run`
2. Verify no errors about missing Razorpay configuration
3. Test subscription creation
4. Check logs for successful Razorpay API connections

For more detailed information, see: `docs/RAZORPAY_SETUP.md`

