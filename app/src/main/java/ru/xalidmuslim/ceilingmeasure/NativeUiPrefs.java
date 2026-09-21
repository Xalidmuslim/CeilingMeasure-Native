package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

final class NativeUiPrefs {
    static final String FONT_SYSTEM = "system";
    static final String FONT_SERIF = "serif";
    static final String FONT_MONO = "mono";

    static final String WEIGHT_NORMAL = "normal";
    static final String WEIGHT_MEDIUM = "medium";
    static final String WEIGHT_BOLD = "bold";

    private static final String PREFS = "ceiling_ui_prefs";
    private static final String KEY_FONT = "font";
    private static final String KEY_SCALE = "scale";
    private static final String KEY_WEIGHT = "weight";

    final String font;
    final float scale;
    final String weight;

    private NativeUiPrefs(String font, float scale, String weight) {
        this.font = font;
        this.scale = clamp(scale, .85f, 1.30f);
        this.weight = weight;
    }

    static NativeUiPrefs load(@NonNull Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new NativeUiPrefs(
                p.getString(KEY_FONT, FONT_SYSTEM),
                p.getFloat(KEY_SCALE, 1.12f),
                p.getString(KEY_WEIGHT, WEIGHT_NORMAL));
    }

    void save(@NonNull Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FONT, font)
                .putFloat(KEY_SCALE, scale)
                .putString(KEY_WEIGHT, weight)
                .apply();
    }

    NativeUiPrefs withFont(String next) {
        return new NativeUiPrefs(next, scale, weight);
    }

    NativeUiPrefs withScale(float next) {
        return new NativeUiPrefs(font, next, weight);
    }

    NativeUiPrefs withWeight(String next) {
        return new NativeUiPrefs(font, scale, next);
    }

    static NativeUiPrefs defaults() {
        return new NativeUiPrefs(FONT_SYSTEM, 1.12f, WEIGHT_NORMAL);
    }

    float textSize(float baseSp) {
        return baseSp * scale;
    }

    Typeface typeface(boolean bold) {
        String family = FONT_SERIF.equals(font)
                ? "serif"
                : (FONT_MONO.equals(font) ? "monospace" : "sans-serif");
        int base = bold ? 700 : 400;
        int delta = WEIGHT_MEDIUM.equals(weight)
                ? 100
                : (WEIGHT_BOLD.equals(weight) ? 200 : 0);
        int target = Math.max(300, Math.min(850, base + delta));
        Typeface raw = Typeface.create(family, Typeface.NORMAL);
        return Typeface.create(raw, target, false);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
