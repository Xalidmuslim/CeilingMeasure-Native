package ru.xalidmuslim.ceilingmeasure;

/** Piecewise, allocation-free timeline for the native ceiling assembly artwork. */
final class CeilingInstallationProfile {
    static final int DURATION_MS = 320; // Tap handoff is unchanged.
    static final int IDLE_DURATION_MS = 1950;
    static final int IDLE_HOLD_MS = 1150;
    static final int RESTART_AFTER_CANCEL_MS = 140;

    private CeilingInstallationProfile() {}

    static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    /** Smoothstep avoids frame-to-frame velocity jumps at stage boundaries. */
    static float phase(float p, float begin, float end) {
        float t = clamp((p - begin) / (end - begin));
        return t * t * (3f - 2f * t);
    }

    // Drawing the baguette, fastening / tensioning the membrane, then light.
    static float profile(float p) { return phase(p, .00f, .26f); }
    static float membrane(float p) { return phase(p, .20f, .66f); }
    static float wrinkles(float p) { return 1f - phase(p, .44f, .70f); }
    static float light(float p, int index) {
        return phase(p, .62f + .073f * index, .76f + .073f * index);
    }
    static float finish(float p) { return phase(p, .75f, 1f); }
}
