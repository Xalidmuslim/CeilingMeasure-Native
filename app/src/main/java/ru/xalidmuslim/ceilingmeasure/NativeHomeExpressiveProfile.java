package ru.xalidmuslim.ceilingmeasure;

/** Pure timing and interaction contract for the native Java Home screen. */
final class NativeHomeExpressiveProfile {
    static final int MICRO_MS = 235;
    // Indicator reaches its tab before the destination obscures Home.
    static final int INDICATOR_TRAVEL_MS = 145;
    static final int INDICATOR_SETTLE_MS = 85;
    static final int MORPH_MS = 460;
    static final int STAGGER_STEP_MS = 45;
    static final int SHARED_AXIS_MS = 270;
    static final int DOCK_SLOTS = 5;
    static final int CENTER_SLOT = 2;
    static final int QUICK_ACTIONS_COUNT = 3;

    static final String CTA_TRANSITION_NAME = "ceiling_new_measure_shared_container";
    static final String CTA_LAUNCH_EXTRA = "ceiling_shared_cta_launch";

    private NativeHomeExpressiveProfile() {}

    static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }

    static boolean shouldHideDock(int scrollY, int previousY, int totalRange,
            int minimumScroll) {
        return totalRange > minimumScroll && scrollY > minimumScroll
                && scrollY - previousY >= 3;
    }
}
