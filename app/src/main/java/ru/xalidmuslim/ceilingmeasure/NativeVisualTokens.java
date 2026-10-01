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
 * Audited effective shell tokens from web v184.
 *
 * Drawing/gesture-specific colors and dimensions intentionally do not live here.
 */
final class NativeVisualTokens {
    static final int APP_BG = Color.rgb(242, 245, 248);          // #f2f5f8
    static final int APP_BG_TOP = Color.rgb(248, 250, 252);      // #f8fafc
    static final int SURFACE = Color.WHITE;                      // #ffffff
    static final int SURFACE_SOFT = Color.rgb(251, 252, 254);    // #fbfcfe
    static final int APP_SOFT = Color.rgb(245, 248, 250);        // #f5f8fa
    static final int DIVIDER = Color.rgb(229, 235, 239);         // #e5ebef
    static final int LINE = Color.rgb(216, 226, 234);            // #d8e2ea
    static final int LINE_STRONG = Color.rgb(203, 215, 224);     // #cbd7e0
    static final int TEXT = Color.rgb(30, 49, 68);               // #1e3144
    static final int TEXT_STRONG = Color.rgb(31, 52, 72);        // #1f3448
    static final int MUTED = Color.rgb(112, 130, 148);           // #708294
    static final int HOME_MUTED = Color.rgb(115, 134, 149);      // #738695
    static final int HOME_KICKER = Color.rgb(120, 144, 161);     // #7890a1
    static final int ACCENT = Color.rgb(47, 143, 190);           // #2f8fbe
    static final int APP_ACCENT = Color.rgb(47, 149, 194);       // #2f95c2
    static final int ACCENT_STRONG = Color.rgb(39, 125, 167);    // #277da7
    static final int ACCENT_SOFT = Color.rgb(237, 246, 251);     // #edf6fb
    static final int DANGER = Color.rgb(207, 71, 71);            // #cf4747
    static final int OK = Color.rgb(47, 154, 104);               // #2f9a68
    static final int WARN = Color.rgb(213, 138, 36);             // #d58a24
    static final int ERROR = Color.rgb(200, 76, 84);             // #c84c54

    static final int HOME_CARD_TEXT = Color.rgb(36, 59, 77);     // #243b4d
    static final int HOME_CARD_MUTED = Color.rgb(122, 141, 156); // #7a8d9c
    static final int HOME_CHEVRON = Color.rgb(154, 171, 183);    // #9aabb7
    static final int HOME_CARD_LINE = Color.rgb(219, 228, 235);  // #dbe4eb
    static final int HOME_SETTINGS_LINE = Color.rgb(220, 229, 235); // #dce5eb
    static final int HOME_ICON_BG = Color.rgb(237, 245, 249);    // #edf5f9
    static final int HOME_ICON = Color.rgb(45, 127, 169);        // #2d7fa9
    static final int HOME_PRIMARY_LINE = Color.rgb(45, 143, 190);// #2d8fbe
    static final int HOME_PRIMARY_TOP = Color.rgb(53, 153, 199); // #3599c7
    static final int HOME_PRIMARY_BOTTOM = Color.rgb(43, 131, 176);// #2b83b0

    static final int CARD_RADIUS_DP = 13;      // effective dense app-card radius
    static final int COMPACT_RADIUS_DP = 16;
    static final int PREMIUM_RADIUS_DP = 22;
    static final int CONTROL_RADIUS_DP = 11;
    static final int HISTORY_RADIUS_DP = 15;
    static final int HISTORY_BUTTON_RADIUS_DP = 10;
    static final int HOME_RADIUS_MOBILE_DP = 19;
    static final int HOME_RADIUS_WIDE_DP = 22;

    static final int APP_COMPACT_GAP_DP = 7;
    static final int HISTORY_BOTTOM_DP = 6;
    static final int HISTORY_BUTTON_WIDTH_DP = 68;
    static final int HISTORY_BUTTON_HEIGHT_DP = 44;
    static final int HISTORY_GAP_DP = 3;
    static final int HISTORY_PADDING_DP = 3;
    static final int HISTORY_WIDTH_DP =
            HISTORY_BUTTON_WIDTH_DP * 4 + HISTORY_GAP_DP * 3 + HISTORY_PADDING_DP * 2; // 287
    static final int HISTORY_HEIGHT_DP =
            HISTORY_BUTTON_HEIGHT_DP + HISTORY_PADDING_DP * 2; // 50
    static final int SCROLL_NAV_RESERVE_DP = 66;

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
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(55, 153, 200), Color.rgb(44, 137, 183)});
        d.setCornerRadius(radiusPx);
        d.setStroke(strokePx, ACCENT);
        return d;
    }

    static GradientDrawable homePrimary(float radiusPx, int strokePx) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{HOME_PRIMARY_TOP, HOME_PRIMARY_BOTTOM});
        d.setCornerRadius(radiusPx);
        d.setStroke(strokePx, HOME_PRIMARY_LINE);
        return d;
    }

    static Drawable homeBackground() {
        return new ShellGradientDrawable(true);
    }

    static Drawable workspaceBackground() {
        return new ShellGradientDrawable(false);
    }

    private static final class ShellGradientDrawable extends Drawable {
        private final boolean home;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int alpha = 255;

        ShellGradientDrawable(boolean home) {
            this.home = home;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            if (b.isEmpty()) return;

            int top = home ? Color.rgb(247, 249, 251) : APP_BG_TOP;
            int bottom = home ? Color.rgb(238, 243, 247) : APP_BG;
            paint.setShader(new LinearGradient(
                    b.left, b.top, b.left, b.bottom,
                    top, bottom, Shader.TileMode.CLAMP));
            paint.setAlpha(alpha);
            canvas.drawRect(b, paint);

            if (home) {
                float radius = Math.max(b.width(), b.height()) * .42f;
                paint.setShader(new RadialGradient(
                        b.left + b.width() * .85f,
                        b.top + b.height() * .04f,
                        Math.max(1f, radius),
                        new int[]{Color.argb(26, 62, 151, 193), Color.TRANSPARENT},
                        null,
                        Shader.TileMode.CLAMP));
                canvas.drawRect(b, paint);
            } else {
                float radiusLeft = Math.max(b.width(), b.height()) * .42f;
                paint.setShader(new RadialGradient(
                        b.left, b.top,
                        Math.max(1f, radiusLeft),
                        new int[]{Color.argb(20, 47, 143, 190), Color.TRANSPARENT},
                        null,
                        Shader.TileMode.CLAMP));
                canvas.drawRect(b, paint);

                float radiusRight = Math.max(b.width(), b.height()) * .34f;
                paint.setShader(new RadialGradient(
                        b.right, b.top,
                        Math.max(1f, radiusRight),
                        new int[]{Color.argb(13, 29, 70, 102), Color.TRANSPARENT},
                        null,
                        Shader.TileMode.CLAMP));
                canvas.drawRect(b, paint);
            }
            paint.setShader(null);
        }

        @Override
        public void setAlpha(int alpha) {
            this.alpha = Math.max(0, Math.min(255, alpha));
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }
}
