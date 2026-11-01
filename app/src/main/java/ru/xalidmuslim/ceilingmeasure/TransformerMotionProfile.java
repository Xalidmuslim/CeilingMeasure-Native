package ru.xalidmuslim.ceilingmeasure;

/** Pure, device-independent motion contract. No animation or Android allocations. */
final class TransformerMotionProfile {
    static final long ENTRY_MS = 310L;
    static final long NAV_OUT_MS = 88L;
    static final long NAV_RETURN_MS = 235L;
    // Prevent duplicate rapid taps; this never delays routing.
    static final long NAV_TAP_LOCK_MS = 120L;
    static final float START_SCALE = .986f;
    static final float ENTRY_X_DP = 14f;
    static final float ENTRY_Y_DP = 7f;
    static final float NAV_X_DP = 7f;
    static final float NAV_Y_DP = 4f;

    static final long SECTION_REVEAL_MS = 440L;

    /** Left items travel left, right items right, a single center item stays centered. */
    static int directionForIndex(int position, int total) {
        if (total <= 1) return 0;
        int twice = 2 * position;
        int midpoint = total - 1;
        return Integer.compare(twice, midpoint);
    }
}
