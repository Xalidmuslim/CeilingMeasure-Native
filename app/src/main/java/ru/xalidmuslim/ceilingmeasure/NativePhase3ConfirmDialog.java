package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** v184-style destructive confirmation used only by Phase-3 Orders/Rooms. */
final class NativePhase3ConfirmDialog extends Dialog {
    interface Listener { void onConfirm(); }

    private final String titleText;
    private final String messageText;
    private final String tagPrefix;
    private final NativeUiPrefs uiPrefs;
    private final Listener listener;

    NativePhase3ConfirmDialog(
            @NonNull NativeWorkspaceActivity activity,
            @NonNull String title,
            @NonNull String message,
            @NonNull String tagPrefix,
            @NonNull NativeUiPrefs prefs,
            @NonNull Listener listener) {
        super(activity);
        titleText = title;
        messageText = message;
        this.tagPrefix = tagPrefix;
        uiPrefs = prefs;
        this.listener = listener;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout backdrop = new FrameLayout(getContext());
        backdrop.setTag(tagPrefix + "_backdrop");
        backdrop.setBackgroundColor(Color.argb(18, 17, 29, 42));
        backdrop.setClickable(true);
        backdrop.setOnClickListener(v -> dismiss());

        LinearLayout panel = new LinearLayout(getContext());
        panel.setTag(tagPrefix + "_panel");
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(13), dp(13), dp(13), dp(13));
        panel.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,
                Color.rgb(214, 224, 233),
                dp(15),
                dp(1)));
        panel.setElevation(dp(10));
        panel.setClickable(true);

        TextView title = text(titleText, 18, NativeVisualTokens.TEXT, true);
        title.setTag(tagPrefix + "_title");
        panel.addView(title);

        TextView message = text(messageText, 11.5f, NativeVisualTokens.MUTED, false);
        message.setTag(tagPrefix + "_text");
        LinearLayout.LayoutParams mp = wrap();
        mp.topMargin = dp(5);
        panel.addView(message, mp);

        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        LinearLayout.LayoutParams ap = match();
        ap.topMargin = dp(12);
        panel.addView(actions, ap);

        Button cancel = button("Отмена", false);
        cancel.setTag(tagPrefix + "_cancel");
        cancel.setOnClickListener(v -> dismiss());
        actions.addView(cancel, new LinearLayout.LayoutParams(dp(92), dp(40)));

        Button confirm = button("Удалить", true);
        confirm.setTag(tagPrefix + "_confirm");
        confirm.setOnClickListener(v -> {
            listener.onConfirm();
            dismiss();
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(92), dp(40));
        cp.leftMargin = dp(7);
        actions.addView(confirm, cp);

        int widthDp = Math.round(
                getContext().getResources().getDisplayMetrics().widthPixels
                        / getContext().getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(
                dp(Math.min(320, Math.max(280, widthDp - 32))),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        backdrop.addView(panel, pp);
        setContentView(backdrop);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        ViewCompat.setOnApplyWindowInsetsListener(backdrop, (v, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(safe.left + dp(8), safe.top + dp(8),
                    safe.right + dp(8), safe.bottom + dp(8));
            return insets;
        });
        ViewCompat.requestApplyInsets(backdrop);
    }

    @Override protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private Button button(String value, boolean danger) {
        Button b = new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(7), 0, dp(7), 0);
        b.setTextColor(danger ? NativeVisualTokens.DANGER : NativeVisualTokens.TEXT);
        b.setBackground(NativeVisualTokens.outlined(
                danger ? Color.rgb(255, 248, 248) : NativeVisualTokens.SURFACE,
                danger ? Color.rgb(239, 202, 202) : NativeVisualTokens.LINE,
                dp(12),
                dp(1)));
        NativeTypography.register(b, 12, true, uiPrefs);
        return b;
    }

    private TextView text(String value, float sp, int color, boolean strong) {
        TextView t = new TextView(getContext());
        t.setText(value);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t, sp, strong, uiPrefs);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
