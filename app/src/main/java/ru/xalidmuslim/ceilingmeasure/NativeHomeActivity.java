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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

/** Native v184 Home shell. */
public final class NativeHomeActivity extends ComponentActivity {
    private NativeUiPrefs uiPrefs;
    private NativeProjectStore store;
    private View shellRoot;
    private NativePremiumHomeUi premiumUi;
    private int pendingHomeLoad;
    private TextView savedMeta;
    private TextView drawingMeta;
    private NativeUiSettingsDialog settingsDialog;
    private NativePricePresetDialog homeMaterialsDialog;
    private NativeLicenseClient licenseClient;
    private TextView licenseMeta;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NativeShellInsets.configureWindow(this);
        store = new NativeProjectStore(this);
        uiPrefs = NativeUiPrefs.load(this);
        licenseClient = new NativeLicenseClient(this);
        View content = createContent();
        setContentView(content);
        NativeMotion.enter(content);
        content.requestApplyInsets();
        // The launcher is the terminal in-app destination. Android Back,
        // including gesture navigation, never exits or backgrounds the app.
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){
                if (homeMaterialsDialog != null && homeMaterialsDialog.isShowing()) {
                    homeMaterialsDialog.dismiss();
                    return;
                }
                if(settingsDialog!=null && settingsDialog.isShowing()){
                    settingsDialog.dismiss();
                }
                // Already on Home: intentionally remain here.
            }
        });
        // First launch automatically starts the server-anchored 7-day trial.
        // If offline, the existing License dialog offers a manual retry.
        if (NativeLicenseClient.enabled() && licenseClient.recoveryCode().isEmpty()) {
            licenseClient.startTrial((ok, message) -> {
                if (!isFinishing()) refreshLicenseOnly();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiPrefs = NativeUiPrefs.load(this);
        if (shellRoot != null) NativeTypography.applyTree(shellRoot, uiPrefs);
        refreshHomeMeta();
        if (NativeLicenseClient.enabled() && !licenseClient.recoveryCode().isEmpty()) {
            licenseClient.refresh((success, message) -> refreshLicenseOnly());
        }
    }

    private View createContent() {
        premiumUi = new NativePremiumHomeUi(
                this, uiPrefs,
                () -> openWorkspace(NativeWorkspaceActivity.ACTION_NEW_ORDER),
                () -> openWorkspace(NativeWorkspaceActivity.ACTION_SAVED_MEASUREMENTS),
                this::openRecentProject,
                () -> openWorkspace(NativeWorkspaceActivity.ACTION_DRAWING),
                this::openMaterialsDirectly,
                this::showLicense,
                this::openSettings);
        View screen = premiumUi.build();
        shellRoot = screen;
        NativeShellInsets.bindWorkspace(
                screen, premiumUi.scroll(), premiumUi.dock(), dp(8));
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
                ? (widthDp <= 430 ? 78 : 88)
                : ("native_home_license".equals(tag) ? 60
                    : (widthDp <= 430 ? 70 : 78))));
        card.setTag(tag);
        card.setContentDescription(title);
        card.setClickable(true);
        card.setFocusable(true);
        card.setClipToOutline(false);
        card.setElevation(0f);

        Drawable content = primary
                ? NativeVisualTokens.homePrimary(dp(radius), 0)
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
        // No physical shrink/jump on touch; the material ripple communicates press.
        card.setOnClickListener(listener);

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
                primary ? 17 : 15,
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
                : ("Чертёж".equals(title) ? "native_home_drawing_meta"
                : ("Лицензия".equals(title) ? "native_home_license_meta" : null)));
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

    /**
     * The price catalog is a fullscreen native Dialog owned by Home itself.
     * No WorkspaceActivity or drawing view is created during navigation.
     * Android Back merely dismisses the dialog and exposes this exact Home
     * instance, preserving scroll offset, in-flight drawings and UI state.
     */
    private void openMaterialsDirectly() {
        if (homeMaterialsDialog != null && homeMaterialsDialog.isShowing()) return;
        if (licenseClient != null && !licenseClient.canEdit()) {
            showLicense();
            return;
        }
        NativeProjectStore.ProjectData active = store.peekCurrent();
        if (active == null) active = new NativeProjectStore.ProjectData();

        NativePricePresetDialog dialog = new NativePricePresetDialog(
                this, store, active, java.util.Collections.singletonList(active),
                false, () -> {
                    // The catalog saves default prices and real orders itself.
                    // Home owns no editor fields and must not schedule a save.
                });
        dialog.setImmediateOpen(true);
        homeMaterialsDialog = dialog;
        dialog.setOnDismissListener(window -> {
            if (homeMaterialsDialog == dialog) homeMaterialsDialog = null;
        });
        dialog.show();
    }

    boolean isHomeMaterialsVisibleForTest() {
        return homeMaterialsDialog != null && homeMaterialsDialog.isShowing();
    }

    @Override
    protected void onDestroy() {
        if (homeMaterialsDialog != null && homeMaterialsDialog.isShowing()) {
            homeMaterialsDialog.dismiss();
        }
        super.onDestroy();
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

    private void refreshLicenseOnly() {
        if (premiumUi != null) premiumUi.refreshLicense(licenseClient);
    }

    private void refreshHomeMeta() {
        refreshLicenseOnly();
        if (premiumUi == null) return;
        final int generation = ++pendingHomeLoad;
        final NativePremiumHomeUi page = premiumUi;
        NativeHomePreloader.load(this, (saved, icon) -> {
            if (isFinishing() || isDestroyed() || generation != pendingHomeLoad
                    || page != premiumUi) return;
            page.applyPreloadedIcon(icon);
            page.renderSaved(saved);
        });
    }

    @Override
    protected void onStop() {
        // A stale queued decode must not redraw the launcher while another
        // screen owns the window. onResume requests the latest saved snapshot.
        pendingHomeLoad++;
        super.onStop();
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

    private void showLicense() {
        NativeLicenseDialog.show(this, licenseClient, this::refreshHomeMeta);
    }

    private void openRecentProject(String projectId) {
        if (licenseClient != null && !licenseClient.canEdit()) { showLicense(); return; }
        Intent intent = new Intent(this, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION, NativeWorkspaceActivity.ACTION_RECENT_PROJECT);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_PROJECT_ID, projectId);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void openWorkspace(String action) {
        // Gate entry in one place. Existing projects are never erased or migrated.
        if (licenseClient != null && !licenseClient.canEdit()) {
            showLicense();
            return;
        }
        Intent intent = new Intent(this, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION, action);
        startActivity(intent);
        if (NativeWorkspaceActivity.ACTION_MATERIALS.equals(action)) overridePendingTransition(0, 0);
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
