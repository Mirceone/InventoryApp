package com.mirceone.inventoryapp.security;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthRateLimiter {

    private final int maxRequests;
    private final long windowSeconds;
    private final Map<String, ArrayDeque<Long>> requestTimesByKey = new ConcurrentHashMap<>();

    public AuthRateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public boolean allow(String key) {
        long nowEpochSecond = Instant.now().getEpochSecond();
        long oldestAllowed = nowEpochSecond - windowSeconds;

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
}
