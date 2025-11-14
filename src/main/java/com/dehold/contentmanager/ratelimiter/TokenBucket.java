package com.dehold.contentmanager.ratelimiter;

import java.util.concurrent.atomic.AtomicLong;

public class TokenBucket {
    private final long capacity;
    private final long refillTokens;
    private final long refillIntervalMillis;

    private final AtomicLong tokens;
    private volatile long lastRefillTimestamp;

    public TokenBucket(long capacity, long refillTokens, long refillIntervalMillis) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMillis = refillIntervalMillis;
        this.tokens = new AtomicLong(capacity);
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    private synchronized void refill() {
        long now = System.currentTimeMillis();
        if (now <= lastRefillTimestamp) return;
        long elapsed = now - lastRefillTimestamp;
        long slots = elapsed / refillIntervalMillis;
        if (slots > 0) {
            long added = slots * refillTokens;
            long newTokens = Math.min(capacity, tokens.get() + added);
            tokens.set(newTokens);
            lastRefillTimestamp += slots * refillIntervalMillis;
        }
    }

    /**
     * Try to consume n tokens. Returns true if successful.
     */
    public boolean tryConsume(long n) {
        refill();
        while (true) {
            long current = tokens.get();
            if (current < n) return false;
            if (tokens.compareAndSet(current, current - n)) return true;
        }
    }

    public long getAvailableTokens() {
        refill();
        return tokens.get();
    }
}
