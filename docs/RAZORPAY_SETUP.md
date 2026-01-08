# Razorpay API Keys Setup Guide

## ⚠️ Security Notice

**DO NOT commit API keys to version control!** API keys must be set as environment variables.

## Quick Setup

### Step 1: Get Your Razorpay API Keys

1. Go to [Razorpay Dashboard](https://dashboard.razorpay.com/)
2. Navigate to **Settings** → **API Keys**
3. Copy your:
   - **Key ID** (e.g., `rzp_test_xxxxxxxxxxxxx` or `rzp_live_xxxxxxxxxxxxx`)
   - **Key Secret** (usually 20-32 characters)

### Step 2: Set Environment Variables

#### Windows (PowerShell)
```powershell
# Set for current session
$env:RAZORPAY_KEY_ID="rzp_test_xxxxxxxxxxxxx"
$env:RAZORPAY_KEY_SECRET="your_secret_key_here"

# To verify
echo $env:RAZORPAY_KEY_ID
echo $env:RAZORPAY_KEY_SECRET
```

#### Windows (Command Prompt)
```cmd
# Set for current session
set RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxxx
set RAZORPAY_KEY_SECRET=your_secret_key_here

# To verify
echo %RAZORPAY_KEY_ID%
echo %RAZORPAY_KEY_SECRET%
```

#### Windows (Permanent - System Properties)
1. Right-click **This PC** → **Properties**
2. Click **Advanced system settings**
3. Click **Environment Variables**
4. Under **User variables** or **System variables**, click **New**
5. Add:
   - Variable name: `RAZORPAY_KEY_ID`
   - Variable value: `rzp_test_xxxxxxxxxxxxx`
6. Repeat for `RAZORPAY_KEY_SECRET`

#### Linux/Mac (Bash/Zsh)
```bash
# Set for current session
export RAZORPAY_KEY_ID="rzp_test_xxxxxxxxxxxxx"
export RAZORPAY_KEY_SECRET="your_secret_key_here"

# To verify
echo $RAZORPAY_KEY_ID
echo $RAZORPAY_KEY_SECRET

# To make permanent, add to ~/.bashrc or ~/.zshrc:
echo 'export RAZORPAY_KEY_ID="rzp_test_xxxxxxxxxxxxx"' >> ~/.bashrc
echo 'export RAZORPAY_KEY_SECRET="your_secret_key_here"' >> ~/.bashrc
source ~/.bashrc
```

### Step 3: Set Webhook Secret (Optional but Recommended)

For webhook signature verification:

#### Windows (PowerShell)
```powershell
$env:RAZORPAY_WEBHOOK_SECRET="your_webhook_secret_from_razorpay_dashboard"
```

#### Windows (Command Prompt)
```cmd
set RAZORPAY_WEBHOOK_SECRET=your_webhook_secret_from_razorpay_dashboard
```

#### Linux/Mac
```bash
export RAZORPAY_WEBHOOK_SECRET="your_webhook_secret_from_razorpay_dashboard"
```

Get your webhook secret from: Razorpay Dashboard → **Settings** → **Webhooks** → Copy the secret

### Step 4: Verify Configuration

Run your Spring Boot application. Check the logs for:
- ✅ No errors about missing Razorpay keys
- ✅ Successful Razorpay API connections (if not using mock mode)

## Development vs Production

### Development (Testing)
- Use **Test Mode** keys (`rzp_test_...`)
- Set `razorpay.mock.enabled=true` in `application.properties` to test without real payments

### Production
- Use **Live Mode** keys (`rzp_live_...`)
- **MUST** set `razorpay.mock.enabled=false`
- **MUST** set all keys as environment variables (never in files)
- **MUST** set `RAZORPAY_WEBHOOK_SECRET` for webhook verification

## Troubleshooting

### Error: "Razorpay key not configured"
**Solution:** Make sure environment variables are set. Check:
1. Variables are set in the same terminal/session where you run the application
2. Variable names are exactly: `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`
3. No extra spaces in the values
4. Restart your IDE/terminal after setting variables

### Error: "Invalid Razorpay credentials"
**Solution:** 
1. Verify keys are correct from Razorpay dashboard
2. Check if you're using test keys with live mode (or vice versa)
3. Make sure keys haven't been rotated/regenerated

### Error: "Webhook signature verification failed"
**Solution:**
1. Set `RAZORPAY_WEBHOOK_SECRET` environment variable
2. Get the correct secret from Razorpay Dashboard → Settings → Webhooks
3. Ensure webhook URL in Razorpay matches your server URL

## Security Best Practices

1. ✅ **Never commit keys to git**
   - Check `.gitignore` includes `.properties` files if they contain secrets
   - Use environment variables only

2. ✅ **Use different keys for test and production**
   - Test keys for development
   - Live keys for production only

3. ✅ **Rotate keys regularly**
   - If keys are exposed, regenerate them immediately in Razorpay dashboard
   - Update environment variables on all servers

4. ✅ **Use secret management services** (for production)
   - AWS Secrets Manager
   - HashiCorp Vault
   - Azure Key Vault
   - Kubernetes Secrets

5. ✅ **Enable webhook signature verification**
   - Always set `RAZORPAY_WEBHOOK_SECRET`
   - Verify signatures in `WebhookController`

## Environment Variables Summary

Required for production:
- `RAZORPAY_KEY_ID` - Your Razorpay Key ID
- `RAZORPAY_KEY_SECRET` - Your Razorpay Key Secret
- `RAZORPAY_WEBHOOK_SECRET` - Webhook signature secret (recommended)

Optional for development:
- `razorpay.mock.enabled=true` - Use mock payments (no real API calls)

