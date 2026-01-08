import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import { createHmac } from "https://deno.land/std@0.168.0/node/crypto.ts"

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
    // Handle CORS
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders })
    }

    try {
        const body = await req.text()
        const signature = req.headers.get('x-razorpay-signature')
        const secret = Deno.env.get('RAZORPAY_WEBHOOK_SECRET')

        if (!secret) {
            console.error('RAZORPAY_WEBHOOK_SECRET is not set')
            return new Response(JSON.stringify({ error: 'Config error' }), { status: 500 })
        }

        // 1. Verify Signature
        const expectedSignature = createHmac('sha256', secret)
            .update(body)
            .digest('hex')

        if (expectedSignature !== signature) {
            console.warn('Invalid signature')
            return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401 })
        }

        const payload = JSON.parse(body)
        const event = payload.event
        console.log(`Received event: ${event}`)

        // 2. Process Successful Payment Events
        if (['payment.captured', 'subscription.activated', 'subscription.charged', 'payment_link.paid'].includes(event)) {
            const entity = payload.payload?.payment?.entity ||
                payload.payload?.payment_link?.entity ||
                payload.payload?.subscription?.entity

            const notes = entity?.notes || {}
            const userId = notes.user_id
            const planDuration = notes.plan_duration || 'monthly'

            if (!userId) {
                console.warn(`[WEBHOOK] No userId found in notes for event: ${event}. Payload:`, JSON.stringify(payload.payload))
                return new Response('No userId found', { status: 200 }) // Return 200 to acknowledge receipt
            }

            // Initialize Supabase Admin Client
            const supabaseAdmin = createClient(
                Deno.env.get('SUPABASE_URL') ?? '',
                Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
            )

            // Calculate dates (simplified UTC formatting for sync compatibility)
            const now = new Date()
            const end = new Date(now)
            if (planDuration.toLowerCase() === 'monthly') {
                end.setMonth(now.getMonth() + 1)
            } else {
                end.setFullYear(now.getFullYear() + 1)
            }

            const formatDate = (d: Date) => d.toISOString().replace('T', ' ').split('.')[0] + '.000'

            // 3. Update Database
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
                console.error('DB Update Error:', error.message)
                return new Response(JSON.stringify({ error: error.message }), { status: 500 })
            }

            console.log(`Successfully activated subscription for user: ${userId}`)
        }

        return new Response(JSON.stringify({ message: 'Success' }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 200,
        })
    } catch (err) {
        console.error('Fatal error:', err.message)
        return new Response(JSON.stringify({ error: err.message }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 500,
        })
    }
})
