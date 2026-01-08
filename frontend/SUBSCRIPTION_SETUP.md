# Subscription System Setup Guide

This guide explains how to set up the Netflix-style subscription payment system with Razorpay integration.

## Architecture Overview

The subscription system consists of:
1. **Spring Boot Backend** - Handles subscription creation, status checks, and webhook processing
2. **JavaFX Desktop App** - Checks subscription status on startup and shows subscription screen if needed
3. **Razorpay Integration** - Handles payment processing via hosted checkout page

## Backend Setup (Spring Boot)

### 1. Add Dependencies

Add these dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Boot Starter Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MySQL Connector -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    
    <!-- Spring Security (for authentication) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20231013</version>
    </dependency>
</dependencies>
```

### 2. Configure Razorpay Credentials

Set these environment variables or add to `application.properties`:

```properties
# Razorpay Configuration
razorpay.key.id=your_razorpay_key_id
razorpay.key.secret=your_razorpay_key_secret

# Webhook Secret (get from Razorpay Dashboard)
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
```

**To get Razorpay credentials:**
1. Sign up at https://razorpay.com
2. Go to Settings → API Keys
3. Generate Key ID and Key Secret
4. Go to Settings → Webhooks
5. Create a webhook endpoint pointing to: `https://your-domain.com/api/webhooks/razorpay`
6. Copy the webhook secret

### 3. Database Setup

Run the migration script to create the `subscriptions` table:

```sql
-- See: backend/src/main/resources/db/migration/V1__create_subscriptions_table.sql
```

Or manually create the table:

```sql
CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    plan_duration VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date DATETIME,
    end_date DATETIME,
    razorpay_subscription_id VARCHAR(255) UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_razorpay_subscription_id (razorpay_subscription_id)
);
```

### 4. Configure Webhook URL in Razorpay

1. Go to Razorpay Dashboard → Settings → Webhooks
2. Add webhook URL: `https://your-domain.com/api/webhooks/razorpay`
3. Select events:
   - `subscription.activated`
   - `subscription.charged`
   - `subscription.cancelled`
   - `subscription.paused`
   - `subscription.resumed`
   - `subscription.completed`
   - `payment.failed`
4. Save the webhook secret

## JavaFX Desktop App Setup

### 1. Subscription Check Flow

The app checks subscription status:
- **On startup** (if user is logged in)
- **After login**
- **After successful payment**

### 2. Subscription Screen

The subscription screen shows:
- Monthly plan: ₹999/month
- Yearly plan: ₹9,999/year (with "Best Value" badge)

### 3. Payment Flow

1. User clicks "Subscribe" on a plan
2. App calls backend API to create subscription
3. Backend returns Razorpay checkout URL
4. App opens checkout URL in WebView
5. User completes payment via UPI or Card
6. Razorpay redirects to success page
7. App detects success and checks subscription status
8. If active, app unlocks and shows dashboard

## API Endpoints

### Check Subscription Status
```
GET /api/subscriptions/status
Authorization: Bearer <token>
Response: {
    "active": true/false,
    "status": "active|expired|not_found",
    "planDuration": "monthly|yearly",
    "endDate": "2024-12-31T23:59:59"
}
```

### Create Subscription
```
POST /api/subscriptions/create
Authorization: Bearer <token>
Body: {
    "planDuration": "monthly" | "yearly"
}
Response: {
    "checkoutUrl": "https://rzp.io/...",
    "subscriptionId": "sub_xxx",
    "status": "pending"
}
```

### Webhook Endpoint
```
POST /api/webhooks/razorpay
Headers: X-Razorpay-Signature: <signature>
Body: <Razorpay webhook payload>
```

## Payment Methods

The system is configured to accept **ONLY**:
- ✅ UPI
- ✅ Debit/Credit Cards
- ❌ Wallets (disabled)
- ❌ Net Banking (disabled)
- ❌ COD (disabled)

## Subscription Plans

### Monthly Plan
- Price: ₹999/month
- Billing: Monthly recurring
- Auto-renewal: Enabled

### Yearly Plan
- Price: ₹9,999/year
- Billing: Annual recurring
- Savings: ₹1,989 compared to monthly
- Auto-renewal: Enabled

## Security Features

1. **Webhook Signature Verification**: All webhooks are verified using HMAC SHA256
2. **User Authentication**: All API endpoints require valid JWT token
3. **Hosted Checkout**: Payment details never touch the desktop app
4. **Online Validation**: Subscription status is always checked online (no offline grace period)

## Testing

### Test Mode
1. Use Razorpay Test Mode credentials
2. Test cards: https://razorpay.com/docs/payments/test-cards/
3. Test UPI: Use any UPI ID (e.g., `success@razorpay`)

### Production Mode
1. Switch to Razorpay Live Mode
2. Update credentials in backend
3. Configure production webhook URL
4. Test with real payment methods

## Troubleshooting

### App stays locked even after payment
- Check webhook is configured correctly
- Verify webhook secret matches
- Check backend logs for webhook processing errors
- Manually check subscription status via API

### Payment page not loading
- Verify Razorpay credentials are correct
- Check network connectivity
- Ensure WebView is enabled in JavaFX

### Webhook not receiving events
- Verify webhook URL is accessible from internet
- Check Razorpay webhook configuration
- Verify webhook secret is correct
- Check backend logs for incoming requests

## Notes

- **No offline grace period**: App requires active internet connection to validate subscription
- **Auto-renewal**: Subscriptions automatically renew at the end of billing period
- **Cancellation**: Users can cancel subscriptions, which will be reflected in the next billing cycle
- **Status check**: App checks subscription status on every startup and after login

