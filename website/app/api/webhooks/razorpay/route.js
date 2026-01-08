import { createClient } from '@supabase/supabase-js';
import crypto from 'crypto';
import { NextResponse } from 'next/server';

// Initialize Supabase client
// For production, SUPABASE_SERVICE_ROLE_KEY should be set in environment variables
const supabase = createClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL || '',
    process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || ''
);

// Helper to get formatted Indian Standard Time (IST) string: YYYY-MM-DD HH:mm:ss.SSS
const getFormattedDateTime = (date = new Date()) => {
    try {
        const formatter = new Intl.DateTimeFormat('en-GB', {
            timeZone: 'Asia/Kolkata',
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
        });

        const parts = formatter.formatToParts(date);
        const getPart = (type) => parts.find(p => p.type === type).value;

        const year = getPart('year');
        const month = getPart('month');
        const day = getPart('day');
        const hour = getPart('hour');
        const minute = getPart('minute');
        const second = getPart('second');
        const ms = String(date.getMilliseconds()).padStart(3, '0');

        return `${year}-${month}-${day} ${hour}:${minute}:${second}.${ms}`;
    } catch (e) {
        return date.toISOString().replace('T', ' ').replace('Z', '');
    }
};

export async function POST(req) {
    try {
        const body = await req.text();
        const signature = req.headers.get('x-razorpay-signature');
        const secret = process.env.RAZORPAY_WEBHOOK_SECRET;

        if (!secret) {
            console.error('❌ [WEBHOOK] RAZORPAY_WEBHOOK_SECRET is not defined');
            return NextResponse.json({ error: 'Webhook secret not configured' }, { status: 500 });
        }

        // 1. Verify Signature
        const expectedSignature = crypto
            .createHmac('sha256', secret)
            .update(body)
            .digest('hex');

        if (expectedSignature !== signature) {
            console.warn('⚠️ [WEBHOOK] Invalid signature detected');
            return NextResponse.json({ error: 'Invalid signature' }, { status: 401 });
        }

        const payload = JSON.parse(body);
        const event = payload.event;
        console.log(`🔔 [WEBHOOK] Received Razorpay Event: ${event}`);

        // 2. Handle Subscription and Payment Events
        if (event === 'payment.captured' || event === 'subscription.activated' || event === 'subscription.charged') {
            const entity = payload.payload?.payment?.entity || payload.payload?.subscription?.entity;

            if (!entity) {
                return NextResponse.json({ message: 'No entity found in payload' }, { status: 200 });
            }

            const notes = entity.notes || {};
            const userId = notes.user_id;

            if (!userId) {
                console.warn('⚠️ [WEBHOOK] No user_id found in notes. Skipping update.');
                return NextResponse.json({ message: 'No user_id found' }, { status: 200 });
            }

            const planDuration = notes.plan_duration || 'monthly';
            const startDate = new Date();
            const endDate = new Date(startDate);

            if (planDuration.toLowerCase() === 'monthly') {
                endDate.setMonth(startDate.getMonth() + 1);
            } else {
                endDate.setFullYear(startDate.getFullYear() + 1);
            }

            console.log(`✅ [WEBHOOK] Updating subscription for User: ${userId}, Plan: ${planDuration}`);

            // 3. Update Supabase Database
            const { error } = await supabase
                .from('subscriptions')
                .update({
                    status: 'active',
                    start_date: getFormattedDateTime(startDate),
                    end_date: getFormattedDateTime(endDate),
                    updated_at: getFormattedDateTime(),
                    razorpay_subscription_id: entity.subscription_id || entity.id
                })
                .eq('user_id', userId);

            if (error) {
                console.error('❌ [WEBHOOK] Database Update Failed:', error.message);
                return NextResponse.json({ error: 'Database update failed' }, { status: 500 });
            }

            console.log('✨ [WEBHOOK] Supabase updated successfully');
        }

        return NextResponse.json({ message: 'Webhook processed' }, { status: 200 });
    } catch (err) {
        console.error('💥 [WEBHOOK] Fatal Error:', err.message);
        return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
    }
}
