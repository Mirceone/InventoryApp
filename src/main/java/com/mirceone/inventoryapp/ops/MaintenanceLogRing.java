package com.mirceone.inventoryapp.ops;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-memory ring buffer for optional diagnostics snippets (last N lines).
 */
public final class MaintenanceLogRing {

    private final int maxLines;
    private final boolean enabled;
    private final Deque<String> deque = new ArrayDeque<>();

    public MaintenanceLogRing(int maxLines, boolean enabled) {
        this.maxLines = Math.max(1, maxLines);
        this.enabled = enabled;
    }

    public synchronized void push(String line) {
        if (!enabled || line == null || line.isBlank()) {
            return;
        }
        if (deque.size() >= maxLines) {
            deque.removeFirst();
        }
        deque.addLast(line.strip());
    }

    public synchronized List<String> recent(int max) {
        int n = Math.min(max, deque.size());
        if (n <= 0) {
            return List.of();
        }
        List<String> tail = new ArrayList<>(deque);
        return tail.subList(Math.max(0, tail.size() - n), tail.size());
    }
}
