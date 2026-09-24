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

/** Exact Phase-3 room action sheet + custom name keyboard from golden v184. */
final class NativeRoomActionDialog extends Dialog {
    interface Listener {
        boolean onRename(@NonNull String rawValue);
        void onDuplicate();
        void onDelete();
    }

    private final NativeUiPrefs uiPrefs;
    private final Listener listener;
    private final StringBuilder buffer = new StringBuilder();
    private TextView valueView;

    NativeRoomActionDialog(
            @NonNull NativeWorkspaceActivity activity,
            @NonNull String initialName,
            @NonNull NativeUiPrefs prefs,
            @NonNull Listener listener) {
        super(activity);
        if (initialName.length() > NativeOrderRoomLogic.ROOM_NAME_MAX) {
            buffer.append(initialName, 0, NativeOrderRoomLogic.ROOM_NAME_MAX);
        } else {
            buffer.append(initialName);
        }
        uiPrefs = prefs;
        this.listener = listener;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout backdrop = new FrameLayout(getContext());
        backdrop.setTag("native_room_action_backdrop");
        backdrop.setBackgroundColor(Color.argb(36, 28, 42, 54));
        backdrop.setClickable(true);
        backdrop.setOnClickListener(v -> dismiss());

        LinearLayout sheet = new LinearLayout(getContext());
        sheet.setTag("native_room_action_sheet");
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(12), dp(12), dp(12), dp(14));
        sheet.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,
                Color.rgb(220, 228, 234),
                dp(18),
                dp(1)));
        sheet.setElevation(dp(12));
        sheet.setClickable(true);

        TextView title = text("Комната", 18, NativeVisualTokens.TEXT, true);
        title.setTag("native_room_action_title");
        sheet.addView(title);

        TextView subtitle = text(
                "Удерживай комнату примерно полсекунды → переименовать или удалить",
                11.5f,
                NativeVisualTokens.MUTED,
                false);
        subtitle.setTag("native_room_action_subtitle");
        LinearLayout.LayoutParams sp = match();
        sp.topMargin = dp(3);
        sheet.addView(subtitle, sp);

        valueView = text(
                buffer.length() == 0 ? " " : buffer.toString(),
                15,
                NativeVisualTokens.TEXT,
                true);
        valueView.setTag("native_room_rename_value");
        valueView.setContentDescription("Название комнаты");
        valueView.setGravity(Gravity.CENTER_VERTICAL);
        valueView.setSingleLine(true);
        valueView.setPadding(dp(10), 0, dp(10), 0);
        valueView.setBackground(NativeVisualTokens.outlined(
                Color.rgb(249, 251, 252),
                NativeVisualTokens.LINE,
                dp(11),
                dp(1)));
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        vp.topMargin = dp(9);
        sheet.addView(valueView, vp);

        LinearLayout keyboard = new LinearLayout(getContext());
        keyboard.setTag("native_room_alpha_keyboard");
        keyboard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams kp = match();
        kp.topMargin = dp(7);
        sheet.addView(keyboard, kp);

        for (String[] row : NativeOrderRoomLogic.ROOM_KEY_ROWS) {
            LinearLayout keyRow = new LinearLayout(getContext());
            keyRow.setOrientation(LinearLayout.HORIZONTAL);
            keyRow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rp = match();
            rp.topMargin = dp(3);
            keyboard.addView(keyRow, rp);
            for (String key : row) {
                Button b = keyButton(key);
                b.setTag("native_room_alpha_key_" + key);
                b.setOnClickListener(v -> {
                    replaceValue(NativeOrderRoomLogic.applyRoomNameKey(
                            buffer.toString(), key, null));
                });
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        0, dp(38), 1f);
                bp.leftMargin = dp(1);
                bp.rightMargin = dp(1);
                keyRow.addView(b, bp);
            }
        }

        LinearLayout controls = new LinearLayout(getContext());
        controls.setTag("native_room_alpha_controls");
        controls.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams controlsParams = match();
        controlsParams.topMargin = dp(5);
        keyboard.addView(controls, controlsParams);

        addControl(controls, "Пробел", "space");
        addControl(controls, "-", "dash");
        addControl(controls, "⌫", "back");
        addControl(controls, "Очистить", "clear");

        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap = match();
        ap.topMargin = dp(8);
        sheet.addView(actions, ap);

        Button close = actionButton("Закрыть", false, false);
        close.setTag("native_room_action_close");
        close.setOnClickListener(v -> dismiss());
        Button rename = actionButton("Переименовать", true, false);
        rename.setTag("native_room_action_rename");
        rename.setOnClickListener(v -> {
            if (listener.onRename(buffer.toString())) dismiss();
        });

        LinearLayout.LayoutParams halfLeft = new LinearLayout.LayoutParams(0, dp(42), 1f);
        halfLeft.rightMargin = dp(4);
        LinearLayout.LayoutParams halfRight = new LinearLayout.LayoutParams(0, dp(42), 1f);
        halfRight.leftMargin = dp(4);
        actions.addView(close, halfLeft);
        actions.addView(rename, halfRight);

        Button duplicate = actionButton("Дублировать комнату", false, false);
        duplicate.setTag("native_room_action_duplicate");
        duplicate.setOnClickListener(v -> {
            dismiss();
            listener.onDuplicate();
        });
        LinearLayout.LayoutParams dup = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        dup.topMargin = dp(8);
        sheet.addView(duplicate, dup);

        Button delete = actionButton("Удалить комнату", false, true);
        delete.setTag("native_room_action_delete");
        delete.setOnClickListener(v -> {
            dismiss();
            listener.onDelete();
        });
        LinearLayout.LayoutParams del = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        del.topMargin = dp(7);
        sheet.addView(delete, del);

        int widthDp = Math.round(
                getContext().getResources().getDisplayMetrics().widthPixels
                        / getContext().getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(
                dp(Math.min(330, Math.max(300, widthDp - 18))),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        backdrop.addView(sheet, pp);

        setContentView(backdrop);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
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

    @NonNull String currentValueForTest() { return buffer.toString(); }

    private void addControl(LinearLayout parent, String label, String action) {
        Button b = keyButton(label);
        b.setTag("native_room_alpha_" + action);
        b.setOnClickListener(v -> replaceValue(
                NativeOrderRoomLogic.applyRoomNameKey(buffer.toString(), null, action)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(40), 1f);
        p.leftMargin = dp(2);
        p.rightMargin = dp(2);
        parent.addView(b, p);
    }

    private void replaceValue(String value) {
        buffer.setLength(0);
        if (value.length() > NativeOrderRoomLogic.ROOM_NAME_MAX) {
            buffer.append(value, 0, NativeOrderRoomLogic.ROOM_NAME_MAX);
        } else {
            buffer.append(value);
        }
        valueView.setText(buffer.length() == 0 ? " " : buffer.toString());
    }

    private Button keyButton(String value) {
        Button b = new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setTextColor("Очистить".equals(value)
                ? NativeVisualTokens.DANGER : NativeVisualTokens.TEXT);
        b.setBackground(NativeVisualTokens.outlined(
                "Очистить".equals(value) ? Color.rgb(255,248,248) : Color.rgb(248,250,252),
                "Очистить".equals(value) ? Color.rgb(239,202,202) : NativeVisualTokens.LINE,
                dp(9),
                dp(1)));
        NativeTypography.register(b, value.length() > 2 ? 10 : 12, true, uiPrefs);
        return b;
    }

    private Button actionButton(String value, boolean primary, boolean danger) {
        Button b = new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), 0, dp(8), 0);
        if (primary) {
            b.setTextColor(Color.WHITE);
            b.setBackground(NativeVisualTokens.primaryButton(dp(11), dp(1)));
        } else {
            b.setTextColor(danger ? NativeVisualTokens.DANGER : NativeVisualTokens.TEXT);
            b.setBackground(NativeVisualTokens.outlined(
                    danger ? Color.rgb(255,248,248) : NativeVisualTokens.SURFACE,
                    danger ? Color.rgb(239,202,202) : NativeVisualTokens.LINE,
                    dp(11),
                    dp(1)));
        }
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

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
