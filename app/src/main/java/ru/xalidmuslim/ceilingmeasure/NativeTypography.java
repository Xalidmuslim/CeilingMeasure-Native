package ru.xalidmuslim.ceilingmeasure;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

/** Shell-only live typography application for v184 UI preferences. */
final class NativeTypography {
    static final class Meta {
        final float baseSp;
        final boolean strong;
        Meta(float baseSp, boolean strong) {
            this.baseSp = baseSp;
            this.strong = strong;
        }
    }

    private NativeTypography() {}

    static <T extends TextView> T register(
            @NonNull T view,
            float baseSp,
            boolean strong,
            @NonNull NativeUiPrefs prefs) {
        view.setTag(R.id.native_typography_meta, new Meta(baseSp, strong));
        apply(view, prefs);
        return view;
    }

    static void applyTree(@NonNull View root, @NonNull NativeUiPrefs prefs) {
        if (root instanceof TextView) {
            Object raw = root.getTag(R.id.native_typography_meta);
            if (raw instanceof Meta) apply((TextView) root, prefs);
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTree(group.getChildAt(i), prefs);
            }
        }
    }

    static Typeface typeface(@NonNull NativeUiPrefs prefs, boolean strong) {
        String family = NativeUiPrefs.FONT_SERIF.equals(prefs.font)
                ? "serif"
                : (NativeUiPrefs.FONT_MONO.equals(prefs.font)
                        ? "monospace"
                        : "sans-serif");
        int base = strong ? 650 : 400;
        int delta = NativeUiPrefs.WEIGHT_MEDIUM.equals(prefs.weight)
                ? 100
                : (NativeUiPrefs.WEIGHT_BOLD.equals(prefs.weight) ? 200 : 0);
        int target = Math.max(300, Math.min(850, base + delta));
        return Typeface.create(Typeface.create(family, Typeface.NORMAL), target, false);
    }

    private static void apply(@NonNull TextView view, @NonNull NativeUiPrefs prefs) {
        Meta meta = (Meta) view.getTag(R.id.native_typography_meta);
        if (meta == null) return;
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, meta.baseSp * prefs.scale);
        view.setTypeface(typeface(prefs, meta.strong));
    }
}
