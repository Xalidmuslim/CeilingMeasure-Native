package ru.xalidmuslim.ceilingmeasure;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.animation.ValueAnimator;
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
    private Bitmap displayedIcon;
    private String recentSnapshotKey;
    private TextView savedCount;
    private TextView licenseState;
    private View liquidIndicator;
    private FrameLayout speedDial;
    private TextView plusButton;
    private LinearLayout compactHeader;
    private boolean compactShowing;
    private boolean speedDialOpen;

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

        // Load is LOCAL (not a server request): matte skeletons replace a
        // misleading empty list until NativeHomePreloader finishes.
        addLoadingSkeleton();
        addDock(screen);
        addCompactHeader(screen);
        addSpeedDial(screen);
        scroll.setOnScrollChangeListener((View v, int x, int y, int oldX, int oldY) ->
                onHomeScroll(screen, y, oldY));
        return screen;
    }

    ScrollView scroll() { return scroll; }
    FrameLayout dock() { return dock; }

    void renderSaved(@NonNull List<NativeProjectStore.ProjectData> saved) {
        if (recentRows == null) return;
        // Saved projects arrive sorted from Dispatchers.IO-equivalent preload.
        // Avoid re-inflating three cards when returning from a dialog or
        // drawing without any relevant changes, preserving scroll and frames.
        List<NativeProjectStore.ProjectData> records = new ArrayList<>(saved);
        StringBuilder snapshot = new StringBuilder().append(records.size());
        for (int i = 0; i < Math.min(3, records.size()); i++) {
            NativeProjectStore.ProjectData record = records.get(i);
            snapshot.append('|').append(record.id)
                    .append('|').append(record.savedAt)
                    .append('|').append(record.updatedAt)
                    .append('|').append(record.name)
                    .append('|').append(record.rooms.size());
            if (record.rooms.size() == 1) {
                snapshot.append('|').append(record.rooms.get(0).name);
            }
        }
        String key = snapshot.toString();
        if (key.equals(recentSnapshotKey)) return;
        recentSnapshotKey = key;
        if (savedCount != null) {
            savedCount.setText(Integer.toString(records.size()));
            savedCount.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
        }
        // Animate the previous matte loading/empty state away before replacing
        // it. The data is already cached; this does not delay project loading.
        if (NativeHomeExpressiveMotion.enabled(recentRows)
                && recentRows.isAttachedToWindow()
                && recentRows.getChildCount() > 0) {
            recentRows.animate().cancel();
            recentRows.animate().alpha(0f).scaleX(.98f).scaleY(.96f)
                    .setDuration(125L).setInterpolator(NativeMotion.easing())
                    .withEndAction(() -> {
                        if (!key.equals(recentSnapshotKey)) return;
                        recentRows.setAlpha(1f);
                        recentRows.setScaleX(1f);
                        recentRows.setScaleY(1f);
                        renderSavedRows(records);
                    }).start();
        } else {
            renderSavedRows(records);
        }
    }

    private void renderSavedRows(@NonNull List<NativeProjectStore.ProjectData> records) {
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
            if (NativeHomeExpressiveMotion.enabled(recentRows)) {
                row.setAlpha(.5f);
                row.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(290).setInterpolator(NativeMotion.easing()).start();
                icon.animate().rotation(7f).setDuration(185)
                        .withEndAction(() -> icon.animate().rotation(0f)
                                .setDuration(220).start()).start();
            }
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
        NativeHomeExpressiveMotion.staggerChildren(recentRows);
    }

    void refreshLicense(NativeLicenseClient license) {
        if (licenseState == null || license == null) return;
        String status = license.status();
        licenseState.setText(status == null || status.trim().isEmpty()
                ? "Статус и управление" : status);
    }

    void applyPreloadedIcon(@Nullable Bitmap bitmap) {
        if (appIcon == null || bitmap == null || bitmap.isRecycled()) return;
        // Reopening the Home or closing Settings must not rebind an identical
        // Drawable, replay the icon animation, or invalidate the toolbar.
        if (displayedIcon == bitmap) return;
        displayedIcon = bitmap;
        appIcon.animate().cancel();
        appIcon.setImageBitmap(bitmap);
        if (NativeMotion.animationsEnabled()) {
            appIcon.setAlpha(.90f);
            appIcon.animate().alpha(1f)
                    .setDuration(NativeMotion.MICRO_ENTER_MS)
                    .setInterpolator(NativeMotion.easing())
                    .start();
        } else {
            appIcon.setAlpha(1f);
        }
    }

    private void addHeader(LinearLayout content) {
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header, matchTop(0));

        ImageView logo = new ImageView(activity);
        logo.setTag("native_home_app_icon");
        // The original master has a baked-in white background. Only the
        // edge-connected white pixels are cleared by background preloading.
        // The view itself never adds a white tile, stroke or extra GPU layer.
        logo.setBackground(null);
        logo.setPadding(0, 0, 0, 0);
        logo.setLayerType(View.LAYER_TYPE_NONE, null);
        // Avoid decoding the 1024px app artwork on the first UI frame.
        // The icon resource is decoded at screen-appropriate resolution on background IO.
        // Reserve the icon's exact dimensions from the first frame, but
        // never flash a different placeholder icon behind the genuine artwork.
        // Background preload supplies the original logo once, then 110ms alpha.
        logo.setImageDrawable(null);
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
        primary.setTransitionName(NativeHomeExpressiveProfile.CTA_TRANSITION_NAME);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(100));
        p.topMargin = dp(14);
        content.addView(primary, p);

        View art = new CeilingContour(activity);
        art.setTag("native_home_ceiling_art");
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
        NativeHomeExpressiveMotion.bindCard(primary, art, chevron, () -> {
            ((CeilingContour) art).scanOnce();
            primary.postDelayed(() -> activity.openPrimaryCta(primary), 155L);
        }, true, false);
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
        NativeHomeExpressiveMotion.bindCard(row, icon, arrow, open, false,
                "native_home_drawing".equals(tag));
    }

    private void addDock(FrameLayout screen) {
        dock = new FrameLayout(activity);
        dock.setTag("native_home_bottom_nav");
        dock.setBackground(pressBackground(Color.WHITE, BORDER, 21));
        FrameLayout.LayoutParams dockLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70),
                Gravity.BOTTOM);
        screen.addView(dock, dockLp);
        liquidIndicator = new View(activity);
        liquidIndicator.setTag("native_home_liquid_indicator");
        liquidIndicator.setVisibility(NativeTransformerMode.enabled(activity)
                ? View.VISIBLE : View.GONE);
        liquidIndicator.setBackground(round(Color.rgb(220, 235, 249), 16));
        dock.addView(liquidIndicator, new FrameLayout.LayoutParams(
                dp(48), dp(43), Gravity.TOP | Gravity.LEFT));
        liquidIndicator.setTranslationY(dp(10));
        liquidIndicator.post(() ->
                NativeHomeExpressiveMotion.moveIndicator(liquidIndicator, 0));

        LinearLayout slots = new LinearLayout(activity);
        slots.setGravity(Gravity.CENTER);
        dock.addView(slots, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        addNav(slots, R.drawable.ic_shell_home, "Главная", true,
                () -> scroll.smoothScrollTo(0, 0), 0);
        addNav(slots, R.drawable.ic_shell_saved, "Замеры", false, openSaved, 1);

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
        plusButton = plus;
        plus.setOnClickListener(v -> {
            if (!NativeHomeExpressiveMotion.enabled(v)) {
                openNew.run();
            } else {
                toggleSpeedDial();
            }
        });
        plusSlot.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        plusSlot.addView(text("Новый", 9.5f, MUTED, false), matchTop(1));

        addNav(slots, R.drawable.ic_shell_layers, "Материалы", false, openMaterials, 3);
        addNav(slots, R.drawable.ic_shell_sliders, "Настройки", false, openSettings, 4);
    }

    private void addNav(LinearLayout parent, int icon, String label,
                        boolean active, Runnable callback, int position) {
        LinearLayout slot = new LinearLayout(activity);
        slot.setOrientation(LinearLayout.VERTICAL);
        slot.setGravity(Gravity.CENTER);
        slot.setClickable(true);
        slot.setFocusable(true);
        slot.setContentDescription(label);
        // Navigation listeners are attached after creating the icon.
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
        NativeTransformerMotion.bindNavigation(slot, glyph,
                TransformerMotionProfile.directionForIndex(position,
                        NativeHomeExpressiveProfile.DOCK_SLOTS),
                () -> {
                    if (NativeHomeExpressiveMotion.enabled(slot)) {
                        NativeHomeExpressiveMotion.moveIndicator(liquidIndicator, position);
                        slot.postDelayed(callback, 140L);
                    } else {
                        callback.run();
                    }
                });
    }

    /** Actual local loading placeholder: one matte opacity pass, no glossy shimmer. */
    private void addLoadingSkeleton() {
        for (int i = 0; i < 2; i++) {
            LinearLayout skeleton = new LinearLayout(activity);
            skeleton.setTag("native_home_loading_skeleton");
            skeleton.setGravity(Gravity.CENTER_VERTICAL);
            skeleton.setPadding(dp(15), dp(12), dp(15), dp(12));
            View icon = new View(activity);
            icon.setBackground(round(Color.rgb(225, 232, 239), 10));
            skeleton.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));
            LinearLayout bars = new LinearLayout(activity);
            bars.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams barsLp =
                    new LinearLayout.LayoutParams(0, dp(36), 1f);
            barsLp.leftMargin = dp(12);
            skeleton.addView(bars, barsLp);
            View first = new View(activity);
            first.setBackground(round(Color.rgb(225, 232, 239), 5));
            bars.addView(first, new LinearLayout.LayoutParams(
                    i == 0 ? dp(115) : dp(145), dp(13)));
            View second = new View(activity);
            second.setBackground(round(Color.rgb(235, 240, 245), 5));
            LinearLayout.LayoutParams secondLp = new LinearLayout.LayoutParams(
                    i == 0 ? dp(88) : dp(112), dp(9));
            secondLp.topMargin = dp(8);
            bars.addView(second, secondLp);
            recentRows.addView(skeleton, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));
            if (NativeHomeExpressiveMotion.enabled(skeleton)) {
                skeleton.setAlpha(.65f);
                skeleton.animate().alpha(.95f).setDuration(440)
                        .setInterpolator(NativeMotion.easing()).start();
            }
        }
    }

    private void addCompactHeader(FrameLayout screen) {
        compactHeader = new LinearLayout(activity);
        compactHeader.setTag("native_home_collapsing_app_bar");
        compactHeader.setGravity(Gravity.CENTER_VERTICAL);
        compactHeader.setPadding(dp(18), 0, dp(14), 0);
        compactHeader.setBackground(panelDrawable(Color.rgb(248, 249, 250),
                BORDER, 0));
        compactHeader.setVisibility(View.INVISIBLE);
        TextView title = text("Замер потолков", 15f, INK, true);
        compactHeader.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView settings = smallIcon(R.drawable.ic_shell_sliders, 36);
        settings.setTag("native_home_compact_settings");
        settings.setContentDescription("Настройки");
        settings.setClickable(true);
        settings.setFocusable(true);
        settings.setOnClickListener(v -> openSettings.run());
        compactHeader.addView(settings, new LinearLayout.LayoutParams(dp(36), dp(36)));
        screen.addView(compactHeader, 1, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50), Gravity.TOP));
    }

    private void addSpeedDial(FrameLayout screen) {
        speedDial = new FrameLayout(activity);
        speedDial.setTag("native_home_speed_dial");
        speedDial.setBackgroundColor(Color.argb(67, 19, 34, 50));
        speedDial.setClickable(true);
        speedDial.setFocusable(true);
        speedDial.setContentDescription("Закрыть быстрые действия");
        speedDial.setOnClickListener(v -> closeSpeedDial());
        speedDial.setVisibility(View.GONE);
        String[] names = {"Новый замер", "Открыть чертёж", "Сохранённые замеры"};
        Runnable[] actions = {openNew, openDrawing, openSaved};
        for (int i = 0; i < NativeHomeExpressiveProfile.QUICK_ACTIONS_COUNT; i++) {
            final int index = i;
            TextView item = text(names[i], 12.5f, INK, true);
            item.setTag("native_home_speed_action_" + i);
            item.setContentDescription(names[i]);
            item.setGravity(Gravity.CENTER);
            item.setBackground(panelDrawable(Color.WHITE, BORDER, 15));
            item.setElevation(dp(2));
            item.setClickable(true);
            item.setFocusable(true);
            item.setOnClickListener(v -> {
                closeSpeedDial();
                v.postDelayed(actions[index], 135L);
            });
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    dp(160), dp(45), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.bottomMargin = dp(115 + i * 60);
            speedDial.addView(item, lp);
        }
        // Behind the permanent dock so the same central + also closes the dial.
        screen.addView(speedDial, 2, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void toggleSpeedDial() {
        if (speedDialOpen) {
            closeSpeedDial();
            return;
        }
        speedDialOpen = true;
        NativeHomeExpressiveMotion.fabState(plusButton, true);
        speedDial.setVisibility(View.VISIBLE);
        speedDial.animate().cancel();
        speedDial.setAlpha(0f);
        speedDial.animate().alpha(1f).setDuration(170)
                .setInterpolator(NativeMotion.easing()).start();
        for (int i = 0; i < speedDial.getChildCount(); i++) {
            View button = speedDial.getChildAt(i);
            float targetX = (i - 1) * dp(58);
            button.animate().cancel();
            button.setAlpha(0f);
            button.setTranslationX(0f);
            button.setTranslationY(dp(28));
            button.setScaleX(.8f);
            button.setScaleY(.88f);
            button.animate().alpha(1f).translationX(targetX).translationY(0f)
                    .scaleX(1f).scaleY(1f).setStartDelay(i * 42L)
                    .setDuration(260).setInterpolator(
                            new android.view.animation.OvershootInterpolator(.67f)).start();
        }
    }

    private void closeSpeedDial() {
        if (!speedDialOpen || speedDial == null) return;
        speedDialOpen = false;
        NativeHomeExpressiveMotion.fabState(plusButton, false);
        for (int i = 0; i < speedDial.getChildCount(); i++) {
            View button = speedDial.getChildAt(i);
            button.animate().cancel();
            button.animate().alpha(0f).translationY(dp(22)).scaleX(.9f)
                    .setDuration(135).setInterpolator(NativeMotion.easing()).start();
        }
        speedDial.animate().cancel();
        speedDial.animate().alpha(0f).setDuration(155)
                .withEndAction(() -> {
                    if (!speedDialOpen) speedDial.setVisibility(View.GONE);
                }).start();
    }

    boolean dismissSpeedDial() {
        if (!speedDialOpen) return false;
        closeSpeedDial();
        return true;
    }

    void updateMotionMode() {
        if (liquidIndicator == null) return;
        boolean expressive = NativeTransformerMode.enabled(activity);
        liquidIndicator.setVisibility(expressive ? View.VISIBLE : View.GONE);
        if (expressive) {
            liquidIndicator.post(() ->
                    NativeHomeExpressiveMotion.moveIndicator(liquidIndicator, 0));
        } else {
            resetTransientUi();
            dock.animate().cancel();
            dock.setTranslationY(0f);
            if (compactHeader != null) compactHeader.setVisibility(View.INVISIBLE);
            compactShowing = false;
        }
    }

    void resetTransientUi() {
        if (speedDial != null && speedDialOpen) closeSpeedDial();
    }

    private void onHomeScroll(FrameLayout screen, int y, int previousY) {
        int available = scroll.getChildAt(0).getHeight() - scroll.getHeight();
        boolean motion = NativeHomeExpressiveMotion.enabled(scroll);
        if (!motion) {
            dock.animate().cancel();
            dock.setTranslationY(0f);
            if (compactHeader != null) compactHeader.setVisibility(View.INVISIBLE);
            return;
        }
        if (NativeHomeExpressiveProfile.shouldHideDock(
                y, previousY, available, dp(48))) {
            dock.animate().translationY(dp(83)).setDuration(245)
                    .setInterpolator(NativeMotion.easing()).start();
            if (speedDialOpen) closeSpeedDial();
        } else if (y < dp(8) || previousY - y >= dp(3)) {
            dock.animate().translationY(0f).setDuration(225)
                    .setInterpolator(NativeMotion.easing()).start();
        }
        boolean show = y >= dp(73);
        if (show == compactShowing) return;
        compactShowing = show;
        int top = NativeShellInsets.currentSafeInsets(screen).top;
        compactHeader.animate().cancel();
        if (show) {
            compactHeader.setVisibility(View.VISIBLE);
            compactHeader.setAlpha(0f);
            compactHeader.setTranslationY(top - dp(15));
            compactHeader.animate().alpha(1f).translationY(top)
                    .setDuration(230).setInterpolator(NativeMotion.easing()).start();
        } else {
            compactHeader.animate().alpha(0f).translationY(top - dp(12))
                    .setDuration(180).setInterpolator(NativeMotion.easing())
                    .withEndAction(() -> {
                        if (!compactShowing) compactHeader.setVisibility(View.INVISIBLE);
                    }).start();
        }
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
     * An architectural reflected-ceiling drawing: a stretch membrane held in a
     * flush shadow-gap profile with perimeter LED and four recessed downlights.
     * The illustration is a wide, shallow ceiling plane, not a 3D box.
     * Native vector Paths are cached, so no bitmap loading or transient
     * allocations are introduced during View drawing or Home transitions.
     */
    private static final class CeilingContour extends View {
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path membrane = polygon(
                21f, 39f, 76f, 25f, 105f, 36f, 50f, 66f);
        private final Path perimeter = polygon(
                8f, 39f, 77f, 13f, 119f, 34f, 50f, 82f);
        private final Path shadowGap = polygon(
                15f, 39f, 77f, 20f, 111f, 35f, 50f, 73f);
        private final Path ledChannel = new Path();
        private final Path magneticTrack = new Path();
        private final PathMeasure contourMeasure = new PathMeasure(perimeter, false);
        private final Path scannedLine = new Path();
        private float scanProgress;
        private ValueAnimator scanAnimator;

        CeilingContour(NativeHomeActivity activity) {
            super(activity);
            // Draw only within the left part of the illustration; leave the
            // primary-card chevron unobstructed to the right.
            ledChannel.moveTo(18f, 43f);
            ledChannel.lineTo(50f, 74f);
            ledChannel.lineTo(110f, 38f);

            magneticTrack.moveTo(42f, 43f);
            magneticTrack.lineTo(83f, 29f);
        }

        void scanOnce() {
            if (!NativeHomeExpressiveMotion.enabled(this)) return;
            if (scanAnimator != null) scanAnimator.cancel();
            scanAnimator = ValueAnimator.ofFloat(0f, 1f);
            scanAnimator.setDuration(485L);
            scanAnimator.setInterpolator(NativeMotion.easing());
            scanAnimator.addUpdateListener(a -> {
                scanProgress = (Float) a.getAnimatedValue();
                invalidate();
            });
            scanAnimator.start();
        }

        private static Path polygon(
                float x0, float y0, float x1, float y1,
                float x2, float y2, float x3, float y3) {
            Path shape = new Path();
            shape.moveTo(x0, y0);
            shape.lineTo(x1, y1);
            shape.lineTo(x2, y2);
            shape.lineTo(x3, y3);
            shape.close();
            return shape;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (getWidth() <= 0 || getHeight() <= 0) return;
            canvas.save();
            canvas.scale(getWidth() / 146f, getHeight() / 100f);

            // The membrane is a continuous, nearly matte plane.
            ink.setStyle(Paint.Style.FILL);
            ink.setColor(Color.argb(24, 238, 250, 255));
            canvas.drawPath(membrane, ink);

            ink.setStrokeJoin(Paint.Join.ROUND);
            ink.setStrokeCap(Paint.Cap.ROUND);
            ink.setStyle(Paint.Style.STROKE);

            // First broad diffused glow, then the precise slim LED strip.
            stroke(3.2f, Color.argb(39, 241, 252, 255));
            canvas.drawPath(ledChannel, ink);
            stroke(.95f, Color.argb(175, 237, 249, 255));
            canvas.drawPath(ledChannel, ink);

            // Flush perimeter and the baguette shadow-gap profile.
            stroke(1.12f, Color.argb(211, 241, 251, 255));
            canvas.drawPath(perimeter, ink);
            if (scanProgress > 0f && scanProgress < 1f) {
                scannedLine.reset();
                contourMeasure.getSegment(0f,
                        contourMeasure.getLength() * scanProgress, scannedLine, true);
                stroke(1.85f, Color.argb(198, 181, 217, 244));
                canvas.drawPath(scannedLine, ink);
            }
            stroke(.72f, Color.argb(136, 237, 249, 255));
            canvas.drawPath(shadowGap, ink);

            // A narrow recessed magnetic track, flush with the membrane;
            // unlike a hanging fixture it has no vertical supports.
            stroke(1.28f, Color.argb(170, 246, 253, 255));
            canvas.drawPath(magneticTrack, ink);
            stroke(.5f, Color.argb(105, 240, 251, 255));
            canvas.drawLine(44f, 46f, 85f, 32f, ink);

            // Four compact downlight rims, foreshortened by the perspective.
            downlight(canvas, 37f, 52f, 3.8f, 2.1f);
            downlight(canvas, 65f, 57f, 4.0f, 2.25f);
            downlight(canvas, 91f, 43f, 3.8f, 1.9f);
            downlight(canvas, 97f, 37f, 2.8f, 1.4f);

            // Dimension ticks identify this as a measured ceiling plan.
            stroke(.65f, Color.argb(74, 230, 246, 255));
            canvas.drawLine(13f, 26f, 71f, 5f, ink);
            canvas.drawLine(12f, 22f, 15f, 30f, ink);
            canvas.drawLine(70f, 2f, 73f, 10f, ink);

            canvas.restore();
        }

        private void stroke(float width, int color) {
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeWidth(width);
            ink.setColor(color);
        }

        private void downlight(Canvas canvas, float cx, float cy, float rx, float ry) {
            ink.setStyle(Paint.Style.FILL);
            ink.setColor(Color.argb(64, 238, 252, 255));
            canvas.drawOval(cx-rx, cy-ry, cx+rx, cy+ry, ink);
            stroke(.8f, Color.argb(212, 251, 255, 255));
            canvas.drawOval(cx-rx, cy-ry, cx+rx, cy+ry, ink);
            stroke(.45f, Color.argb(117, 250, 254, 255));
            canvas.drawOval(cx-rx*.6f, cy-ry*.6f, cx+rx*.6f, cy+ry*.6f, ink);
        }
    }
}
