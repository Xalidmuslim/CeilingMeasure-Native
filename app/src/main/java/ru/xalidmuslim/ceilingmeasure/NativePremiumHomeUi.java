package ru.xalidmuslim.ceilingmeasure;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** The launcher presentation only. No geometry, estimate or persistence mutations. */
final class NativePremiumHomeUi {
    private static final int BG = Color.rgb(245, 246, 248);
    private static final int INK = Color.rgb(22, 35, 55);
    private static final int MUTED = Color.rgb(99, 113, 132);
    private static final int BLUE = Color.rgb(25, 103, 166);
    private static final int ICON_BG = Color.rgb(236, 244, 250);
    private static final int BORDER = Color.rgb(227, 233, 238);

    private final NativeHomeActivity activity;
    private final NativeUiPrefs prefs;
    private final Runnable openNew;
    private final Runnable openSaved;
    private final java.util.function.Consumer<String> openRecent;
    private final Runnable openDrawing;
    private final Runnable openMaterials;
    private final Runnable openLicense;
    private final Runnable openSettings;
    private ScrollView scroll;
    private FrameLayout dock;
    private LinearLayout recentRows;
    private ImageView appIcon;
    private TextView savedCount;
    private TextView licenseState;

    NativePremiumHomeUi(
            NativeHomeActivity activity,
            NativeUiPrefs prefs,
            Runnable openNew,
            Runnable openSaved,
            java.util.function.Consumer<String> openRecent,
            Runnable openDrawing,
            Runnable openMaterials,
            Runnable openLicense,
            Runnable openSettings) {
        this.activity = activity;
        this.prefs = prefs;
        this.openNew = openNew;
        this.openSaved = openSaved;
        this.openRecent = openRecent;
        this.openDrawing = openDrawing;
        this.openMaterials = openMaterials;
        this.openLicense = openLicense;
        this.openSettings = openSettings;
    }

    View build() {
        FrameLayout screen = new FrameLayout(activity);
        screen.setTag("native_home_root");
        screen.setBackgroundColor(BG);

        scroll = new ScrollView(activity);
        scroll.setTag("native_home_scroll");
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        screen.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(activity);
        content.setTag("native_home_content");
        content.setOrientation(LinearLayout.VERTICAL);
        // Reserve enough room for the floating dock in both short and tall screens.
        content.setPadding(dp(14), dp(12), dp(14), dp(116));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        addHeader(content);
        addPrimary(content);

        LinearLayout actions = new LinearLayout(activity);
        actions.setTag("native_home_actions");
        actions.setOrientation(LinearLayout.VERTICAL);
        content.addView(actions, matchTop(10));

        addRow(actions, R.drawable.ic_shell_saved, "Замеры", "Сохранённые проекты",
                "native_home_saved", openSaved, true, 0);
        addRow(actions, R.drawable.ic_shell_drawing, "Чертёж", "Построение по размерам",
                "native_home_drawing", openDrawing, false, 6);
        addRow(actions, R.drawable.ic_shell_sliders, "Лицензия", "Статус и управление",
                "native_home_license", openLicense, false, 6);

        LinearLayout heading = new LinearLayout(activity);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headingParams = matchTop(19);
        headingParams.bottomMargin = dp(8);
        content.addView(heading, headingParams);

        TextView recentTitle = text("Последние замеры", 15f, INK, true);
        heading.addView(recentTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView all = text("Все проекты  ›", 12f, BLUE, true);
        all.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        all.setPadding(dp(5), dp(11), dp(1), dp(11));
        all.setClickable(true);
        all.setFocusable(true);
        all.setOnClickListener(v -> openSaved.run());
        heading.addView(all);

        recentRows = new LinearLayout(activity);
        recentRows.setTag("native_home_recent");
        recentRows.setOrientation(LinearLayout.VERTICAL);
        recentRows.setBackground(panel(16));
        content.addView(recentRows, matchTop(0));

        // Avoid a misleading empty list while saved projects are being read asynchronously.
        recentRows.addView(text("Загрузка замеров…", 11.5f, MUTED, false),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dp(46)));
        addDock(screen);
        return screen;
    }

    ScrollView scroll() { return scroll; }
    FrameLayout dock() { return dock; }

    void renderSaved(@NonNull List<NativeProjectStore.ProjectData> saved) {
        if (recentRows == null) return;
        List<NativeProjectStore.ProjectData> records = new ArrayList<>(saved);
        if (savedCount != null) {
            savedCount.setText(Integer.toString(records.size()));
            savedCount.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
        }
        // Display saved data only; never alter the order stored on disk.
        records.sort((a, b) -> Long.compare(b.savedAt, a.savedAt));
        recentRows.removeAllViews();
        if (records.isEmpty()) {
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(15), dp(15), dp(15), dp(15));
            row.setMinimumHeight(dp(66));
            ImageView icon = smallIcon(R.drawable.ic_shell_saved, 36);
            row.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));
            LinearLayout copy = new LinearLayout(activity);
            copy.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cp.leftMargin = dp(12);
            row.addView(copy, cp);
            copy.addView(text("Пока нет сохранённых замеров", 13f, INK, true));
            TextView hint = text("Создайте первый замер", 11.5f, MUTED, false);
            LinearLayout.LayoutParams hintLp = matchTop(3);
            copy.addView(hint, hintLp);
            row.setOnClickListener(v -> openNew.run());
            row.setClickable(true);
            row.setFocusable(true);
            recentRows.addView(row);
            return;
        }

        int limit = Math.min(3, records.size());
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy", new Locale("ru", "RU"));
        for (int index = 0; index < limit; index++) {
            NativeProjectStore.ProjectData project = records.get(index);
            if (index > 0) recentRows.addView(separator());
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(9), dp(12), dp(9));
            row.setMinimumHeight(dp(60));
            ImageView symbol = smallIcon(R.drawable.ic_shell_drawing, 39);
            row.addView(symbol, new LinearLayout.LayoutParams(dp(39), dp(39)));

            LinearLayout copy = new LinearLayout(activity);
            copy.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            copyLp.leftMargin = dp(10);
            copyLp.rightMargin = dp(4);
            row.addView(copy, copyLp);

            String name = project.name == null || project.name.trim().isEmpty()
                    ? "Замер без названия" : project.name.trim();
            TextView nameText = text(name, 13f, INK, true);
            nameText.setSingleLine(true);
            nameText.setEllipsize(TextUtils.TruncateAt.END);
            copy.addView(nameText);

            int rooms = project.rooms.size();
            String description = rooms == 0 ? "Без помещений"
                    : rooms == 1 ? project.rooms.get(0).name
                    : rooms + " помещения";
            TextView detail = text(description, 11.5f, MUTED, false);
            detail.setSingleLine(true);
            detail.setEllipsize(TextUtils.TruncateAt.END);
            copy.addView(detail, matchTop(3));

            if (project.savedAt > 0L) {
                TextView date = text(dateFormat.format(new Date(project.savedAt)), 10f, MUTED, false);
                date.setSingleLine(true);
                row.addView(date);
            }
            TextView arrow = text("›", 24f, MUTED, false);
            arrow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(dp(20), dp(40));
            arrowLp.leftMargin = dp(5);
            row.addView(arrow, arrowLp);
            // Saved order selection is handled by the existing native list.
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> openRecent.accept(project.id));
            recentRows.addView(row);
        }
    }

    void refreshLicense(NativeLicenseClient license) {
        if (licenseState == null || license == null) return;
        String status = license.status();
        licenseState.setText(status == null || status.trim().isEmpty()
                ? "Статус и управление" : status);
    }

    void applyPreloadedIcon(@Nullable Bitmap bitmap) {
        if (appIcon != null && bitmap != null && !bitmap.isRecycled()) {
            appIcon.setImageBitmap(bitmap);
        }
    }

    private void addHeader(LinearLayout content) {
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header, matchTop(0));

        ImageView logo = new ImageView(activity);
        logo.setTag("native_home_app_icon");
        // Avoid decoding the 1024px app artwork on the first UI frame.
        // The icon resource is decoded at screen-appropriate resolution on background IO.
        logo.setImageResource(R.drawable.ic_shell_drawing);
        appIcon = logo;
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("Иконка приложения");
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        logoLp.rightMargin = dp(11);
        header.addView(logo, logoLp);

        LinearLayout titles = new LinearLayout(activity);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titlesLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titlesLp.rightMargin = dp(6);
        header.addView(titles, titlesLp);

        int widthDp = Math.round(activity.getResources().getDisplayMetrics().widthPixels
                / activity.getResources().getDisplayMetrics().density);
        TextView title = text("Замер потолков", widthDp <= 360 ? 19f : 22f, INK, true);
        title.setTag("native_home_title");
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(title);

        TextView subtitle = text("Замер · чертёж · смета", 11.5f, MUTED, false);
        subtitle.setSingleLine(true);
        titles.addView(subtitle, matchTop(3));

        ImageView settings = smallIcon(R.drawable.ic_shell_sliders, 43);
        settings.setTag("native_home_settings");
        settings.setContentDescription("Настройки");
        settings.setBackground(pressBackground(Color.WHITE, BORDER, 13));
        settings.setPadding(dp(9), dp(9), dp(9), dp(9));
        settings.setClickable(true);
        settings.setFocusable(true);
        settings.setOnClickListener(v -> openSettings.run());
        header.addView(settings, new LinearLayout.LayoutParams(dp(43), dp(43)));
    }

    private void addPrimary(LinearLayout content) {
        FrameLayout primary = new FrameLayout(activity);
        primary.setTag("native_home_new");
        primary.setContentDescription("Новый замер");
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(20, 91, 152), Color.rgb(27, 117, 190)});
        gradient.setCornerRadius(dp(16));
        primary.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(38, 255, 255, 255)),
                gradient, round(Color.WHITE, 16)));
        primary.setClickable(true);
        primary.setFocusable(true);
        primary.setOnClickListener(v -> openNew.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(100));
        p.topMargin = dp(14);
        content.addView(primary, p);

        View art = new CeilingContour(activity);
        FrameLayout.LayoutParams artLp = new FrameLayout.LayoutParams(
                dp(146), ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END | Gravity.CENTER_VERTICAL);
        artLp.rightMargin = dp(10);
        primary.addView(art, artLp);

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams copyLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL);
        copyLp.leftMargin = dp(18);
        copyLp.rightMargin = dp(123);
        primary.addView(copy, copyLp);
        copy.addView(text("Новый замер", 19f, Color.WHITE, true));
        TextView help = text("Создать проект, чертёж и расчёт",
                11.5f, Color.rgb(224, 238, 250), false);
        help.setMaxLines(2);
        help.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(help, matchTop(4));

        TextView chevron = text("›", 29f, Color.WHITE, false);
        chevron.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams arrow = new FrameLayout.LayoutParams(
                dp(24), dp(44), Gravity.END | Gravity.CENTER_VERTICAL);
        arrow.rightMargin = dp(9);
        primary.addView(chevron, arrow);
    }

    private void addRow(LinearLayout list, int image, String title, String detail,
                        String tag, Runnable open, boolean withCount, int topMargin) {
        LinearLayout row = new LinearLayout(activity);
        row.setTag(tag);
        row.setContentDescription(title);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setMinimumHeight(dp(68));
        row.setBackground(pressBackground(Color.WHITE, BORDER, 15));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> open.run());
        list.addView(row, matchTop(topMargin));

        ImageView icon = smallIcon(image, 43);
        row.addView(icon, new LinearLayout.LayoutParams(dp(43), dp(43)));
        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        copyLp.leftMargin = dp(12);
        copyLp.rightMargin = dp(7);
        row.addView(copy, copyLp);

        copy.addView(text(title, 15f, INK, true));
        TextView sub = text(detail, 11.5f, MUTED, false);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(sub, matchTop(3));
        if ("native_home_license".equals(tag)) licenseState = sub;

        if (withCount) {
            savedCount = text("", 14f, MUTED, false);
            savedCount.setVisibility(View.GONE);
            row.addView(savedCount);
        }
        TextView arrow = text("›", 25f, MUTED, false);
        arrow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(dp(20), dp(38));
        arrowLp.leftMargin = dp(5);
        row.addView(arrow, arrowLp);
    }

    private void addDock(FrameLayout screen) {
        dock = new FrameLayout(activity);
        dock.setTag("native_home_bottom_nav");
        dock.setBackground(pressBackground(Color.WHITE, BORDER, 21));
        FrameLayout.LayoutParams dockLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70),
                Gravity.BOTTOM);
        screen.addView(dock, dockLp);
        LinearLayout slots = new LinearLayout(activity);
        slots.setGravity(Gravity.CENTER);
        dock.addView(slots, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        addNav(slots, R.drawable.ic_shell_home, "Главная", true, () -> scroll.smoothScrollTo(0, 0));
        addNav(slots, R.drawable.ic_shell_saved, "Замеры", false, openSaved);

        LinearLayout plusSlot = new LinearLayout(activity);
        plusSlot.setOrientation(LinearLayout.VERTICAL);
        plusSlot.setGravity(Gravity.CENTER);
        slots.addView(plusSlot, new LinearLayout.LayoutParams(0, dp(70), 1f));
        TextView plus = text("+", 32f, Color.WHITE, false);
        plus.setTag("native_home_nav_new");
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(40, 255, 255, 255)),
                round(Color.rgb(13, 107, 220), 30),
                round(Color.WHITE, 30)));
        plus.setClickable(true);
        plus.setFocusable(true);
        plus.setContentDescription("Новый замер");
        plus.setOnClickListener(v -> openNew.run());
        plusSlot.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        plusSlot.addView(text("Новый", 9.5f, MUTED, false), matchTop(1));

        addNav(slots, R.drawable.ic_shell_layers, "Материалы", false, openMaterials);
        addNav(slots, R.drawable.ic_shell_sliders, "Настройки", false, openSettings);
    }

    private void addNav(LinearLayout parent, int icon, String label,
                        boolean active, Runnable callback) {
        LinearLayout slot = new LinearLayout(activity);
        slot.setOrientation(LinearLayout.VERTICAL);
        slot.setGravity(Gravity.CENTER);
        slot.setClickable(true);
        slot.setFocusable(true);
        slot.setContentDescription(label);
        slot.setOnClickListener(v -> callback.run());
        parent.addView(slot, new LinearLayout.LayoutParams(0, dp(70), 1f));

        ImageView glyph = new ImageView(activity);
        glyph.setImageResource(icon);
        glyph.setImageTintList(ColorStateList.valueOf(active ? BLUE : MUTED));
        glyph.setPadding(dp(7), dp(7), dp(7), dp(7));
        slot.addView(glyph, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView text = text(label, 10f, active ? BLUE : MUTED, active);
        text.setSingleLine(true);
        text.setGravity(Gravity.CENTER);
        slot.addView(text, matchTop(2));
    }

    private ImageView smallIcon(int drawable, int size) {
        ImageView icon = new ImageView(activity);
        icon.setImageResource(drawable);
        icon.setImageTintList(ColorStateList.valueOf(Color.rgb(29, 78, 119)));
        icon.setBackground(round(ICON_BG, 12));
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        return icon;
    }

    private View separator() {
        View line = new View(activity);
        line.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.leftMargin = dp(10);
        params.rightMargin = dp(10);
        line.setLayoutParams(params);
        return line;
    }

    private Drawable pressBackground(int fill, int border, int radius) {
        GradientDrawable base = panelDrawable(fill, border, radius);
        return new RippleDrawable(
                ColorStateList.valueOf(Color.argb(18, 23, 58, 92)),
                base, round(Color.WHITE, radius));
    }

    private Drawable panel(int radius) { return panelDrawable(Color.WHITE, BORDER, radius); }

    private GradientDrawable panelDrawable(int fill, int border, int radius) {
        GradientDrawable d = round(fill, radius);
        d.setStroke(dp(1), border);
        return d;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private TextView text(String content, float sp, int color, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(content);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t, sp, bold, prefs);
    }

    private LinearLayout.LayoutParams matchTop(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    /**
     * A flat recessed stretch-ceiling panel in perspective, not a 3D cube.
     * Two nested profile outlines communicate the stretched membrane/cove;
     * small elliptical downlights make the subject recognizable at 100dp.
     * Everything is drawn as lightweight native Canvas strokes without
     * bitmaps or extra hardware layers.
     */
    private static final class CeilingContour extends View {
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        CeilingContour(NativeHomeActivity activity) { super(activity); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.scale(getWidth() / 146f, getHeight() / 100f);

            ink.setStyle(Paint.Style.FILL);
            ink.setColor(Color.argb(15, 239, 250, 255));
            Path membrane = new Path();
            membrane.moveTo(18f, 42f);
            membrane.lineTo(91f, 14f);
            membrane.lineTo(137f, 34f);
            membrane.lineTo(64f, 76f);
            membrane.close();
            canvas.drawPath(membrane, ink);

            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeWidth(1f);
            ink.setStrokeJoin(Paint.Join.ROUND);
            ink.setStrokeCap(Paint.Cap.ROUND);

            // Thin aluminum perimeter of the ceiling, viewed from below.
            ink.setColor(Color.argb(194, 236, 249, 255));
            Path perimeter = new Path();
            perimeter.moveTo(6f, 41f);
            perimeter.lineTo(90f, 9f);
            perimeter.lineTo(143f, 33f);
            perimeter.lineTo(62f, 82f);
            perimeter.close();
            canvas.drawPath(perimeter, ink);

            // Cove profile / second level; no vertical cube walls.
            ink.setColor(Color.argb(135, 225, 246, 255));
            Path profile = new Path();
            profile.moveTo(17f, 42f);
            profile.lineTo(90f, 17f);
            profile.lineTo(132f, 34f);
            profile.lineTo(62f, 71f);
            profile.close();
            canvas.drawPath(profile, ink);

            // Recessed light strip following the ceiling membrane.
            ink.setColor(Color.argb(93, 228, 249, 255));
            Path inner = new Path();
            inner.moveTo(26f, 43f);
            inner.lineTo(91f, 23f);
            inner.lineTo(123f, 35f);
            inner.lineTo(62f, 63f);
            inner.close();
            canvas.drawPath(inner, ink);

            // Four flush recessed downlights in the membrane (not cube corners).
            drawDownlight(canvas, 42f, 44f, 4.5f, 2.2f);
            drawDownlight(canvas, 88f, 27f, 3.7f, 1.9f);
            drawDownlight(canvas, 111f, 36f, 3.8f, 1.9f);
            drawDownlight(canvas, 63f, 58f, 4.7f, 2.4f);

            // Technical guide and profile ticks, kept faint behind the subject.
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeWidth(.7f);
            ink.setColor(Color.argb(70, 233, 249, 255));
            canvas.drawLine(20f, 30f, 83f, 6f, ink);
            canvas.drawLine(21f, 25f, 18f, 34f, ink);
            canvas.drawLine(87f, 2f, 83f, 12f, ink);
            canvas.restore();
        }

        private void drawDownlight(Canvas canvas, float x, float y, float w, float h) {
            ink.setStyle(Paint.Style.FILL);
            ink.setColor(Color.argb(46, 245, 253, 255));
            canvas.drawOval(x - w, y - h, x + w, y + h, ink);
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeWidth(.85f);
            ink.setColor(Color.argb(205, 250, 254, 255));
            canvas.drawOval(x - w, y - h, x + w, y + h, ink);
            ink.setStyle(Paint.Style.FILL);
            ink.setColor(Color.argb(115, 241, 251, 255));
            canvas.drawOval(x - 1.2f, y - .5f, x + 1.2f, y + .5f, ink);
            ink.setStyle(Paint.Style.STROKE);
        }
    }
}
