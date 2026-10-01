package ru.xalidmuslim.ceilingmeasure;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

/** Native v184 Home shell. */
public final class NativeHomeActivity extends ComponentActivity {
    private NativeUiPrefs uiPrefs;
    private NativeProjectStore store;
    private View shellRoot;
    private TextView savedMeta;
    private TextView drawingMeta;
    private NativeUiSettingsDialog settingsDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NativeShellInsets.configureWindow(this);
        store = new NativeProjectStore(this);
        uiPrefs = NativeUiPrefs.load(this);
        View content = createContent();
        setContentView(content);
        NativeMotion.enter(content);
        content.requestApplyInsets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiPrefs = NativeUiPrefs.load(this);
        if (shellRoot != null) NativeTypography.applyTree(shellRoot, uiPrefs);
        refreshHomeMeta();
    }

    private View createContent() {
        LinearLayout screen = new LinearLayout(this);
        screen.setTag("native_home_root");
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackground(NativeVisualTokens.homeBackground());

        ScrollView scroll = new ScrollView(this);
        scroll.setTag("native_home_scroll");
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        screen.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setTag("native_home_content");
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(10), dp(32), dp(10), dp(18));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout shell = new LinearLayout(this);
        shell.setTag("native_home_shell");
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(shell, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout hero = new LinearLayout(this);
        hero.setTag("native_home_hero");
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.TOP);
        hero.setPadding(dp(2), dp(6), dp(2), dp(14));
        shell.addView(hero, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        hero.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView kicker = text(
                "РАБОЧЕЕ ПРИЛОЖЕНИЕ",
                11,
                NativeVisualTokens.HOME_KICKER,
                true);
        kicker.setLetterSpacing(.04f);
        copy.addView(kicker);

        int screenWidthDp = Math.round(
                getResources().getDisplayMetrics().widthPixels
                        / getResources().getDisplayMetrics().density);
        TextView title = text(
                "Замер потолков",
                screenWidthDp <= 430 ? 24 : 27,
                Color.rgb(32, 56, 74),
                true);
        title.setTag("native_home_title");
        LinearLayout.LayoutParams titleParams = wrap();
        titleParams.topMargin = dp(4);
        copy.addView(title, titleParams);

        TextView subtitle = text(
                "Замер · чертёж · смета",
                12.5f,
                NativeVisualTokens.HOME_MUTED,
                false);
        LinearLayout.LayoutParams subtitleParams = wrap();
        subtitleParams.topMargin = dp(7);
        copy.addView(subtitle, subtitleParams);

        ImageView settings = iconButton(R.drawable.ic_shell_sliders, 42, 42, 14);
        settings.setTag("native_home_settings");
        settings.setContentDescription("Настройки");
        settings.setOnClickListener(v -> openSettings());
        LinearLayout.LayoutParams settingsParams =
                new LinearLayout.LayoutParams(dp(42), dp(42));
        settingsParams.topMargin = dp(2);
        hero.addView(settings, settingsParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setTag("native_home_actions");
        actions.setOrientation(LinearLayout.VERTICAL);
        shell.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        View newOrder = actionCard(
                R.drawable.ic_shell_add,
                "Новый заказ",
                "Создать новый замер",
                true,
                "native_home_new",
                v -> openWorkspace(NativeWorkspaceActivity.ACTION_NEW_ORDER));
        actions.addView(newOrder, cardParams(0));

        View saved = actionCard(
                R.drawable.ic_shell_saved,
                "Замеры",
                "",
                false,
                "native_home_saved",
                v -> openWorkspace(NativeWorkspaceActivity.ACTION_SAVED_MEASUREMENTS));
        actions.addView(saved, cardParams(7));
        savedMeta = saved.findViewWithTag("native_home_saved_meta");

        View drawing = actionCard(
                R.drawable.ic_shell_drawing,
                "Чертёж",
                "",
                false,
                "native_home_drawing",
                v -> openWorkspace(NativeWorkspaceActivity.ACTION_DRAWING));
        actions.addView(drawing, cardParams(7));
        drawingMeta = drawing.findViewWithTag("native_home_drawing_meta");

        shellRoot = screen;
        NativeShellInsets.bindHome(screen, scroll);
        refreshHomeMeta();
        return screen;
    }

    private View actionCard(
            @DrawableRes int iconRes,
            String title,
            String subtitle,
            boolean primary,
            String tag,
            View.OnClickListener listener) {
        int widthDp = Math.round(
                getResources().getDisplayMetrics().widthPixels
                        / getResources().getDisplayMetrics().density);
        int radius = widthDp <= 430
                ? NativeVisualTokens.HOME_RADIUS_MOBILE_DP
                : NativeVisualTokens.HOME_RADIUS_WIDE_DP;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(widthDp <= 430 ? 12 : 14), dp(widthDp <= 430 ? 11 : 13),
                dp(widthDp <= 430 ? 12 : 14), dp(widthDp <= 430 ? 11 : 13));
        card.setMinimumHeight(dp(primary
                ? (widthDp <= 430 ? 92 : 104)
                : (widthDp <= 430 ? 80 : 94)));
        card.setTag(tag);
        card.setContentDescription(title);
        card.setClickable(true);
        card.setFocusable(true);
        card.setClipToOutline(false);
        card.setElevation(dp(primary ? 4 : 1));

        Drawable content = primary
                ? NativeVisualTokens.homePrimary(dp(radius), dp(1))
                : NativeVisualTokens.outlined(
                        NativeVisualTokens.SURFACE,
                        NativeVisualTokens.HOME_CARD_LINE,
                        dp(radius),
                        dp(1));
        GradientDrawable mask = NativeVisualTokens.solid(Color.WHITE, dp(radius));
        int ripple = primary
                ? Color.argb(36, 255, 255, 255)
                : Color.argb(20, 36, 59, 77);
        card.setBackground(new RippleDrawable(
                ColorStateList.valueOf(ripple),
                content,
                mask));
        card.setOnClickListener(listener);
        card.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.setScaleX(.985f);
                v.setScaleY(.985f);
            } else if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
            return false;
        });

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        if (!primary) icon.setImageTintList(
                ColorStateList.valueOf(NativeVisualTokens.HOME_ICON));
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setBackground(NativeVisualTokens.solid(
                primary ? Color.argb(41, 255, 255, 255)
                        : NativeVisualTokens.HOME_ICON_BG,
                dp(15)));
        card.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        copyParams.leftMargin = dp(10);
        copyParams.rightMargin = dp(8);
        card.addView(copy, copyParams);

        TextView titleView = text(
                title,
                16,
                primary ? Color.WHITE : NativeVisualTokens.HOME_CARD_TEXT,
                true);
        copy.addView(titleView);

        TextView subtitleView = text(
                subtitle,
                11.5f,
                primary ? Color.argb(204, 255, 255, 255)
                        : NativeVisualTokens.HOME_CARD_MUTED,
                false);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        subtitleView.setTag("Замеры".equals(title)
                ? "native_home_saved_meta"
                : ("Чертёж".equals(title) ? "native_home_drawing_meta" : null));
        LinearLayout.LayoutParams subtitleLp = wrap();
        subtitleLp.topMargin = dp(2);
        copy.addView(subtitleView, subtitleLp);

        TextView chevron = text(
                "›",
                30,
                primary ? Color.argb(204, 255, 255, 255)
                        : NativeVisualTokens.HOME_CHEVRON,
                false);
        chevron.setGravity(Gravity.CENTER);
        card.addView(chevron, new LinearLayout.LayoutParams(dp(18), dp(42)));

        return card;
    }

    private ImageView iconButton(
            @DrawableRes int iconRes,
            int widthDp,
            int heightDp,
            int radiusDp) {
        ImageView view = new ImageView(this);
        view.setImageResource(iconRes);
        view.setPadding(dp(9), dp(9), dp(9), dp(9));
        view.setClickable(true);
        view.setFocusable(true);
        GradientDrawable content = NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,
                NativeVisualTokens.HOME_SETTINGS_LINE,
                dp(radiusDp),
                dp(1));
        GradientDrawable mask = NativeVisualTokens.solid(Color.WHITE, dp(radiusDp));
        view.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(20, 35, 56, 72)),
                content,
                mask));
        return view;
    }

    private void openSettings() {
        if (settingsDialog != null && settingsDialog.isShowing()) return;
        settingsDialog = new NativeUiSettingsDialog(this, uiPrefs, next -> {
            uiPrefs = next;
            if (shellRoot != null) NativeTypography.applyTree(shellRoot, uiPrefs);
        });
        settingsDialog.setOnDismissListener(dialog -> settingsDialog = null);
        settingsDialog.show();
    }

    private void refreshHomeMeta() {
        if (store == null) return;
        if (savedMeta != null) {
            int count = store.loadSaved().size();
            savedMeta.setText(count > 0 ? count + " сохранённых" : "Сохранённых пока нет");
        }
        if (drawingMeta != null) {
            NativeProjectStore.ProjectData project = store.loadCurrent();
            String orderName = project.name == null || project.name.trim().isEmpty()
                    ? "Текущий заказ" : project.name.trim();
            String roomName = project.rooms.isEmpty()
                    ? "Комната 1" : project.rooms.get(0).name;
            drawingMeta.setText(orderName + " · " + roomName);
        }
    }

    private TextView text(
            String value,
            float baseSp,
            int color,
            boolean strong) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        return NativeTypography.register(view, baseSp, strong, uiPrefs);
    }

    private LinearLayout.LayoutParams cardParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMarginDp);
        return params;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void openWorkspace(String action) {
        Intent intent = new Intent(this, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION, action);
        startActivity(intent);
    }

    NativeUiPrefs getUiPrefsForTest() {
        return uiPrefs;
    }

    @Nullable
    NativeUiSettingsDialog getSettingsDialogForTest() {
        return settingsDialog;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
