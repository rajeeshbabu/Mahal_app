/// <reference lib="deno.ns" />
/// <reference lib="deno.window" />

// deno-lint-ignore no-remote-import
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7"

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

// Helper to convert hex string to Uint8Array
const hexToUint8Array = (hex: string) => {
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < hex.length; i += 2) {
        bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
    }
    return bytes;
};

Deno.serve(async (req: Request) => {
    // Handle CORS
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders })
    }

    try {
        const body = await req.text()
        const signature = req.headers.get('x-razorpay-signature')
        const secret = Deno.env.get('RAZORPAY_WEBHOOK_SECRET')

        if (!secret) {
            console.error('RAZORPAY_WEBHOOK_SECRET is not set in Supabase Secrets')
            return new Response(JSON.stringify({ error: 'Config error: RAZORPAY_WEBHOOK_SECRET missing' }), { status: 500 })
        }

        if (!signature) {
            console.warn('Missing x-razorpay-signature header')
            return new Response(JSON.stringify({ error: 'Unauthorized: Missing signature' }), { status: 401 })
        }

        // 1. Verify Signature using Web Crypto API (more robust than older std lib imports)
        const encoder = new TextEncoder()
        const keyData = encoder.encode(secret)
        const key = await crypto.subtle.importKey(
            'raw',
            keyData,
            { name: 'HMAC', hash: 'SHA-256' },
            false,
            ['verify']
        )

        const isValid = await crypto.subtle.verify(
            'HMAC',
            key,
            hexToUint8Array(signature),
            encoder.encode(body)
        )

        if (!isValid) {
            console.warn('Invalid HMAC signature')
            return new Response(JSON.stringify({ error: 'Unauthorized: Invalid signature' }), { status: 401 })
        }

        const payload = JSON.parse(body)
        const event = payload.event
        console.log(`[WEBHOOK] Received verified event: ${event}`)

        // 2. Process Successful Payment Events
        if (['payment.captured', 'subscription.activated', 'subscription.charged', 'payment_link.paid'].includes(event)) {
            const entity = payload.payload?.payment?.entity ||
                payload.payload?.payment_link?.entity ||
                payload.payload?.subscription?.entity

            const notes = entity?.notes || {}
            const userId = notes.user_id
            const planDuration = notes.plan_duration || 'monthly'

            if (!userId) {
                console.warn(`[WEBHOOK] No userId found in notes for event: ${event}. Payload notes:`, JSON.stringify(notes))
                return new Response('Handled: No userId in notes', { status: 200 })
            }

            // Initialize Supabase Admin Client
            const supabaseAdmin = createClient(
                Deno.env.get('SUPABASE_URL') ?? '',
                Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
            )

            // Calculate dates
            const now = new Date()
            const end = new Date(now)
            if (planDuration.toLowerCase() === 'monthly') {
                end.setMonth(now.getMonth() + 1)
            } else {
                end.setFullYear(now.getFullYear() + 1)
            }

            const formatDate = (d: Date) => d.toISOString().replace('T', ' ').split('.')[0] + '.000'

            // 3. Update Database (Syncs to local App via SyncManager/SubscriptionService)
            console.log(`[WEBHOOK] Activating subscription for User: ${userId}`)
            const { error } = await supabaseAdmin
                .from('subscriptions')
                .update({
                    status: 'active',
                    start_date: formatDate(now),
                    end_date: formatDate(end),
                    updated_at: formatDate(now),
                    razorpay_subscription_id: entity.subscription_id || entity.id
                })
                .eq('user_id', userId)

            if (error) {
                console.error('[WEBHOOK] DB Update Error:', error.message)
                return new Response(JSON.stringify({ error: error.message }), { status: 500 })
            }

            console.log(`[WEBHOOK] Success: Activated subscriber ${userId}`)
        }

        return new Response(JSON.stringify({ message: 'Webhook processed successfully' }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 200,
        })
    } catch (err: unknown) {
        const error = err as Error
        console.error('[WEBHOOK] Fatal error:', error.message)
        return new Response(JSON.stringify({ error: error.message }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 500,
        })
    }
})
