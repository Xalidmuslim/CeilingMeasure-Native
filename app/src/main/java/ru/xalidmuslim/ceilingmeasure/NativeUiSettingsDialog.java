package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** v184 interface-settings overlay shared by Home and Workspace. */
final class NativeUiSettingsDialog extends Dialog {
    interface Listener {
        void onPrefsChanged(@NonNull NativeUiPrefs prefs);
    }

    private final Activity activity;
    private final Listener listener;
    private NativeUiPrefs prefs;
    private LinearLayout sheet;
    private TextView sizeValue;
    private boolean closing;
    private final Handler scaleHandler=new Handler(Looper.getMainLooper());
    private Runnable pendingScaleApply;

    NativeUiSettingsDialog(
            @NonNull Activity activity,
            @NonNull NativeUiPrefs initial,
            @NonNull Listener listener) {
        super(activity);
        this.activity = activity;
        this.prefs = initial;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Fade-only dialog transition. Never slide or scale the window.
            window.setWindowAnimations(R.style.CeilingFadeWindowAnimation);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowCompat.setDecorFitsSystemWindows(window, false);
        }
        rebuild();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        View decor = window == null ? null : window.getDecorView();
        if (decor != null) decor.requestApplyInsets();
        if (sheet != null) {
            sheet.animate().cancel();
            sheet.setScaleX(1f);
            sheet.setScaleY(1f);
            sheet.setTranslationY(0f);
            sheet.setAlpha(.94f);
            sheet.animate()
                    .alpha(1f)
                    .setDuration(NativeVisualTokens.OVERLAY_MOTION_MS)
                    .start();
        }
    }

    @Override
    public void onBackPressed() {
        dismissAnimated();
    }

    NativeUiPrefs prefsForTest() {
        return prefs;
    }

    void setScaleForTest(float scale) {
        changePrefs(prefs.withScale(scale), false);
        if (sheet != null) NativeTypography.applyTree(sheet, prefs);
        if (sizeValue != null) sizeValue.setText(Math.round(prefs.scale * 100f) + "%");
    }

    @Override
    public void dismiss() {
        if(pendingScaleApply!=null){
            scaleHandler.removeCallbacks(pendingScaleApply);
            pendingScaleApply=null;
        }
        if (closing) return;
        super.dismiss();
    }

    private void dismissAnimated() {
        if (closing) return;
        closing = true;
        if (sheet == null) {
            super.dismiss();
            return;
        }
        sheet.animate()
                .alpha(0f)
                .setDuration(NativeVisualTokens.OVERLAY_MOTION_MS)
                .withEndAction(() -> NativeUiSettingsDialog.super.dismiss())
                .start();
    }

    private void rebuild() {
        FrameLayout backdrop = new FrameLayout(activity);
        backdrop.setTag("native_ui_settings_backdrop");
        backdrop.setBackgroundColor(Color.argb(31, 17, 29, 42));
        backdrop.setClickable(true);
        backdrop.setOnClickListener(v -> dismissAnimated());

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setTag("native_ui_settings_scroll");
        scroll.setOnClickListener(v -> {
            // Consume clicks inside the sheet area.
        });

        sheet = new LinearLayout(activity);
        sheet.setTag("native_ui_settings_sheet");
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(10), dp(10), dp(10), dp(10));
        sheet.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,
                NativeVisualTokens.LINE,
                dp(18),
                dp(1)));
        sheet.setElevation(dp(2));
        sheet.setClickable(true);

        int widthDp = Math.round(
                activity.getResources().getDisplayMetrics().widthPixels
                        / activity.getResources().getDisplayMetrics().density);
        int width = dp(Math.min(430, Math.max(300, widthDp - 18)));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        scrollParams.setMargins(dp(9), dp(9), dp(9), dp(9));
        scroll.addView(sheet, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        backdrop.addView(scroll, scrollParams);

        ViewCompat.setOnApplyWindowInsetsListener(backdrop, (view, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            int side = dp(9);
            view.setPadding(
                    Math.max(side, safe.left),
                    Math.max(side, safe.top),
                    Math.max(side, safe.right),
                    Math.max(side, safe.bottom));
            return insets;
        });

        addHeader();
        addFontGroup();
        addSizeGroup();
        addWeightGroup();
        addPreview();
        addActions();

        setContentView(backdrop);
        ViewCompat.requestApplyInsets(backdrop);
        NativeTypography.applyTree(sheet, prefs);
    }

    private void addHeader() {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP);
        sheet.addView(header, matchWrap());

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        header.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = text("Настройки", 18, NativeVisualTokens.TEXT, true);
        copy.addView(title);
        TextView sub = text(
                "Изменения применяются сразу ко всему приложению",
                11,
                NativeVisualTokens.MUTED,
                false);
        LinearLayout.LayoutParams subParams = wrapWrap();
        subParams.topMargin = dp(3);
        copy.addView(sub, subParams);

        Button close = choiceButton("×", false);
        close.setTag("native_ui_settings_close");
        close.setContentDescription("Закрыть");
        close.setTextSize(24);
        close.setOnClickListener(v -> dismissAnimated());
        LinearLayout.LayoutParams closeParams =
                new LinearLayout.LayoutParams(dp(38), dp(38));
        closeParams.leftMargin = dp(8);
        header.addView(close, closeParams);
    }

    private void addFontGroup() {
        LinearLayout group = group("Шрифт", null);
        LinearLayout row = threeColumnRow();
        group.addView(row, topMargin(dp(5)));

        addChoice(row, "Aa\nСистемный", "native_ui_font_system",
                NativeUiPrefs.FONT_SYSTEM.equals(prefs.font),
                () -> changePrefs(prefs.withFont(NativeUiPrefs.FONT_SYSTEM), true));
        addChoice(row, "Aa\nС засечками", "native_ui_font_serif",
                NativeUiPrefs.FONT_SERIF.equals(prefs.font),
                () -> changePrefs(prefs.withFont(NativeUiPrefs.FONT_SERIF), true));
        addChoice(row, "Aa\nМоно", "native_ui_font_mono",
                NativeUiPrefs.FONT_MONO.equals(prefs.font),
                () -> changePrefs(prefs.withFont(NativeUiPrefs.FONT_MONO), true));
    }

    private void addSizeGroup() {
        LinearLayout group = group(
                "Размер текста",
                "Меняется плавно во всём приложении");

        LinearLayout head = new LinearLayout(activity);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView small = text("Aa", 12, NativeVisualTokens.MUTED, true);
        TextView large = text("Aa", 21, NativeVisualTokens.MUTED, true);
        head.addView(small, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
        View spacer = new View(activity);
        head.addView(spacer, new LinearLayout.LayoutParams(0, dp(1), 1f));
        large.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        head.addView(large, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
        group.addView(head, topMargin(dp(5)));

        // Keep the percentage indicator independent of the live text scale.
        // Previously it was squeezed between the two "Aa" labels and grew with
        // the global scale, which is why 112% visually drifted out of place.
        LinearLayout valueRow = new LinearLayout(activity);
        valueRow.setGravity(Gravity.CENTER);
        sizeValue = new TextView(activity);
        sizeValue.setText(Math.round(prefs.scale * 100f) + "%");
        sizeValue.setTextSize(13);
        sizeValue.setTextColor(Color.rgb(40, 107, 141));
        sizeValue.setTypeface(android.graphics.Typeface.create(
                "sans-serif",
                android.graphics.Typeface.BOLD));
        sizeValue.setIncludeFontPadding(false);
        sizeValue.setTag("native_ui_size_value");
        sizeValue.setGravity(Gravity.CENTER);
        sizeValue.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.ACCENT_SOFT,
                NativeVisualTokens.LINE,
                dp(11),
                dp(1)));
        valueRow.addView(sizeValue, new LinearLayout.LayoutParams(dp(66), dp(34)));
        group.addView(valueRow, topMargin(dp(2)));

        SeekBar slider = new SeekBar(activity);
        slider.setTag("native_ui_size_slider");
        slider.setMax(45);
        slider.setProgress(Math.round(prefs.scale * 100f) - 85);
        slider.setProgressTintList(ColorStateList.valueOf(NativeVisualTokens.APP_ACCENT));
        slider.setThumbTintList(ColorStateList.valueOf(NativeVisualTokens.APP_ACCENT));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int percent = 85 + progress;
                sizeValue.setText(percent + "%");
                if (fromUser) {
                    if(pendingScaleApply!=null)scaleHandler.removeCallbacks(pendingScaleApply);
                    pendingScaleApply=()->{
                        pendingScaleApply=null;
                        applyLiveScale(percent);
                    };
                    scaleHandler.postDelayed(pendingScaleApply,55L);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {
                if(pendingScaleApply!=null){
                    scaleHandler.removeCallbacks(pendingScaleApply);
                    pendingScaleApply=null;
                }
                applyLiveScale(85+bar.getProgress());
            }
        });
        group.addView(slider, topMargin(dp(4)));

        LinearLayout marks = new LinearLayout(activity);
        marks.setOrientation(LinearLayout.HORIZONTAL);
        marks.setGravity(Gravity.CENTER_VERTICAL);
        marks.addView(text("85%", 10, NativeVisualTokens.MUTED, false), weightedWrap());
        TextView hundred = text("100%", 10, NativeVisualTokens.MUTED, false);
        hundred.setGravity(Gravity.CENTER);
        marks.addView(hundred, weightedWrap());
        TextView max = text("130%", 10, NativeVisualTokens.MUTED, false);
        max.setGravity(Gravity.END);
        marks.addView(max, weightedWrap());
        group.addView(marks);
    }

    private void addWeightGroup() {
        LinearLayout group = group(
                "Жирность",
                "Меняет базовую жирность, сохраняя разницу между обычным текстом и заголовками");
        LinearLayout row = threeColumnRow();
        group.addView(row, topMargin(dp(5)));

        addChoice(row, "Aa\nОбычная", "native_ui_weight_normal",
                NativeUiPrefs.WEIGHT_NORMAL.equals(prefs.weight),
                () -> changePrefs(prefs.withWeight(NativeUiPrefs.WEIGHT_NORMAL), true));
        addChoice(row, "Aa\nСредняя", "native_ui_weight_medium",
                NativeUiPrefs.WEIGHT_MEDIUM.equals(prefs.weight),
                () -> changePrefs(prefs.withWeight(NativeUiPrefs.WEIGHT_MEDIUM), true));
        addChoice(row, "Aa\nЖирная", "native_ui_weight_bold",
                NativeUiPrefs.WEIGHT_BOLD.equals(prefs.weight),
                () -> changePrefs(prefs.withWeight(NativeUiPrefs.WEIGHT_BOLD), true));
    }

    private void addPreview() {
        LinearLayout preview = new LinearLayout(activity);
        preview.setTag("native_ui_preview");
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(dp(10), dp(9), dp(10), dp(9));
        preview.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE_SOFT,
                NativeVisualTokens.LINE,
                dp(12),
                dp(1)));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(6);
        sheet.addView(preview, params);

        preview.addView(text("Предпросмотр", 10, NativeVisualTokens.MUTED, false));
        preview.addView(text("Комната 1 · 28,00 м²", 16, NativeVisualTokens.TEXT, true));
        preview.addView(text(
                "Калькулятор, смета, кнопки и чертёж меняются сразу.",
                11,
                NativeVisualTokens.MUTED,
                false));
    }

    private void addActions() {
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(7);
        sheet.addView(actions, params);

        Button reset = choiceButton("По умолчанию", false);
        reset.setTag("native_ui_reset");
        reset.setOnClickListener(v -> changePrefs(NativeUiPrefs.defaults(), true));

        Button done = choiceButton("Готово", true);
        done.setTag("native_ui_done");
        done.setOnClickListener(v -> dismissAnimated());

        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(
                0, dp(40), 1f);
        left.rightMargin = dp(4);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(
                0, dp(40), 1f);
        right.leftMargin = dp(4);
        actions.addView(reset, left);
        actions.addView(done, right);
    }

    private LinearLayout group(String label, String hint) {
        LinearLayout group = new LinearLayout(activity);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, dp(8), 0, dp(8));
        group.setBackground(NativeVisualTokens.outlined(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                0,
                0));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(2);
        sheet.addView(group, params);

        TextView labelView = text(label, 12, NativeVisualTokens.TEXT, true);
        group.addView(labelView);
        if (hint != null) {
            TextView hintView = text(hint, 10, NativeVisualTokens.MUTED, false);
            LinearLayout.LayoutParams hp = wrapWrap();
            hp.topMargin = dp(2);
            group.addView(hintView, hp);
        }

        View divider = new View(activity);
        divider.setBackgroundColor(NativeVisualTokens.DIVIDER);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.topMargin = dp(7);
        group.addView(divider, dividerParams);
        return group;
    }

    private void addChoice(
            LinearLayout row,
            String label,
            String tag,
            boolean active,
            Runnable action) {
        Button button = choiceButton(label, active);
        button.setTag(tag);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(48), 1f);
        params.leftMargin = dp(3);
        params.rightMargin = dp(3);
        row.addView(button, params);
    }

    private Button choiceButton(String label, boolean active) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(active ? Color.WHITE : NativeVisualTokens.TEXT);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(7), 0, dp(7), 0);
        NativeTypography.register(button, 10.5f, true, prefs);
        if (active) {
            button.setBackground(NativeVisualTokens.primaryButton(dp(12), 0));
        } else {
            button.setBackground(NativeVisualTokens.outlined(
                    NativeVisualTokens.APP_SOFT,
                    Color.TRANSPARENT,
                    dp(12),
                    0));
        }
        return button;
    }

    private TextView text(String value, float sp, int color, boolean strong) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        return NativeTypography.register(view, sp, strong, prefs);
    }

    private void applyLiveScale(int percent) {
        if(closing || !isShowing())return;
        float scale=Math.max(.85f,Math.min(1.30f,percent/100f));
        if(Math.abs(prefs.scale-scale)<.0001f)return;
        changePrefs(prefs.withScale(scale),false);
        if(sheet!=null)NativeTypography.applyTree(sheet,prefs);
    }

    private void changePrefs(@NonNull NativeUiPrefs next, boolean rebuildChoices) {
        prefs = next;
        prefs.save(activity);
        listener.onPrefsChanged(prefs);
        if (rebuildChoices) rebuild();
    }

    private LinearLayout threeColumnRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weightedWrap() {
        return new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams topMargin(int marginPx) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = marginPx;
        return params;
    }

    private int dp(int value) {
        return Math.round(
                value * activity.getResources().getDisplayMetrics().density);
    }
}
