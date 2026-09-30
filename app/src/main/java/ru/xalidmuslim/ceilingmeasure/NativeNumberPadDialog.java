package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;

final class NativeNumberPadDialog extends Dialog {
    interface Listener {
        void onValue(float value);
    }

    private static final int TEXT = Color.rgb(35, 56, 72);
    private static final int MUTED = Color.rgb(116, 136, 151);
    private static final int BLUE = Color.rgb(47, 143, 190);
    private static final int LINE = Color.rgb(220, 229, 235);
    private static final int PANEL = Color.WHITE;
    private static final int BG = Color.rgb(246, 248, 250);

    private final String titleText;
    private final float initialValue;
    private final Listener listener;
    private TextView display;
    private String buffer;

    NativeNumberPadDialog(
            @NonNull Context context,
            @NonNull String title,
            float initialValue,
            @NonNull Listener listener) {
        super(context);
        this.titleText = title;
        this.initialValue = initialValue;
        this.listener = listener;
        this.buffer = initialValue > 0 ? format(initialValue) : "0";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell = new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(16), dp(14), dp(16), dp(16));
        shell.setBackground(rounded(PANEL, LINE, 20, 1));

        TextView title = text(titleText, 17, TEXT, Typeface.BOLD);
        shell.addView(title);

        TextView hint = text("Введите размер", 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(3);
        shell.addView(hint, hintParams);

        display = text(buffer, 30, TEXT, Typeface.BOLD);
        display.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        display.setPadding(dp(12), 0, dp(12), 0);
        display.setTag("native_number_pad_display");
        display.setBackground(rounded(BG, LINE, 12, 1));
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56));
        displayParams.topMargin = dp(12);
        shell.addView(display, displayParams);

        LinearLayout keypad = new LinearLayout(getContext());
        keypad.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams keypadParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        keypadParams.topMargin = dp(10);
        shell.addView(keypad, keypadParams);

        addRow(keypad, "1", "2", "3");
        addRow(keypad, "4", "5", "6");
        addRow(keypad, "7", "8", "9");
        // Phone layout: utility / 0 / backspace on the last row.
        addRow(keypad, ",", "0", "⌫");

        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48));
        actionsParams.topMargin = dp(10);
        shell.addView(actions, actionsParams);

        Button cancel = actionButton("Отмена", false);
        cancel.setTag("native_number_pad_cancel");
        cancel.setOnClickListener(v -> dismiss());

        Button done = actionButton("Готово", true);
        done.setTag("native_number_pad_done");
        done.setOnClickListener(v -> {
            try {
                float value = Float.parseFloat(buffer.replace(',', '.'));
                listener.onValue(value);
                dismiss();
            } catch (Exception ignored) {
                buffer = "0";
                updateDisplay();
            }
        });

        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(48), 1f);
        left.rightMargin = dp(4);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(48), 1f);
        right.leftMargin = dp(4);
        actions.addView(cancel, left);
        actions.addView(done, right);

        setContentView(shell);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = Math.min(
                    getContext().getResources().getDisplayMetrics().widthPixels - dp(24),
                    dp(360));
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    private void addRow(LinearLayout parent, String a, String b, String c) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60));
        rowParams.topMargin = dp(5);
        parent.addView(row, rowParams);

        addKey(row, a);
        addKey(row, b);
        addKey(row, c);
    }

    private void addKey(LinearLayout row, String value) {
        Button key = new Button(getContext());
        key.setAllCaps(false);
        key.setText(value);
        key.setTextSize(value.equals("⌫") ? 22 : 24);
        key.setTextColor(TEXT);
        key.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        key.setGravity(Gravity.CENTER);
        key.setPadding(0, 0, 0, 0);
        key.setBackground(rounded(BG, LINE, 18, 1));
        key.setTag("native_number_pad_" + keyTag(value));
        key.setOnClickListener(v -> press(value));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(60), 1f);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        row.addView(key, params);
    }

    private void press(String value) {
        if ("⌫".equals(value)) {
            if (buffer.length() <= 1) buffer = "0";
            else buffer = buffer.substring(0, buffer.length() - 1);
            updateDisplay();
            return;
        }

        if (",".equals(value)) {
            if (!buffer.contains(",") && !buffer.contains(".")) buffer += ",";
            updateDisplay();
            return;
        }

        if ("0".equals(buffer)) buffer = value;
        else if (buffer.length() < 9) buffer += value;
        updateDisplay();
    }

    private void updateDisplay() {
        if (display != null) display.setText(buffer);
    }

    private Button actionButton(String value, boolean primary) {
        Button button = new Button(getContext());
        button.setAllCaps(false);
        button.setText(value);
        button.setTextSize(14);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        button.setTextColor(primary ? Color.WHITE : TEXT);
        button.setBackground(rounded(primary ? BLUE : BG, primary ? BLUE : LINE, 11, 1));
        return button;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        view.setIncludeFontPadding(false);
        return view;
    }

    private GradientDrawable rounded(int fill, int stroke, int radius, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), stroke);
        return drawable;
    }

    private String keyTag(String value) {
        if (",".equals(value)) return "decimal";
        if ("⌫".equals(value)) return "backspace";
        return value;
    }

    private String format(float value) {
        return Math.abs(value - Math.round(value)) < .01f
                ? String.valueOf(Math.round(value))
                : String.format(Locale.US, "%.1f", value);
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
