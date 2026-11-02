package ru.xalidmuslim.ceilingmeasure;

/** Pure safe horizontal swipe threshold, independent of drawing scale. */
final class NativeRoomSwipePolicy {
    static final int MIN_DISTANCE_DP = 70;
    static final int ARM_DISTANCE_DP = 14;
    private NativeRoomSwipePolicy() {}
    static boolean horizontal(float dx, float dy, float density, int distanceDp) {
        return Math.abs(dx) >= distanceDp * density
                && Math.abs(dx) >= Math.abs(dy) * 1.75f;
    }
    /** Left -> next room; right -> previous. */
    static int direction(float dx) { return dx < 0f ? 1 : -1; }
    static int target(int current, int count, int direction) {
        int next = current + direction;
        return next >= 0 && next < count ? next : current;
    }
}
