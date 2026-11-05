package app.chatgpt.web;

/** Lightweight main-thread heartbeat. Measures scheduling delay, not WebView FPS. */
final class UiStallTracker {
    static final long INTERVAL_MS = 250;
    static final long STALL_THRESHOLD_MS = 350;
    private long lastExpectedMs = -1;
    private long maxDelayMs;
    private int stallCount;
    private int sampleCount;

    void start(long nowMs) { lastExpectedMs = nowMs + INTERVAL_MS; }

    void sample(long nowMs) {
        if (lastExpectedMs < 0) return;
        long delay = Math.max(0, nowMs - lastExpectedMs);
        maxDelayMs = Math.max(maxDelayMs, delay);
        if (delay >= STALL_THRESHOLD_MS) stallCount++;
        sampleCount++;
        // Reschedule relative to actual time to avoid a catch-up loop after a stall.
        lastExpectedMs = nowMs + INTERVAL_MS;
    }

    void stop() { lastExpectedMs = -1; }
    int stalls() { return stallCount; }
    int samples() { return sampleCount; }
    long maxDelayMs() { return maxDelayMs; }
}
