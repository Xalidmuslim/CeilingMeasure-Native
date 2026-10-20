package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

import org.json.JSONObject;

/**
 * v184-compatible typography preferences.
 *
 * The canonical persisted value is one JSON object under ceilingMeasureUiPrefsV1:
 * {version:2,font,scale,weight}. There is intentionally no night-mode field.
 */
final class NativeUiPrefs {
    static final String FONT_SYSTEM = "system";
    static final String FONT_SERIF = "serif";
    static final String FONT_MONO = "mono";

    static final String WEIGHT_NORMAL = "normal";
    static final String WEIGHT_MEDIUM = "medium";
    static final String WEIGHT_BOLD = "bold";

    static final String PREFS = "ceiling_ui_prefs";
    private static final String KEY_CANONICAL = NativeDataContract.UI_PREFS_STORAGE_KEY;

    // Pre-Phase-1 native direct keys. Read only as a one-time compatibility bridge.
    private static final String KEY_FONT = "font";
    private static final String KEY_SCALE = "scale";
    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_VERSION = "version";
    private static final String KEY_SIZE = "size";

    final int version;
    final String font;
    final float scale;
    final String weight;

    private NativeUiPrefs(int version, String font, float scale, String weight) {
        this.version = NativeDataContract.UI_PREFS_VERSION;
        this.font = normalizeFont(font);
        this.scale = clamp(scale, .85f, 1.30f);
        this.weight = normalizeWeight(weight);
    }

    static NativeUiPrefs load(@NonNull Context context) {
        return loadFrom(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    static NativeUiPrefs loadFrom(@NonNull SharedPreferences prefs) {
        JSONObject raw = null;
        String canonical = prefs.getString(KEY_CANONICAL, null);
        if (canonical != null && !canonical.isEmpty()) {
            try {
                raw = new JSONObject(canonical);
            } catch (Exception ignored) {
                raw = new JSONObject();
            }
        } else if (prefs.contains(KEY_FONT)
                || prefs.contains(KEY_SCALE)
                || prefs.contains(KEY_WEIGHT)
                || prefs.contains(KEY_VERSION)
                || prefs.contains(KEY_SIZE)) {
            raw = new JSONObject();
            try {
                if (prefs.contains(KEY_FONT)) raw.put("font", prefs.getString(KEY_FONT, FONT_SYSTEM));
                if (prefs.contains(KEY_SCALE)) raw.put("scale", prefs.getFloat(KEY_SCALE, 1.18f));
                if (prefs.contains(KEY_WEIGHT)) raw.put("weight", prefs.getString(KEY_WEIGHT, WEIGHT_NORMAL));
                if (prefs.contains(KEY_VERSION)) raw.put("version", prefs.getInt(KEY_VERSION, 1));
                if (prefs.contains(KEY_SIZE)) raw.put("size", prefs.getString(KEY_SIZE, "normal"));
            } catch (Exception ignored) {
            }
        }

        JSONObject canonicalObject = NativeDataContract.canonicalizeUiPrefs(raw);
        NativeUiPrefs result = fromJson(canonicalObject);

        // Persist canonical version 2 on load, exactly like web migration becomes
        // durable on the next settings/application persistence event.
        String nextText=canonicalObject.toString();
        // Dialogs can load typography dozens of times; avoid a disk write
        // (and SharedPreferences notifications) when nothing has changed.
        boolean needsMigration=prefs.contains(KEY_FONT)
                || prefs.contains(KEY_SCALE)
                || prefs.contains(KEY_WEIGHT)
                || prefs.contains(KEY_VERSION)
                || prefs.contains(KEY_SIZE);
        if(!nextText.equals(canonical)||needsMigration){
            prefs.edit()
                    .putString(KEY_CANONICAL,nextText)
                    .remove(KEY_FONT)
                    .remove(KEY_SCALE)
                    .remove(KEY_WEIGHT)
                    .remove(KEY_VERSION)
                    .remove(KEY_SIZE)
                    .apply();
        }
        return result;
    }

    void save(@NonNull Context context) {
        saveTo(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    void saveTo(@NonNull SharedPreferences prefs) {
        prefs.edit()
                .putString(KEY_CANONICAL, toJson().toString())
                .remove(KEY_FONT)
                .remove(KEY_SCALE)
                .remove(KEY_WEIGHT)
                .remove(KEY_VERSION)
                .remove(KEY_SIZE)
                .apply();
    }

    NativeUiPrefs withFont(String next) {
        return new NativeUiPrefs(version, next, scale, weight);
    }

    NativeUiPrefs withScale(float next) {
        return new NativeUiPrefs(version, font, next, weight);
    }

    NativeUiPrefs withWeight(String next) {
        return new NativeUiPrefs(version, font, scale, next);
    }

    static NativeUiPrefs defaults() {
        return new NativeUiPrefs(
                NativeDataContract.UI_PREFS_VERSION,
                FONT_SYSTEM,
                1.18f,
                WEIGHT_NORMAL);
    }

    JSONObject toJson() {
        JSONObject out = new JSONObject();
        try {
            out.put("version", NativeDataContract.UI_PREFS_VERSION);
            out.put("font", font);
            out.put("scale", scale);
            out.put("weight", weight);
        } catch (Exception ignored) {
        }
        return out;
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

    private static NativeUiPrefs fromJson(JSONObject object) {
        return new NativeUiPrefs(
                object.optInt("version", NativeDataContract.UI_PREFS_VERSION),
                object.optString("font", FONT_SYSTEM),
                (float) object.optDouble("scale", 1.18d),
                object.optString("weight", WEIGHT_NORMAL));
    }

    private static String normalizeFont(String value) {
        if ("georgia".equals(value)) return FONT_SERIF;
        if ("arial".equals(value)) return FONT_SYSTEM;
        if (FONT_SERIF.equals(value) || FONT_MONO.equals(value)) return value;
        return FONT_SYSTEM;
    }

    private static String normalizeWeight(String value) {
        if (WEIGHT_MEDIUM.equals(value) || WEIGHT_BOLD.equals(value)) return value;
        return WEIGHT_NORMAL;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
