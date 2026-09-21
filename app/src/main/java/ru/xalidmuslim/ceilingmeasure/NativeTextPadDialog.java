package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

final class NativeTextPadDialog extends Dialog {
    interface Listener {
        void onValue(String value);
    }

    private static final int TEXT = Color.rgb(35, 56, 72);
    private static final int MUTED = Color.rgb(116, 136, 151);
    private static final int BLUE = Color.rgb(47, 143, 190);
    private static final int LINE = Color.rgb(220, 229, 235);
    private static final int PANEL = Color.WHITE;
    private static final int BG = Color.rgb(246, 248, 250);

    private final String titleText;
    private final Listener listener;
    private final StringBuilder buffer = new StringBuilder();
    private TextView display;

    NativeTextPadDialog(
            @NonNull Context context,
            @NonNull String title,
            String initialValue,
            @NonNull Listener listener) {
        super(context);
        this.titleText = title;
        this.listener = listener;
        if (initialValue != null) buffer.append(initialValue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell = new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(12), dp(12), dp(12), dp(14));
        shell.setBackground(rounded(PANEL, LINE, 20, 1));

        shell.addView(text(titleText, 17, TEXT, Typeface.BOLD));

        TextView hint = text("Название комнаты", 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dp(3);
        shell.addView(hint, hp);

        display = text(buffer.length() == 0 ? " " : buffer.toString(), 20, TEXT, Typeface.BOLD);
        display.setGravity(Gravity.CENTER_VERTICAL);
        display.setPadding(dp(12), 0, dp(12), 0);
        display.setSingleLine(true);
        display.setEllipsize(TextUtils.TruncateAt.START);
        display.setTag("native_text_pad_display");
        display.setBackground(rounded(BG, LINE, 12, 1));
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        displayParams.topMargin = dp(10);
        shell.addView(display, displayParams);

        addKeysRow(shell, new String[]{"1","2","3","4","5","6","7","8","9","0"});
        addKeysRow(shell, new String[]{"Й","Ц","У","К","Е","Н","Г","Ш","Щ","З","Х","Ъ"});
        addKeysRow(shell, new String[]{"Ф","Ы","В","А","П","Р","О","Л","Д","Ж","Э"});
        addKeysRow(shell, new String[]{"Я","Ч","С","М","И","Т","Ь","Б","Ю"});

        LinearLayout utility = new LinearLayout(getContext());
        utility.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        up.topMargin = dp(6);
        shell.addView(utility, up);

        Button clear = key("Очистить");
        clear.setTag("native_text_pad_clear");
        clear.setOnClickListener(v -> {
            buffer.setLength(0);
            updateDisplay();
        });

        Button space = key("Пробел");
        space.setTag("native_text_pad_space");
        space.setOnClickListener(v -> append(" "));

        Button backspace = key("⌫");
        backspace.setTag("native_text_pad_backspace");
        backspace.setOnClickListener(v -> {
            if (buffer.length() > 0) buffer.deleteCharAt(buffer.length() - 1);
            updateDisplay();
        });

        LinearLayout.LayoutParams third = new LinearLayout.LayoutParams(0, dp(48), 1f);
        third.leftMargin = dp(3);
        third.rightMargin = dp(3);
        utility.addView(clear, new LinearLayout.LayoutParams(third));
        utility.addView(space, new LinearLayout.LayoutParams(third));
        utility.addView(backspace, new LinearLayout.LayoutParams(third));

        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        ap.topMargin = dp(8);
        shell.addView(actions, ap);

        Button cancel = actionButton("Отмена", false);
        cancel.setTag("native_text_pad_cancel");
        cancel.setOnClickListener(v -> dismiss());

        Button done = actionButton("Готово", true);
        done.setTag("native_text_pad_done");
        done.setOnClickListener(v -> {
            String value = buffer.toString().trim();
            if (!value.isEmpty()) listener.onValue(value);
            dismiss();
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
                    getContext().getResources().getDisplayMetrics().widthPixels - dp(12),
                    dp(560));
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    private void addKeysRow(LinearLayout parent, String[] values) {
        HorizontalScrollView scroller = new HorizontalScrollView(getContext());
        scroller.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        scroller.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(46)));

        for (String value : values) {
            Button key = key(value);
            key.setTag("native_text_pad_key_" + value);
            key.setOnClickListener(v -> append(value));
            LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(dp(43), dp(46));
            kp.leftMargin = dp(2);
            kp.rightMargin = dp(2);
            row.addView(key, kp);
        }

        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        sp.topMargin = dp(4);
        parent.addView(scroller, sp);
    }

    private void append(String value) {
        if (buffer.length() >= 32) return;
        buffer.append(value);
        updateDisplay();
    }

    private void updateDisplay() {
        if (display != null) display.setText(buffer.length() == 0 ? " " : buffer.toString());
    }

    private Button key(String value) {
        Button button = new Button(getContext());
        button.setAllCaps(false);
        button.setText(value);
        button.setTextSize(value.length() > 2 ? 12 : 15);
        button.setTextColor(TEXT);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(rounded(BG, LINE, 10, 1));
        return button;
    }

    private Button actionButton(String value, boolean primary) {
        Button button = key(value);
        button.setTextSize(14);
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

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
