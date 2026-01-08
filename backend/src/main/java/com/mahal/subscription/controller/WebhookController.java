package com.mahal.subscription.controller;

import com.mahal.subscription.service.WebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/razorpay")
// No CORS needed - webhooks are server-to-server (not browser requests)
// Razorpay sends webhooks directly, signature verification ensures authenticity
public class WebhookController {

    @Autowired
    private WebhookService webhookService;

    private static final String WEBHOOK_SECRET = System.getenv("RAZORPAY_WEBHOOK_SECRET");

    /**
     * Handle Razorpay webhook events
     * Verifies webhook signature before processing
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            // Verify webhook signature
            if (!verifySignature(payload, signature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // Parse webhook event
            Map<String, Object> event = webhookService.parseWebhookPayload(payload);
            String eventType = (String) event.get("event");

            // Handle different webhook events
            switch (eventType) {
                case "subscription.activated":
                case "subscription.charged":
                    webhookService.handleSubscriptionActivated(event);
                    break;
                case "payment.authorized":
                    // Payment authorized - activate subscription immediately
                    webhookService.handlePaymentAuthorized(event);
                    break;
                case "payment.captured":
                    // Payment captured (successful payment) - activate subscription
                    System.out.println("💰 Received payment.captured webhook event");
                    webhookService.handlePaymentCaptured(event);
                    break;
                case "subscription.cancelled":
                    webhookService.handleSubscriptionCancelled(event);
                    break;
                case "subscription.paused":
                    webhookService.handleSubscriptionPaused(event);
                    break;
                case "subscription.resumed":
                    webhookService.handleSubscriptionResumed(event);
                    break;
                case "subscription.completed":
                    webhookService.handleSubscriptionCompleted(event);
                    break;
                case "payment.failed":
                    webhookService.handlePaymentFailed(event);
                    break;
                default:
                    System.out.println("Unhandled webhook event: " + eventType);
            }

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            // Log detailed error server-side, but don't expose to client
            System.err.println("Error processing webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }

    /**
     * Verify Razorpay webhook signature
     */
    private boolean verifySignature(String payload, String signature) {
        try {
            if (WEBHOOK_SECRET == null || WEBHOOK_SECRET.isEmpty()) {
                System.err.println("RAZORPAY_WEBHOOK_SECRET not configured");
                return false;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = bytesToHex(hash);

            return MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
