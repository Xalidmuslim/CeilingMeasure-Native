package ru.xalidmuslim.ceilingmeasure;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Professional engineering-minimalism palette (v231).
 *
 * Drawing/gesture-specific colors and dimensions intentionally do not live here.
 */
final class NativeVisualTokens {
    static final int APP_BG = Color.rgb(246, 248, 250);          // #f2f5f8
    static final int APP_BG_TOP = Color.rgb(246, 248, 250);      // #f8fafc
    static final int SURFACE = Color.WHITE;                      // #ffffff
    static final int SURFACE_SOFT = Color.rgb(250, 251, 252);    // #fbfcfe
    static final int APP_SOFT = Color.rgb(241, 245, 247);        // #f5f8fa
    static final int DIVIDER = Color.rgb(230, 237, 242);         // #e5ebef
    static final int LINE = Color.rgb(223, 230, 236);            // #d8e2ea
    static final int LINE_STRONG = Color.rgb(199, 211, 219);     // #cbd7e0
    static final int TEXT = Color.rgb(32, 52, 69);               // #1e3144
    static final int TEXT_STRONG = Color.rgb(32, 52, 69);        // #1f3448
    static final int MUTED = Color.rgb(88, 108, 125);           // #708294
    static final int HOME_MUTED = Color.rgb(88, 108, 125);      // #738695
    static final int HOME_KICKER = Color.rgb(88, 108, 125);     // #7890a1
    static final int ACCENT = Color.rgb(33, 110, 147);           // #2f8fbe
    static final int APP_ACCENT = Color.rgb(33, 110, 147);       // #2f95c2
    static final int ACCENT_STRONG = Color.rgb(28, 92, 122);    // #277da7
    static final int ACCENT_SOFT = Color.rgb(233, 243, 248);     // #edf6fb
    static final int DANGER = Color.rgb(207, 71, 71);            // #cf4747
    static final int OK = Color.rgb(47, 154, 104);               // #2f9a68
    static final int WARN = Color.rgb(213, 138, 36);             // #d58a24
    static final int ERROR = Color.rgb(200, 76, 84);             // #c84c54

    static final int HOME_CARD_TEXT = Color.rgb(32, 52, 69);     // #243b4d
    static final int HOME_CARD_MUTED = Color.rgb(88, 108, 125); // #7a8d9c
    static final int HOME_CHEVRON = Color.rgb(88, 108, 125);    // #9aabb7
    static final int HOME_CARD_LINE = Color.rgb(223, 230, 236);  // #dbe4eb
    static final int HOME_SETTINGS_LINE = Color.rgb(223, 230, 236); // #dce5eb
    static final int HOME_ICON_BG = Color.rgb(233, 243, 248);    // #edf5f9
    static final int HOME_ICON = Color.rgb(33, 110, 147);        // #2d7fa9
    static final int HOME_PRIMARY_LINE = Color.rgb(33, 110, 147);// #2d8fbe
    static final int HOME_PRIMARY_TOP = Color.rgb(33, 110, 147); // #3599c7
    static final int HOME_PRIMARY_BOTTOM = Color.rgb(33, 110, 147);// #2b83b0

    static final int CARD_RADIUS_DP = 16;      // effective dense app-card radius
    static final int COMPACT_RADIUS_DP = 12;
    static final int PREMIUM_RADIUS_DP = 18;
    static final int CONTROL_RADIUS_DP = 11;
    static final int HISTORY_RADIUS_DP = 20;
    static final int HISTORY_BUTTON_RADIUS_DP = 14;
    static final int HOME_RADIUS_MOBILE_DP = 16;
    static final int HOME_RADIUS_WIDE_DP = 18;

    static final int APP_COMPACT_GAP_DP = 7;
    static final int HISTORY_BOTTOM_DP = 10;
    static final int HISTORY_BUTTON_WIDTH_DP = 78;
    static final int HISTORY_BUTTON_HEIGHT_DP = 53;
    static final int HISTORY_GAP_DP = 4;
    static final int HISTORY_PADDING_DP = 3;
    static final int HISTORY_WIDTH_DP =
            HISTORY_BUTTON_WIDTH_DP * 4 + HISTORY_GAP_DP * 3 + HISTORY_PADDING_DP * 2; // 287
    static final int HISTORY_HEIGHT_DP =
            HISTORY_BUTTON_HEIGHT_DP + HISTORY_PADDING_DP * 2; // 50
    static final int SCROLL_NAV_RESERVE_DP = 92;

    static final long OVERLAY_MOTION_MS = 115L;
    static final long COLLAPSE_MOTION_MS = 145L;

    private NativeVisualTokens() {}

    static GradientDrawable solid(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    static GradientDrawable outlined(int fill, int stroke, float radiusPx, int strokePx) {
        GradientDrawable d = solid(fill, radiusPx);
        if (strokePx > 0) d.setStroke(strokePx, stroke);
        return d;
    }

    static Drawable rippleOutlined(
            int fill,
            int stroke,
            float radiusPx,
            int strokePx,
            int rippleColor) {
        GradientDrawable content = outlined(fill, stroke, radiusPx, strokePx);
        GradientDrawable mask = solid(Color.WHITE, radiusPx);
        return new RippleDrawable(
                ColorStateList.valueOf(rippleColor),
                content,
                mask);
    }

    static Drawable controlButton(boolean primary, float radiusPx) {
        GradientDrawable content = primary
                ? primaryButton(radiusPx, 1)
                : outlined(SURFACE, LINE, radiusPx, 1);
        int ripple = primary
                ? Color.argb(44, 255, 255, 255)
                : Color.argb(24, 30, 49, 68);
        return new RippleDrawable(
                ColorStateList.valueOf(ripple),
                content,
                solid(Color.WHITE, radiusPx));
    }

    static GradientDrawable primaryButton(float radiusPx, int strokePx) {
        GradientDrawable d = solid(ACCENT, radiusPx);
        if (strokePx > 0) d.setStroke(strokePx, ACCENT);
        return d;
    }

    static GradientDrawable homePrimary(float radiusPx, int strokePx) {
        return primaryButton(radiusPx, strokePx);
    }

    static Drawable homeBackground() {
        return solid(APP_BG, 0f);
    }

    static Drawable workspaceBackground() {
        return solid(APP_BG, 0f);
    }
}
