package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public final class NativePhase2ShellTest {
    private Instrumentation instrumentation;
    private Context context;

    @Before
    public void resetState() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        context.getSharedPreferences(NativeProjectStore.PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit();
        context.getSharedPreferences(NativeUiPrefs.PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test
    public void homeDefault112RespectsStatusInsetAndHasNoHorizontalClipping() throws Exception {
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            AtomicReference<NativeHomeActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            instrumentation.waitForIdleSync();
            NativeHomeActivity activity = ref.get();
            assertNotNull(activity);

            View page = activity.getWindow().getDecorView()
                    .findViewWithTag("native_home_root");
            ScrollView scroll = activity.getWindow().getDecorView()
                    .findViewWithTag("native_home_scroll");
            View hero = activity.getWindow().getDecorView()
                    .findViewWithTag("native_home_hero");
            TextView title = activity.getWindow().getDecorView()
                    .findViewWithTag("native_home_title");

            assertNotNull(page);
            assertNotNull(scroll);
            assertNotNull(hero);
            assertNotNull(title);
            assertWidthTarget(activity);
            assertEquals(1.12f, activity.getUiPrefsForTest().scale, .0001f);

            Insets safe = NativeShellInsets.currentSafeInsets(page);
            assertEquals(safe.top, scroll.getPaddingTop());
            assertEquals(safe.bottom, scroll.getPaddingBottom());

            int[] heroLoc = location(hero);
            assertTrue("home hero must start below status/cutout inset",
                    heroLoc[1] >= safe.top + dp(activity, 32));
            assertShellViewWithinDisplay(activity, hero);
            assertShellViewWithinDisplay(
                    activity,
                    activity.getWindow().getDecorView().findViewWithTag("native_home_new"));
            assertShellViewWithinDisplay(
                    activity,
                    activity.getWindow().getDecorView().findViewWithTag("native_home_saved"));
            assertShellViewWithinDisplay(
                    activity,
                    activity.getWindow().getDecorView().findViewWithTag("native_home_drawing"));
            assertLightSystemBars(activity);

            writeMetric(activity, "home_default",
                    "safeTop=" + safe.top
                            + ",safeBottom=" + safe.bottom
                            + ",heroTop=" + heroLoc[1]
                            + ",titlePx=" + title.getTextSize());
            capture("home_default");
        }
    }

    @Test
    public void workspaceTopUsesAuditedHeaderAndBottomNavigationOrder() throws Exception {
        seedNamedProject("Phase2 объект");
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(workspaceIntent())) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            instrumentation.waitForIdleSync();
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);
            assertWidthTarget(activity);

            View page = activity.getWindow().getDecorView()
                    .findViewWithTag("native_workspace_page");
            ScrollView scroll = activity.getWindow().getDecorView()
                    .findViewWithTag("native_editor_scroll");
            View topbar = activity.getWindow().getDecorView()
                    .findViewWithTag("native_workspace_topbar");
            View nav = activity.getWindow().getDecorView()
                    .findViewWithTag("native_history_nav");

            assertNotNull(page);
            assertNotNull(scroll);
            assertNotNull(topbar);
            assertNotNull(nav);

            Insets safe = NativeShellInsets.currentSafeInsets(page);
            assertEquals(safe.top, scroll.getPaddingTop());
            assertEquals(safe.bottom, scroll.getPaddingBottom());

            int[] topbarLoc = location(topbar);
            assertTrue("workspace header must be below status/cutout inset",
                    topbarLoc[1] >= safe.top);
            assertShellViewWithinDisplay(activity, topbar);
            assertShellViewWithinDisplay(activity, nav);

            ViewGroup navGroup = (ViewGroup) nav;
            assertEquals(3, navGroup.getChildCount());
            assertEquals("native_nav_redo", navGroup.getChildAt(0).getTag());
            assertEquals("native_nav_undo", navGroup.getChildAt(1).getTag());
            assertEquals("native_nav_more", navGroup.getChildAt(2).getTag());
            for (int i = 0; i < 3; i++) {
                View child = navGroup.getChildAt(i);
                assertEquals(dp(activity, NativeVisualTokens.HISTORY_BUTTON_WIDTH_DP),
                        child.getLayoutParams().width);
                assertEquals(dp(activity, NativeVisualTokens.HISTORY_BUTTON_HEIGHT_DP),
                        child.getLayoutParams().height);
            }

            TextView brand = activity.getWindow().getDecorView()
                    .findViewWithTag("native_workspace_brand_title");
            assertNotNull(brand);
            writeMetric(activity, "workspace_top",
                    "safeTop=" + safe.top
                            + ",topbarTop=" + topbarLoc[1]
                            + ",navOrder=redo-undo-more"
                            + ",brandPx=" + brand.getTextSize());
            assertLightSystemBars(activity);
            capture("workspace_top");
        }
    }

    @Test
    public void workspaceBottomKeepsLastContentAboveFixedNavAndSystemBar() throws Exception {
        seedNamedProject("Нижний тест");
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(workspaceIntent())) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);

            scenario.onActivity(a -> {
                ScrollView scroll = a.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                scroll.fullScroll(View.FOCUS_DOWN);
            });
            instrumentation.waitForIdleSync();
            SystemClock.sleep(80);

            View page = activity.getWindow().getDecorView()
                    .findViewWithTag("native_workspace_page");
            View nav = activity.getWindow().getDecorView()
                    .findViewWithTag("native_history_nav");
            View last = activity.getWindow().getDecorView()
                    .findViewWithTag("native_estimate_card");
            ScrollView scroll = activity.getWindow().getDecorView()
                    .findViewWithTag("native_editor_scroll");
            assertNotNull(page);
            assertNotNull(nav);
            assertNotNull(last);
            assertNotNull(scroll);

            Insets safe = NativeShellInsets.currentSafeInsets(page);
            int[] pageLoc = location(page);
            int[] navLoc = location(nav);
            int[] lastLoc = location(last);
            int pageBottom = pageLoc[1] + page.getHeight();
            int navBottom = navLoc[1] + nav.getHeight();
            int lastBottom = lastLoc[1] + last.getHeight();

            assertTrue("bottom nav must remain above system gesture/navigation inset",
                    navBottom <= pageBottom - safe.bottom - dp(activity, 7));
            assertTrue("last workspace content must scroll clear of fixed navigation",
                    lastBottom <= navLoc[1] - dp(activity, 4));
            assertEquals(safe.bottom, scroll.getPaddingBottom());

            View main = activity.getWindow().getDecorView()
                    .findViewWithTag("native_workspace_main");
            assertNotNull(main);
            assertEquals(dp(activity, NativeVisualTokens.SCROLL_NAV_RESERVE_DP),
                    main.getPaddingBottom());

            writeMetric(activity, "workspace_bottom",
                    "safeBottom=" + safe.bottom
                            + ",pageBottom=" + pageBottom
                            + ",navTop=" + navLoc[1]
                            + ",navBottom=" + navBottom
                            + ",lastBottom=" + lastBottom);
            capture("workspace_bottom_fixed_nav");
        }
    }

    @Test
    public void homeTypographyPrefsApplyLivePersistAndHaveNoNightControl() throws Exception {
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            AtomicReference<NativeHomeActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            instrumentation.waitForIdleSync();
            NativeHomeActivity activity = ref.get();
            TextView title = activity.getWindow().getDecorView()
                    .findViewWithTag("native_home_title");
            float before = title.getTextSize();

            scenario.onActivity(a -> a.getWindow().getDecorView()
                    .findViewWithTag("native_home_settings").performClick());
            instrumentation.waitForIdleSync();
            SystemClock.sleep(130);

            NativeUiSettingsDialog dialog = activity.getSettingsDialogForTest();
            assertNotNull(dialog);
            assertTrue(dialog.isShowing());
            View dialogRoot = dialog.getWindow().getDecorView();
            assertNotNull(dialogRoot.findViewWithTag("native_ui_settings_sheet"));
            assertNotNull(dialogRoot.findViewWithTag("native_ui_size_slider"));
            assertFalse(containsText(dialogRoot, "ночн"));
            assertFalse(containsText(dialogRoot, "night"));
            capture("settings_default");

            scenario.onActivity(a -> {
                NativeUiSettingsDialog current = a.getSettingsDialogForTest();
                View serif = current.getWindow().getDecorView()
                        .findViewWithTag("native_ui_font_serif");
                serif.performClick();
            });
            instrumentation.waitForIdleSync();

            scenario.onActivity(a -> {
                NativeUiSettingsDialog current = a.getSettingsDialogForTest();
                current.setScaleForTest(1.24f);
                View bold = current.getWindow().getDecorView()
                        .findViewWithTag("native_ui_weight_bold");
                bold.performClick();
            });
            instrumentation.waitForIdleSync();
            SystemClock.sleep(130);

            assertEquals(NativeUiPrefs.FONT_SERIF, activity.getUiPrefsForTest().font);
            assertEquals(1.24f, activity.getUiPrefsForTest().scale, .0001f);
            assertEquals(NativeUiPrefs.WEIGHT_BOLD, activity.getUiPrefsForTest().weight);
            assertTrue("Home text must update live without Activity restart",
                    title.getTextSize() > before);
            assertEquals(850, title.getTypeface().getWeight());
            capture("settings_custom");

            scenario.onActivity(a -> {
                NativeUiSettingsDialog current = a.getSettingsDialogForTest();
                if (current != null && current.isShowing()) {
                    View done = current.getWindow().getDecorView()
                            .findViewWithTag("native_ui_done");
                    assertNotNull(done);
                    done.performClick();
                }
            });
            instrumentation.waitForIdleSync();
            SystemClock.sleep(130);
            NativeUiSettingsDialog afterSettings = activity.getSettingsDialogForTest();
            assertTrue(afterSettings == null || !afterSettings.isShowing());
            capture("home_custom_prefs");

            NativeUiPrefs persisted = NativeUiPrefs.load(context);
            assertEquals(NativeUiPrefs.FONT_SERIF, persisted.font);
            assertEquals(1.24f, persisted.scale, .0001f);
            assertEquals(NativeUiPrefs.WEIGHT_BOLD, persisted.weight);
            assertFalse(persisted.toJson().has("night"));
        }
    }

    @Test
    public void workspaceUsesSamePersistedPrefsAndCanUpdateThemLive() throws Exception {
        NativeUiPrefs custom = NativeUiPrefs.defaults()
                .withFont(NativeUiPrefs.FONT_MONO)
                .withScale(1.18f)
                .withWeight(NativeUiPrefs.WEIGHT_MEDIUM);
        custom.save(context);
        seedNamedProject("Prefs");

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(workspaceIntent())) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            instrumentation.waitForIdleSync();
            NativeWorkspaceActivity activity = ref.get();
            TextView brand = activity.getWindow().getDecorView()
                    .findViewWithTag("native_workspace_brand_title");
            assertNotNull(brand);
            assertEquals(NativeUiPrefs.FONT_MONO, activity.getUiPrefsForTest().font);
            assertEquals(1.18f, activity.getUiPrefsForTest().scale, .0001f);
            assertEquals(NativeUiPrefs.WEIGHT_MEDIUM, activity.getUiPrefsForTest().weight);
            float before = brand.getTextSize();
            capture("workspace_custom_prefs");

            scenario.onActivity(a -> a.getWindow().getDecorView()
                    .findViewWithTag("native_top_settings").performClick());
            instrumentation.waitForIdleSync();

            scenario.onActivity(a -> a.getSettingsDialogForTest().setScaleForTest(1.30f));
            instrumentation.waitForIdleSync();
            assertEquals(1.30f, activity.getUiPrefsForTest().scale, .0001f);
            assertTrue("Workspace typography must update live", brand.getTextSize() > before);

            scenario.onActivity(a -> a.getSettingsDialogForTest().dismiss());
        }
    }

    @Test
    public void homeToWorkspaceToHomeRoutingPreservesPhase1ProjectData() throws Exception {
        NativeProjectStore store = new NativeProjectStore(context);
        NativeProjectStore.ProjectData project = store.newProject();
        project.name = "Маршрут Phase 2";
        project.phone = "+7 999";
        project.address = "Parity street";
        String orderId = project.id;
        String roomId = project.rooms.get(0).id;
        store.saveCurrent(project);

        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                NativeWorkspaceActivity.class.getName(), null, false);
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            scenario.onActivity(a -> a.getWindow().getDecorView()
                    .findViewWithTag("native_home_drawing").performClick());
            Activity workspace = instrumentation.waitForMonitorWithTimeout(monitor, 4000);
            assertNotNull("Home drawing action must route to native Workspace", workspace);
            assertTrue(workspace instanceof NativeWorkspaceActivity);

            NativeProjectStore.ProjectData inside = new NativeProjectStore(context).loadCurrent();
            assertEquals(orderId, inside.id);
            assertEquals(roomId, inside.rooms.get(0).id);
            assertEquals("Маршрут Phase 2", inside.name);

            instrumentation.runOnMainSync(() -> workspace.getWindow().getDecorView()
                    .findViewWithTag("native_top_home").performClick());
            instrumentation.waitForIdleSync();

            NativeProjectStore.ProjectData after = new NativeProjectStore(context).loadCurrent();
            assertEquals(orderId, after.id);
            assertEquals(roomId, after.rooms.get(0).id);
            assertEquals("Маршрут Phase 2", after.name);
            assertEquals("+7 999", after.phone);
            assertEquals("Parity street", after.address);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void shellTokenConstantsMatchAuditedV184Values() {
        assertEquals(0xFFF2F5F8, NativeVisualTokens.APP_BG);
        assertEquals(0xFFFFFFFF, NativeVisualTokens.SURFACE);
        assertEquals(0xFFD8E2EA, NativeVisualTokens.LINE);
        assertEquals(0xFF1E3144, NativeVisualTokens.TEXT);
        assertEquals(0xFF708294, NativeVisualTokens.MUTED);
        assertEquals(0xFF2F8FBE, NativeVisualTokens.ACCENT);
        assertEquals(13, NativeVisualTokens.CARD_RADIUS_DP);
        assertEquals(7, NativeVisualTokens.APP_COMPACT_GAP_DP);
        assertEquals(84, NativeVisualTokens.HISTORY_BUTTON_WIDTH_DP);
        assertEquals(39, NativeVisualTokens.HISTORY_BUTTON_HEIGHT_DP);
        assertEquals(8, NativeVisualTokens.HISTORY_BOTTOM_DP);
        assertEquals(104, NativeVisualTokens.SCROLL_NAV_RESERVE_DP);
        assertEquals(115L, NativeVisualTokens.OVERLAY_MOTION_MS);
        assertEquals(145L, NativeVisualTokens.COLLAPSE_MOTION_MS);
    }

    private Intent workspaceIntent() {
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void seedNamedProject(String name) {
        NativeProjectStore store = new NativeProjectStore(context);
        NativeProjectStore.ProjectData project = store.newProject();
        project.name = name;
        store.saveCurrent(project);
    }

    private void assertWidthTarget(Activity activity) {
        Bundle args = InstrumentationRegistry.getArguments();
        String raw = args.getString("phase2Width");
        int actual = activity.getResources().getConfiguration().screenWidthDp;
        if (raw == null || raw.isEmpty()) {
            assertTrue("default emulator width must remain phone-class: " + actual,
                    actual >= 350 && actual <= 430);
            return;
        }
        int expected = Integer.parseInt(raw);
        assertTrue("expected ~" + expected + "dp but was " + actual,
                Math.abs(actual - expected) <= 2);
    }

    private void assertShellViewWithinDisplay(Activity activity, View view) {
        assertNotNull(view);
        int[] loc = location(view);
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        assertTrue("left clipping for " + view.getTag(), loc[0] >= 0);
        assertTrue("right clipping for " + view.getTag(),
                loc[0] + view.getWidth() <= width);
    }

    private void assertLightSystemBars(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = decor.getWindowInsetsController();
            assertNotNull(controller);
            int appearance = controller.getSystemBarsAppearance();
            assertTrue((appearance
                    & WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS) != 0);
            assertTrue((appearance
                    & WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS) != 0);
        } else {
            int flags = decor.getSystemUiVisibility();
            assertTrue((flags & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0);
            assertTrue((flags & View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) != 0);
        }
    }

    private int[] location(View view) {
        int[] out = new int[2];
        view.getLocationInWindow(out);
        return out;
    }

    private int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    private boolean containsText(View root, String needle) {
        String query = needle.toLowerCase(Locale.ROOT);
        if (root instanceof TextView) {
            CharSequence value = ((TextView) root).getText();
            if (value != null
                    && value.toString().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsText(group.getChildAt(i), query)) return true;
            }
        }
        return false;
    }

    private void capture(String state) throws Exception {
        instrumentation.waitForIdleSync();
        SystemClock.sleep(60);
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);

        File dir = new File(context.getExternalFilesDir(null), "phase2");
        assertTrue(dir.exists() || dir.mkdirs());
        int widthDp = context.getResources().getConfiguration().screenWidthDp;
        File out = new File(dir,
                String.format(Locale.US, "api%d_%ddp_%s.png",
                        Build.VERSION.SDK_INT, widthDp, state));
        try (FileOutputStream stream = new FileOutputStream(out)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        assertTrue(out.length() > 0L);
    }

    private void writeMetric(Activity activity, String state, String line) throws Exception {
        File dir = new File(context.getExternalFilesDir(null), "phase2");
        assertTrue(dir.exists() || dir.mkdirs());
        int widthDp = activity.getResources().getConfiguration().screenWidthDp;
        File out = new File(dir,
                String.format(Locale.US, "api%d_%ddp_metrics.txt",
                        Build.VERSION.SDK_INT, widthDp));
        try (FileWriter writer = new FileWriter(out, true)) {
            writer.write(state + ":" + line + "\n");
        }
    }
}
