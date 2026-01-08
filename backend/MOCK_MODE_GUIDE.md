# Mock Razorpay Mode - Testing Guide

## Overview

Mock mode allows you to test the subscription system without real Razorpay API keys or actual payments. This is perfect for development and testing.

## How to Enable Mock Mode

Mock mode is **already enabled** by default in `application.properties`:

```properties
razorpay.mock.enabled=true
```

To disable mock mode and use real Razorpay:
```properties
razorpay.mock.enabled=false
```

## How It Works

1. **Subscription Creation**: When you create a subscription, it generates a fake subscription ID and returns a mock checkout URL
2. **Mock Payment Page**: The checkout URL opens a test payment page at `http://localhost:8080/mock-payment.html`
3. **Payment Simulation**: You can simulate payment success or failure using buttons on the page
4. **Database Updates**: The subscription status is updated in your database based on the simulated payment result

## Testing Flow

### Step 1: Create a Subscription

From your JavaFX app, click "Subscribe" on either Monthly or Yearly plan. This will:
- Create a subscription record in the database with status "pending"
- Return a mock checkout URL
- Open the mock payment page in a WebView

### Step 2: Simulate Payment

On the mock payment page, you'll see:
- **Subscription ID**: The fake subscription ID
- **User ID**: The user creating the subscription
- **Plan**: monthly or yearly

Click one of the buttons:
- **✅ Simulate Payment Success**: Activates the subscription (status → "active")
- **❌ Simulate Payment Failure**: Cancels the subscription (status → "cancelled")

### Step 3: Verify Subscription Status

After simulating payment:
- Check subscription status via: `GET /api/subscriptions/status`
- The subscription should now be "active" (if success) or "cancelled" (if failure)
- Your JavaFX app should automatically unlock if subscription is active

## API Endpoints for Mock Mode

### Mock Payment Page
```
GET /mock-payment.html?subscription_id={id}&user_id={userId}&plan={plan}
```
Opens the HTML payment simulation page.

### Simulate Payment Success
```
POST /mock-payment/success?subscription_id={id}
```
Activates the subscription in the database.

### Simulate Payment Failure
```
POST /mock-payment/failure?subscription_id={id}
```
Cancels the subscription in the database.

## Console Output

When mock mode is active, you'll see warnings in the backend console:
```
⚠️  MOCK MODE: Created fake subscription sub_mock_1234567890 for user test_user
⚠️  MOCK MODE: Checkout URL: http://localhost:8080/mock-payment.html?...
⚠️  MOCK MODE: To activate subscription, manually update status in database or use webhook simulator
```

## Switching to Real Razorpay

When you're ready to use real Razorpay:

1. **Disable mock mode**:
   ```properties
   razorpay.mock.enabled=false
   ```

2. **Set real Razorpay keys**:
   ```properties
   razorpay.key.id=rzp_test_xxxxxxxxxxxxx
   razorpay.key.secret=your_secret_key_here
   ```
   Or use environment variables (recommended):
   ```powershell
   $env:RAZORPAY_KEY_ID="rzp_test_xxxxxxxxxxxxx"
   $env:RAZORPAY_KEY_SECRET="your_secret_key_here"
   ```

3. **Restart the backend**

## Benefits of Mock Mode

✅ **No API Keys Required**: Test without Razorpay account  
✅ **No Real Payments**: Safe for development  
✅ **Fast Testing**: Instant responses, no network delays  
✅ **Full Flow Testing**: Test complete subscription lifecycle  
✅ **Easy Debugging**: Clear console messages and status updates  

## Limitations

⚠️ **Not for Production**: Mock mode should never be enabled in production  
⚠️ **No Real Webhooks**: Webhook events are not simulated (use manual API calls)  
⚠️ **Fake URLs**: Checkout URLs are localhost only, not real Razorpay pages  

## Troubleshooting

**Q: Mock payment page doesn't open**  
A: Make sure the backend is running on port 8080 and the static HTML file exists at `src/main/resources/static/mock-payment.html`

**Q: Subscription doesn't activate after clicking success**  
A: Check the backend console for errors. Verify the subscription ID matches.

**Q: Want to test webhook events**  
A: Use the WebhookController endpoints or manually call the subscription service methods.

