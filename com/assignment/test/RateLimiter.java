package com.assignment.test;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class RateLimiter {
    private final int maxRequestsPerMinute;
    private final ConcurrentHashMap<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public boolean allowRequest(String userId) {
        TokenBucket bucket = userBuckets.computeIfAbsent(userId, id -> new TokenBucket(maxRequestsPerMinute));
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int maxTokens;
        private int tokens;
        private long lastRefillTimestamp;
        private final Lock lock = new ReentrantLock();

        TokenBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.tokens = maxTokens;
            this.lastRefillTimestamp = System.nanoTime();
        }

        boolean tryConsume() {
            lock.lock();
            try {
                refillTokensIfNeeded();
                if (tokens > 0) {
                    tokens--;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        private void refillTokensIfNeeded() {
            long now = System.nanoTime();
            long elapsedTime = now - lastRefillTimestamp;
            long tokensToAdd = (elapsedTime * maxTokens) / TimeUnit.MINUTES.toNanos(1);

            if (tokensToAdd > 0) {
                tokens = Math.min(maxTokens, tokens + (int) tokensToAdd);
                lastRefillTimestamp = now;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        RateLimiter limiter = new RateLimiter(100);

        for (int i = 0; i < 105; i++) {
            boolean allowed = limiter.allowRequest("user1");
            System.out.println("Request " + (i + 1) + " for user1: " + (allowed ? "Allowed" : "Denied"));
            if (!allowed) {
                Thread.sleep(100);
            }
        }
    }
}
