package ru.xalidmuslim.ceilingmeasure;

/** Pure, device-independent motion contract. No animation or Android allocations. */
final class TransformerMotionProfile {
    static final long ENTRY_MS = 310L;
    static final long NAV_OUT_MS = 88L;
    static final long NAV_RETURN_MS = 235L;
    static final long CLICK_COMMIT_MS = 95L;
    static final float START_SCALE = .986f;
    static final float ENTRY_X_DP = 14f;
    static final float ENTRY_Y_DP = 7f;
    static final float NAV_X_DP = 7f;
    static final float NAV_Y_DP = 4f;

    static final long SCATTER_MS = 440L;
    static final long SCATTER_STEP_MS = 82L;
    static final float SCATTER_X_DP = 42f;
    static final float SCATTER_Y_DP = 23f;
    static final float SCATTER_ROTATION_Y = 8f;

    /** Sequence: center, left edge, right edge, then alternating inward. */
    static int scatterRank(int index, int count) {
        if (count < 1 || index < 0 || index >= count) return 0;
        int middle = count / 2;
        if (index == middle) return 0;
        return index < middle ? index * 2 + 1 : (count - 1 - index) * 2 + 2;
    }

    private TransformerMotionProfile() {}

    /** Left items travel left, right items right, a single center item stays centered. */
    static int directionForIndex(int position, int total) {
        if (total <= 1) return 0;
        int twice = 2 * position;
        int midpoint = total - 1;
        return Integer.compare(twice, midpoint);
    }
}
