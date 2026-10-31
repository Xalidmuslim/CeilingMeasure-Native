package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;

import androidx.annotation.NonNull;

/** Independently stored from the canonical typography/measurement data contract. */
final class NativeTransformerMode {
    private static final String PREFS = "ceiling_animation_prefs";
    private static final String KEY_ENABLED = "transformer_enabled";

    private NativeTransformerMode() {}

    static boolean enabled(@NonNull Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
