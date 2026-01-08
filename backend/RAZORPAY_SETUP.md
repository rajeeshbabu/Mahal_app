# Razorpay API Setup Guide

## Error: Authentication Failed (401)

The backend is currently using placeholder Razorpay API keys. You need to configure your actual Razorpay credentials.

## Option 1: Set Environment Variables (Recommended)

### Windows PowerShell:
```powershell
$env:RAZORPAY_KEY_ID="rzp_test_xxxxxxxxxxxxx"
$env:RAZORPAY_KEY_SECRET="your_secret_key_here"
```

### Windows Command Prompt:
```cmd
set RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxxx
set RAZORPAY_KEY_SECRET=your_secret_key_here
```

### Permanent Setup (Windows):
1. Open System Properties → Environment Variables
2. Add new User variables:
   - `RAZORPAY_KEY_ID` = `rzp_test_xxxxxxxxxxxxx`
   - `RAZORPAY_KEY_SECRET` = `your_secret_key_here`

## Option 2: Update application.properties

Edit `backend/src/main/resources/application.properties`:

```properties
razorpay.key.id=rzp_test_xxxxxxxxxxxxx
razorpay.key.secret=your_secret_key_here
```

**⚠️ WARNING:** Never commit real API keys to version control!

## How to Get Razorpay API Keys

1. **Sign up/Login** to [Razorpay Dashboard](https://dashboard.razorpay.com/)
2. Go to **Settings** → **API Keys**
3. Generate **Test Keys** (for development) or **Live Keys** (for production)
4. Copy the **Key ID** (starts with `rzp_test_` or `rzp_live_`)
5. Copy the **Key Secret** (shown only once - save it securely!)

## Test Mode vs Live Mode

- **Test Mode**: Use keys starting with `rzp_test_`
  - No real money transactions
  - Use test cards: https://razorpay.com/docs/payments/test-cards/
  
- **Live Mode**: Use keys starting with `rzp_live_`
  - Real money transactions
  - Requires KYC verification

## Verify Setup

After setting the keys, restart the backend. The authentication error should be resolved.

## Webhook Secret (Optional for now)

For webhook signature verification, you'll also need to set:
```powershell
$env:RAZORPAY_WEBHOOK_SECRET="your_webhook_secret"
```

Get this from: Razorpay Dashboard → Settings → Webhooks → Webhook Secret

