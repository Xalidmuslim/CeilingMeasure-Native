package ru.xalidmuslim.ceilingmeasure;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class NativeHomeActivity extends ComponentActivity {
    private static final int BG = Color.rgb(244, 247, 249);
    private long lastBackAtMs = 0L;
    private static final int TEXT = Color.rgb(35, 56, 72);
    private static final int MUTED = Color.rgb(116, 136, 151);
    private static final int BLUE = Color.rgb(47, 143, 190);
    private static final int LINE = Color.rgb(220, 229, 235);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContent());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long now = android.os.SystemClock.elapsedRealtime();
                if (now - lastBackAtMs <= 1500L) {
                    lastBackAtMs = 0L;
                    moveTaskToBack(true);
                    return;
                }

                lastBackAtMs = now;
                Toast.makeText(
                        NativeHomeActivity.this,
                        "Ещё раз назад — свернуть приложение",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private View createContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(18), dp(24), dp(18), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams shellParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        shellParams.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(520));
        content.addView(shell, shellParams);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.TOP);
        shell.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        head.addView(titles, titleParams);

        TextView kicker = text("РАБОЧЕЕ ПРИЛОЖЕНИЕ", 11, MUTED, Typeface.BOLD);
        kicker.setLetterSpacing(0.06f);
        titles.addView(kicker);

        TextView title = text("Замер потолков", 28, TEXT, Typeface.BOLD);
        LinearLayout.LayoutParams mainTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mainTitleParams.topMargin = dp(5);
        titles.addView(title, mainTitleParams);

        TextView subtitle = text("Замер · чертёж · смета", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(6);
        titles.addView(subtitle, subtitleParams);

        TextView settings = text("⚙", 22, TEXT, Typeface.NORMAL);
        settings.setGravity(Gravity.CENTER);
        settings.setContentDescription("Настройки");
        settings.setTag("native_home_settings");
        settings.setClickable(true);
        settings.setFocusable(true);
        settings.setBackground(roundedRipple(Color.WHITE, LINE, dp(14), false));
        settings.setOnClickListener(v -> openWorkspace(NativeWorkspaceActivity.ACTION_SETTINGS));
        head.addView(settings, sizeParams(44, 44));

        LinearLayout.LayoutParams actionsMargin = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsMargin.topMargin = dp(28);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        shell.addView(actions, actionsMargin);

        View newOrder = actionCard(
                "＋",
                "Новый заказ",
                "Создать новый замер",
                true,
                "native_home_new",
                v -> openWorkspace(NativeWorkspaceActivity.ACTION_NEW_ORDER));
        actions.addView(newOrder, cardParams(0));

        View saved = actionCard(
                "▣",
                "Замеры",
                "Открыть сохранённые заказы",
                false,
                "native_home_saved",
                v -> openWorkspace(NativeWorkspaceActivity.ACTION_SAVED_MEASUREMENTS));
        actions.addView(saved, cardParams(10));

        View drawing = actionCard(
                "⌗",
                "Чертёж",
                "Продолжить текущий чертёж",
                false,
                "native_home_drawing",
                v -> openWorkspace(NativeWorkspaceActivity.ACTION_DRAWING));
        actions.addView(drawing, cardParams(10));

        ViewCompat.requestApplyInsets(scroll);
        return scroll;
    }

    private View actionCard(
            String icon,
            String title,
            String subtitle,
            boolean primary,
            String tag,
            View.OnClickListener listener) {
        int bg = primary ? BLUE : Color.WHITE;
        int fg = primary ? Color.WHITE : TEXT;
        int secondary = primary ? Color.argb(205, 255, 255, 255) : MUTED;
        int stroke = primary ? BLUE : LINE;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(15), dp(15), dp(14), dp(15));
        card.setMinimumHeight(dp(primary ? 112 : 94));
        card.setTag(tag);
        card.setContentDescription(title);
        card.setClickable(true);
        card.setFocusable(true);
        card.setElevation(dp(primary ? 8 : 4));
        card.setBackground(roundedRipple(bg, stroke, dp(21), primary));
        card.setOnClickListener(listener);

        TextView iconView = text(icon, primary ? 30 : 27, primary ? Color.WHITE : BLUE, Typeface.NORMAL);
        iconView.setGravity(Gravity.CENTER);
        int iconBg = primary ? Color.argb(42, 255, 255, 255) : Color.rgb(237, 245, 249);
        iconView.setBackground(rounded(iconBg, iconBg, dp(15), 0));
        card.addView(iconView, sizeParams(48, 48));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        copyParams.leftMargin = dp(13);
        card.addView(copy, copyParams);

        TextView titleView = text(title, 18, fg, Typeface.BOLD);
        copy.addView(titleView);

        TextView subtitleView = text(subtitle, 12, secondary, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(4);
        copy.addView(subtitleView, subtitleParams);

        TextView arrow = text("›", 30, primary ? Color.argb(205,255,255,255) : Color.rgb(151,169,181), Typeface.NORMAL);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, sizeParams(28, 48));

        return card;
    }

    private LinearLayout.LayoutParams cardParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMarginDp);
        return params;
    }

    private LinearLayout.LayoutParams sizeParams(int widthDp, int heightDp) {
        return new LinearLayout.LayoutParams(dp(widthDp), dp(heightDp));
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        view.setIncludeFontPadding(false);
        return view;
    }

    private RippleDrawable roundedRipple(
            int fill,
            int stroke,
            int radius,
            boolean primary) {
        GradientDrawable content = rounded(fill, stroke, radius, dp(1));
        GradientDrawable mask = rounded(Color.WHITE, Color.WHITE, radius, 0);
        int ripple = primary ? Color.argb(40, 255, 255, 255) : Color.argb(24, 35, 56, 72);
        return new RippleDrawable(ColorStateList.valueOf(ripple), content, mask);
    }

    private GradientDrawable rounded(int fill, int stroke, int radius, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, stroke);
        return drawable;
    }

    private void openWorkspace(String action) {
        Intent intent = new Intent(this, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION, action);
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
