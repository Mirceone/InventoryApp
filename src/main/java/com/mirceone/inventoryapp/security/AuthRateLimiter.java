package com.mirceone.inventoryapp.security;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window-per-key in-memory rate limiter. Note: state is per-instance and is not shared
 * across nodes; for a multi-instance deployment a shared store (e.g. Redis) would be required.
 */
public class AuthRateLimiter {

    /** Run an eviction sweep of idle keys roughly every N {@link #allow} calls. */
    private static final int SWEEP_EVERY_CALLS = 1000;

    private final int maxRequests;
    private final long windowSeconds;
    private final Map<String, ArrayDeque<Long>> requestTimesByKey = new ConcurrentHashMap<>();
    private final AtomicInteger callsSinceSweep = new AtomicInteger();

    public AuthRateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public boolean allow(String key) {
        long nowEpochSecond = Instant.now().getEpochSecond();
        long oldestAllowed = nowEpochSecond - windowSeconds;

        if (callsSinceSweep.incrementAndGet() >= SWEEP_EVERY_CALLS) {
            callsSinceSweep.set(0);
            evictIdleKeys(oldestAllowed);
        }

        ArrayDeque<Long> deque = requestTimesByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() <= oldestAllowed) {
                deque.pollFirst();
            }

            if (deque.size() >= maxRequests) {
                return false;
            }

            deque.addLast(nowEpochSecond);
            return true;
        }
    }

    /**
     * Removes keys whose timestamps have all aged out of the window, bounding memory growth.
     * Without this, one entry per distinct key would accumulate indefinitely.
     */
    private void evictIdleKeys(long oldestAllowed) {
        for (Map.Entry<String, ArrayDeque<Long>> entry : requestTimesByKey.entrySet()) {
            ArrayDeque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() <= oldestAllowed) {
                    deque.pollFirst();
                }
                if (deque.isEmpty()) {
                    // Guarded removal: only drop the mapping if it still points at this empty deque.
                    requestTimesByKey.remove(entry.getKey(), deque);
                }
            }
        }
    }
}
