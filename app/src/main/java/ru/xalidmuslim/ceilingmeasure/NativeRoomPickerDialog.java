package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

final class NativeRoomPickerDialog extends Dialog {
    interface Listener { void onRoomType(String type); }

    private final Listener listener;
    private final NativeUiPrefs uiPrefs;

    NativeRoomPickerDialog(
            @NonNull NativeWorkspaceActivity activity,
            @NonNull NativeUiPrefs prefs,
            @NonNull Listener listener) {
        super(activity);
        this.listener = listener;
        uiPrefs = prefs;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout backdrop = new FrameLayout(getContext());
        backdrop.setTag("native_room_picker_backdrop");
        backdrop.setBackgroundColor(Color.argb(36, 28, 42, 54));
        backdrop.setClickable(true);
        backdrop.setOnClickListener(v -> dismiss());

        LinearLayout panel = new LinearLayout(getContext());
        panel.setTag("native_room_picker_panel");
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));
        panel.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,
                Color.rgb(220, 228, 234),
                dp(17),
                dp(1)));
        panel.setElevation(dp(12));
        panel.setClickable(true);

        LinearLayout head = new LinearLayout(getContext());
        head.setTag("native_room_picker_head");
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(4), 0, dp(2), dp(7));
        TextView title = text("Добавить помещение", 16, Color.rgb(53,77,95), true);
        title.setTag("native_room_picker_title");
        head.addView(title, new LinearLayout.LayoutParams(
                0, dp(34), 1f));
        Button close = button("×", false);
        close.setTag("native_room_picker_close");
        close.setContentDescription("Закрыть");
        close.setTextSize(20);
        close.setOnClickListener(v -> dismiss());
        head.addView(close, new LinearLayout.LayoutParams(dp(30), dp(30)));
        panel.addView(head);

        GridLayout grid = new GridLayout(getContext());
        grid.setTag("native_room_picker_grid");
        grid.setColumnCount(2);
        panel.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        add(grid, "▣", "Комната");
        add(grid, "↔", "Коридор");
        add(grid, "◇", "Ванная");
        add(grid, "＋", "Другое");

        TextView tip = text(
                "Название потом можно изменить долгим нажатием по комнате.",
                10.5f,
                Color.rgb(136,151,162),
                false);
        tip.setTag("native_room_picker_tip");
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.setMargins(dp(3), dp(8), dp(3), dp(1));
        panel.addView(tip, tp);

        int widthDp = Math.round(
                getContext().getResources().getDisplayMetrics().widthPixels
                        / getContext().getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(
                dp(Math.min(320, Math.max(290, widthDp - 36))),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        backdrop.addView(panel, pp);
        setContentView(backdrop);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowCompat.setDecorFitsSystemWindows(window, false);
        }
        ViewCompat.setOnApplyWindowInsetsListener(backdrop, (v, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(safe.left + dp(9), safe.top + dp(9),
                    safe.right + dp(9), safe.bottom + dp(9));
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

    private void add(GridLayout grid, String icon, String label) {
        LinearLayout button = new LinearLayout(getContext());
        button.setTag("native_room_type_" + label);
        button.setContentDescription(label);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(6), dp(8), dp(6), dp(8));
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(NativeVisualTokens.outlined(
                Color.rgb(250,252,253),
                Color.rgb(220,228,234),
                dp(12),
                dp(1)));
        TextView iv = text(icon, 20, Color.rgb(64,89,107), false);
        iv.setGravity(Gravity.CENTER);
        TextView name = text(label, 14, Color.rgb(64,89,107), true);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        np.topMargin = dp(4);
        button.addView(iv);
        button.addView(name, np);
        button.setOnClickListener(v -> {
            dismiss();
            listener.onRoomType(label);
        });

        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = dp(68);
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(3), dp(3), dp(3), dp(3));
        grid.addView(button, p);
    }

    private Button button(String value, boolean primary) {
        Button b = new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setTextColor(primary ? Color.WHITE : Color.rgb(90,113,129));
        b.setGravity(Gravity.CENTER);
        b.setPadding(0,0,0,0);
        b.setBackground(primary
                ? NativeVisualTokens.primaryButton(dp(9), dp(1))
                : NativeVisualTokens.solid(Color.rgb(240,244,246), dp(9)));
        NativeTypography.register(b, 13, true, uiPrefs);
        return b;
    }

    private TextView text(String value, float sp, int color, boolean strong) {
        TextView t = new TextView(getContext());
        t.setText(value);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t, sp, strong, uiPrefs);
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
