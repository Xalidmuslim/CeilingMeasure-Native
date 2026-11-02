package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Bottom-navigation layout is independent of animation mode, project data and
 * typography JSON. Existing installations retain their contextual layout.
 */
final class NativeBottomNavMode {
    private static final String PREFS = "ceiling_bottom_nav";
    private static final String KEY_FIXED = "fixed_v1";

    private NativeBottomNavMode() {}

    static boolean isFixed(@NonNull Context context) {
        return isFixed(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    static void setFixed(@NonNull Context context, boolean fixed) {
        setFixed(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE), fixed);
    }

    static boolean isFixed(@NonNull SharedPreferences prefs) {
        return prefs.getBoolean(KEY_FIXED, false);
    }

    static void setFixed(@NonNull SharedPreferences prefs, boolean fixed) {
        prefs.edit().putBoolean(KEY_FIXED, fixed).apply();
    }
}
