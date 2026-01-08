# Payment Security Analysis

## 🔴 CRITICAL SECURITY RISKS

### 1. **CORS Wildcard on Webhook Endpoint** ⚠️ HIGH RISK
**Location:** `WebhookController.java:17`
```java
@CrossOrigin(origins = "*")
```

**Problem:** The webhook endpoint accepts requests from ANY origin. While webhook signature verification provides protection, allowing CORS from all origins is unnecessary and increases attack surface.

**Impact:** 
- Allows potential CSRF attacks
- Exposes endpoint to public discovery
- Unnecessary attack surface

**Recommendation:**
```java
@CrossOrigin(origins = {})  // Remove CORS for webhooks (no browser access needed)
// OR
@CrossOrigin(origins = {"https://api.razorpay.com"})  // Only allow Razorpay if needed
```

---

### 2. **Mock Payment Endpoints in Production** ⚠️ CRITICAL RISK
**Location:** `MockPaymentController.java`, `RazorpaySubscriptionService.java`

**Problem:** If `razorpay.mock.enabled=true` is accidentally set in production, anyone can activate subscriptions without paying.

**Current Protection:** ✅ Good - checks `mockEnabled` flag
```java
if (!mockEnabled) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
}
```

**Additional Risks:**
- If flag is `true`, endpoint is accessible to anyone
- No authentication required on mock endpoints
- Could be exploited if configuration is wrong

**Recommendations:**
1. ✅ Keep current check (already implemented)
2. Add environment variable check in production builds
3. Add warning logs when mock mode is enabled
4. Consider removing mock endpoints entirely in production builds

---

### 3. **API Keys in application.properties** ⚠️ MEDIUM RISK
**Location:** `application.properties:11-13`

**Problem:** API keys are hardcoded in the properties file.

**Current State:**
- Uses environment variables as fallback: `${RAZORPAY_KEY_ID:rzp_test_...}`
- Default values are exposed in the file

**Risk:**
- If repository is public, keys are exposed
- Keys committed to version control
- Anyone with file access can use the keys

**Recommendations:**
1. ✅ Remove default values from properties file
2. ✅ Use ONLY environment variables in production
3. Use Spring Cloud Config or secret management service
4. Add `.properties` to `.gitignore` if containing secrets
5. Rotate keys immediately if exposed

**Better Approach:**
```properties
# application.properties - NO DEFAULT VALUES
razorpay.key.id=${RAZORPAY_KEY_ID}
razorpay.key.secret=${RAZORPAY_KEY_SECRET}
```

---

### 4. **Webhook Signature Verification** ✅ GOOD
**Location:** `WebhookController.java:84-106`

**Status:** ✅ **PROPERLY IMPLEMENTED**

The webhook signature verification is correctly implemented using HMAC-SHA256:
```java
private boolean verifySignature(String payload, String signature) {
    if (WEBHOOK_SECRET == null || WEBHOOK_SECRET.isEmpty()) {
        return false;  // ✅ Fails safely if secret not configured
    }
    // ✅ Uses HMAC-SHA256
    // ✅ Uses constant-time comparison (MessageDigest.isEqual)
}
```

**Note:** Ensure `RAZORPAY_WEBHOOK_SECRET` environment variable is set in production!

---

## 🟡 MEDIUM SECURITY CONCERNS

### 5. **No Rate Limiting** ⚠️ MEDIUM RISK
**Location:** All payment/subscription endpoints

**Problem:** No rate limiting on:
- Subscription status checks (`GET /api/subscriptions/status`)
- Subscription creation (`POST /api/subscriptions`)
- Webhook endpoint

**Impact:**
- DoS attacks possible
- Brute force attempts on subscription IDs
- Unnecessary API calls to Razorpay

**Recommendation:**
Add Spring Boot rate limiting (e.g., `spring-boot-starter-aop` + custom rate limiter or `bucket4j`)

---

### 6. **Payment Status Verification Logic** ⚠️ LOW-MEDIUM RISK
**Location:** `SubscriptionService.java:343-350`

**Current Logic:**
```java
boolean shouldActivate = ("active".equals(razorpayStatus) || "authenticated".equals(razorpayStatus) 
                        || "charged".equals(razorpayStatus) || "completed".equals(razorpayStatus)) 
                        || hasSuccessfulPayment;
```

**Concerns:**
1. **"charged" status**: This might indicate a renewal charge, not initial payment completion. Verify if this should trigger activation.
2. **Payment verification fallback**: The code checks Razorpay API directly, which is good, but there's a potential race condition if webhook arrives after status check.

**Recommendation:**
- Document why each status triggers activation
- Add additional validation (e.g., check payment amount matches expected amount)
- Consider idempotency checks to prevent duplicate activations

---

### 7. **No Input Validation on Subscription ID** ⚠️ LOW RISK
**Location:** Various endpoints accepting `subscription_id` parameter

**Problem:** Subscription IDs from user input are used directly in Razorpay API calls.

**Risk:**
- SQL injection (if IDs are used in queries) - but using JPA should protect against this
- Path traversal in API calls - low risk with proper URL encoding

**Current Protection:** ✅ Using JPA repositories, not direct SQL

**Recommendation:**
- Add input validation (format check for Razorpay subscription IDs)
- Sanitize all user inputs
- Use parameterized queries (already done with JPA)

---

## 🟢 LOW RISK / GOOD PRACTICES

### 8. **Supabase API Key Exposure** ⚠️ LOW RISK
**Location:** `application.properties:68`

**Problem:** Supabase anon key is hardcoded.

**Note:** Anon keys are meant to be public (they're used client-side), but they should still be restricted via RLS policies in Supabase.

**Recommendation:**
- ✅ Ensure Row Level Security (RLS) is enabled in Supabase
- ✅ Verify RLS policies prevent unauthorized access
- Consider using service role key server-side (more secure)

---

### 9. **Error Messages Reveal Information** ⚠️ LOW RISK
**Location:** Various error responses

**Current State:** Error messages might reveal internal details.

**Example:**
```java
return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    .body("Error processing webhook: " + e.getMessage());
```

**Recommendation:**
- Return generic error messages to clients
- Log detailed errors server-side only
- Don't expose stack traces to clients

---

## ✅ SECURITY BEST PRACTICES ALREADY IMPLEMENTED

1. ✅ **Webhook signature verification** - Properly implemented
2. ✅ **Mock payment protection** - Checks flag before allowing
3. ✅ **JPA for database access** - Prevents SQL injection
4. ✅ **Environment variable support** - Allows secure configuration
5. ✅ **HTTPS should be used** - Ensure in production (use Spring Security to enforce)

---

## 📋 RECOMMENDED IMMEDIATE ACTIONS

### Priority 1 (CRITICAL - Do Immediately):
1. **Remove CORS wildcard from WebhookController**
   ```java
   // Change from:
   @CrossOrigin(origins = "*")
   // To:
   @CrossOrigin(origins = {})  // No CORS needed for webhooks
   ```

2. **Secure API Keys**
   - Remove default values from `application.properties`
   - Use ONLY environment variables
   - Verify no keys are committed to git

3. **Verify Mock Mode is Disabled in Production**
   - Check `razorpay.mock.enabled=false` in production
   - Add startup warning if mock mode is enabled

### Priority 2 (HIGH - Do Soon):
4. **Add Rate Limiting** to prevent abuse
5. **Improve Error Messages** - Don't expose internal details
6. **Add Input Validation** for subscription IDs

### Priority 3 (MEDIUM - Good to Have):
7. **Add Monitoring/Alerts** for failed webhook verifications
8. **Add Audit Logging** for all subscription status changes
9. **Add Idempotency Checks** for subscription activation

---

## 🔒 PRODUCTION DEPLOYMENT CHECKLIST

Before deploying to production:

- [ ] Remove CORS wildcard from WebhookController
- [ ] Remove API key defaults from application.properties
- [ ] Set all secrets as environment variables
- [ ] Verify `razorpay.mock.enabled=false`
- [ ] Set `RAZORPAY_WEBHOOK_SECRET` environment variable
- [ ] Enable HTTPS (TLS/SSL)
- [ ] Add rate limiting
- [ ] Review and test webhook signature verification
- [ ] Ensure Supabase RLS policies are enabled
- [ ] Add monitoring/alerting for payment failures
- [ ] Test payment flow end-to-end
- [ ] Review error messages (don't expose internals)
- [ ] Add logging for all subscription status changes
- [ ] Backup database before deployment

