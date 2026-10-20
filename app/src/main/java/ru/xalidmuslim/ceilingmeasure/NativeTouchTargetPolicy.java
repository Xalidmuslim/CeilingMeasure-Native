package ru.xalidmuslim.ceilingmeasure;

/** Hit-target arbitration in screen dp, independent of drawing scale. */
final class NativeTouchTargetPolicy {
    static final float PRECISE_CORNER_RADIUS_DP = 10f;
    static final float WALL_HIT_RADIUS_DP = 17f;
    static final long EDIT_HOLD_MS = 220L;
    static final long WALL_START_HOLD_MS = 130L;

    private NativeTouchTargetPolicy() {}

    /** Exact corner dot keeps its action; nearby wall segment wins elsewhere. */
    static boolean preferWall(float cornerDistanceDp, float wallDistanceDp, float wallLengthDp) {
        if (!Float.isFinite(wallDistanceDp) || wallDistanceDp > WALL_HIT_RADIUS_DP) return false;
        if (cornerDistanceDp <= PRECISE_CORNER_RADIUS_DP) return false;
        // The short-wall case must not be swallowed by adjacent vertex hitboxes.
        return wallLengthDp < 90f || cornerDistanceDp >= 13f;
    }
}
