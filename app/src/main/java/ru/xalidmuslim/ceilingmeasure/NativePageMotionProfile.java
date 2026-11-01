package ru.xalidmuslim.ceilingmeasure;

/** Short, horizontal page navigation; button motion has a separate profile. */
final class NativePageMotionProfile {
    static final long DURATION_MS = 185L;
    static final float SLIDE_DP = 14f;
    static final float START_ALPHA = .975f;

    private NativePageMotionProfile() {}

    /** +1: forward from the right. -1: back from the left. */
    static int direction(boolean back) {
        return back ? -1 : 1;
    }
}
