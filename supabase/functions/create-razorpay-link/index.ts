/// <reference lib="deno.ns" />
/// <reference lib="deno.window" />

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

Deno.serve(async (req: Request) => {
    // Handle CORS
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders })
    }

    try {
        const payload = await req.json()
        const { userId, planDuration, amountRupees } = payload

        console.log("Received request:", JSON.stringify(payload))

        if (!userId || !planDuration || !amountRupees) {
            return new Response(JSON.stringify({ error: 'Missing required fields: userId, planDuration, amountRupees' }), {
                headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                status: 400
            })
        }

        const razorpayKeyId = Deno.env.get('RAZORPAY_KEY_ID')
        const razorpayKeySecret = Deno.env.get('RAZORPAY_KEY_SECRET')

        if (!razorpayKeyId || !razorpayKeySecret) {
            console.error('RAZORPAY_KEY_ID or RAZORPAY_KEY_SECRET not set in Supabase Secrets')
            return new Response(JSON.stringify({ error: 'Razorpay keys not configured on server. Please set secrets.' }), {
                headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                status: 500
            })
        }

        const amountInPaise = Math.round(parseFloat(amountRupees) * 100)

        // Create Payment Link data as per Razorpay API
        const paymentLinkData = {
            amount: amountInPaise,
            currency: "INR",
            accept_partial: false,
            description: `Mahal App Subscription - ${planDuration}`,
            customer: {
                email: `${userId}@mahal-app.internal`
            },
            notify: {
                email: true,
                sms: false
            },
            reminder_enable: true,
            notes: {
                user_id: String(userId),
                plan_duration: planDuration.toLowerCase()
            }
        }

        console.log(`Calling Razorpay for User ${userId}, Amount ${amountInPaise}`)

        const authHeader = `Basic ${btoa(razorpayKeyId + ":" + razorpayKeySecret)}`

        const response = await fetch("https://api.razorpay.com/v1/payment_links", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": authHeader
            },
            body: JSON.stringify(paymentLinkData)
        })

        const razorpayResponse = await response.json()

        if (!response.ok) {
            console.error('Razorpay Error:', JSON.stringify(razorpayResponse))
            return new Response(JSON.stringify({ error: 'Razorpay API Error', details: razorpayResponse }), {
                headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                status: response.status
            })
        }

        console.log(`Success: Payment Link ${razorpayResponse.id} created`)

        return new Response(JSON.stringify({
            checkout_url: razorpayResponse.short_url,
            subscription_id: razorpayResponse.id,
            status: 'pending'
        }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 200
        })

    } catch (err: any) {
        console.error('Edge Function Request Error:', err?.message || 'Unknown error')
        return new Response(JSON.stringify({ error: err?.message || 'Internal Server Error' }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 500
        })
    }
})
