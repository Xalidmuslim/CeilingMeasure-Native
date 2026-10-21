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
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

final class NativeDrawToolsDialog extends Dialog {
    interface Listener {
        void onAction(String action);
    }

    private static final int TEXT = Color.rgb(35, 56, 72);
    private static final int MUTED = Color.rgb(116, 136, 151);
    private static final int BLUE = Color.rgb(47, 143, 190);
    private static final int LINE = Color.rgb(220, 229, 235);
    private static final int DANGER = Color.rgb(190, 72, 79);

    private final NativeProjectStore.RoomData room;
    private final Listener listener;

    NativeDrawToolsDialog(
            @NonNull Context context,
            @NonNull NativeProjectStore.RoomData room,
            @NonNull Listener listener) {
        super(context);
        this.room = room;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell = new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(12), dp(11), dp(12), dp(14));
        shell.setBackground(rounded(Color.WHITE, LINE, 19, 1));

        LinearLayout head = new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Инструменты чертежа", 17, TEXT, Typeface.BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, dp(38), 1f));
        Button close = toolButton("×", "", false, false);
        close.setTag("native_draw_tools_close");
        close.setOnClickListener(v -> dismiss());
        head.addView(close, new LinearLayout.LayoutParams(dp(42), dp(38)));
        shell.addView(head);

        GridLayout grid = new GridLayout(getContext());
        grid.setColumnCount(2);
        grid.setRowCount(6);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.topMargin = dp(7);
        shell.addView(grid, gp);

        add(grid, "⌁", "45°/90° стены", "magnet", room.wallAngleMagnet, room.geometryLocked, false);
        add(grid, "∟", "90° точки", "point90", room.point90Magnet, room.geometryLocked, false);
        add(grid, "⌂", "Геометрия эскиза", "sketchGeometry",
                !NativeGeometry.hasSketchGeometryBackup(room),
                room.geometryLocked, false);
        add(grid, "△", NativeGeometry.isFactualGeometryDirty(room)
                        ? "Фактическая · обновить"
                        : "Фактическая геометрия", "actualGeometry",
                NativeGeometry.isFactualGeometry(room),
                room.geometryLocked, false);
        add(grid, "◎", NativeGeometry.isMeasurementsGeometryDirty(room)
                        ? "Точно по размерам · обновить"
                        : "Точно по размерам", "exactGeometry",
                NativeGeometry.isMeasurementsGeometry(room),
                room.geometryLocked, false);
        add(grid, "A", room.showAutoLetters ? "Скрыть буквы" : "Показать буквы", "letters", room.showAutoLetters, false, false);
        add(grid, "⌁", "Авто диагонали", "autoDiags", false, room.geometryLocked, false);
        add(grid, "✓", "Проверить замер", "validate", false, false, false);
        add(grid, "123", "Размеры стен", "wallSequence", false, !NativeGeometry.isClosed(room), false);
        add(grid, room.geometryLocked ? "🔒" : "🔓",
                room.geometryLocked ? "Разблокировать" : "Зафиксировать",
                "lock", room.geometryLocked, false, false);
        add(grid, "×", "Очистить", "clear", false, false, true);

        setContentView(shell);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            // Fade-only dialog transition. Never slide or scale the window.
            window.setWindowAnimations(R.style.Animation_CeilingFade);
            window.setDimAmount(.18f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = Math.min(
                    getContext().getResources().getDisplayMetrics().widthPixels - dp(16),
                    dp(420));
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            window.setAttributes(lp);
        }
    }

    private void add(
            GridLayout grid,
            String icon,
            String title,
            String action,
            boolean active,
            boolean disabled,
            boolean danger) {
        Button button = toolButton(icon, title, active, danger);
        button.setEnabled(!disabled);
        button.setAlpha(disabled ? .42f : 1f);
        button.setTag("native_draw_tool_" + action);
        button.setOnClickListener(v -> {
            dismiss();
            listener.onAction(action);
        });

        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = dp(54);
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(3), dp(3), dp(3), dp(3));
        grid.addView(button, p);
    }

    private Button toolButton(
            String icon,
            String title,
            boolean active,
            boolean danger) {
        Button button = new Button(getContext());
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), dp(4), dp(8), dp(4));
        button.setText(title.isEmpty() ? icon : title);
        button.setTextSize(title.isEmpty() ? 20 : 12f);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        button.setTextColor(danger ? DANGER : (active ? BLUE : TEXT));
        int fill = active ? NativeVisualTokens.ACCENT_SOFT
                : NativeVisualTokens.SURFACE_SOFT;
        int stroke = active ? NativeVisualTokens.ACCENT : Color.TRANSPARENT;
        button.setBackground(rounded(fill, stroke, 12, active ? 1 : 0));
        return button;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        view.setGravity(Gravity.CENTER_VERTICAL);
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
