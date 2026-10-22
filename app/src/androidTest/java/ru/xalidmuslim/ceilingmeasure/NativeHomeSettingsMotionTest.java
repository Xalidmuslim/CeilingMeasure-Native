package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression test for a single, stable Settings fade over the existing Home.
 * Both the top-right settings icon and the dock must follow the same path.
 */
@RunWith(AndroidJUnit4.class)
public final class NativeHomeSettingsMotionTest {

    @Test
    public void topSettingsUsesOneFadeLayerWithoutWindowAnimationOrLayoutShift() {
        try (ActivityScenario<NativeHomeActivity> scene =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            scene.onActivity(home -> {
                View headerSettings = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_settings");
                assertNotNull(headerSettings);
                headerSettings.performClick();
                NativeUiSettingsDialog dialog = home.getSettingsDialogForTest();
                assertNotNull(dialog);
                assertTrue(dialog.isShowing());
                assertTrue("Home Settings must be a single alpha transition",
                        dialog.isStableHomeTransitionForTest());
                assertEquals("Use explicit no-op window style; 0 allows Android's fallback",
                        R.style.CeilingNoWindowAnimation,
                        dialog.getWindow().getAttributes().windowAnimations);
                assertEquals(0, dialog.getWindow().getAttributes().flags
                        & WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                View sheet = dialog.getWindow().getDecorView()
                        .findViewWithTag("native_ui_settings_sheet");
                assertNotNull(sheet);
                assertEquals(1f, sheet.getScaleX(), .001f);
                assertEquals(1f, sheet.getScaleY(), .001f);
                assertEquals(0f, sheet.getTranslationY(), .001f);
                assertNotEquals(View.LAYER_TYPE_HARDWARE, sheet.getLayerType());
            });
        }
    }

    @Test
    public void dockSettingsAndroidBackKeepsExactSameHomeAndScrollPosition()
            throws Exception {
        try (ActivityScenario<NativeHomeActivity> scene =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            final NativeHomeActivity[] original = {null};
            final ScrollView[] homeScroll = {null};
            final int[] before = {0};
            scene.onActivity(home -> {
                original[0] = home;
                homeScroll[0] = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_scroll");
                assertNotNull(homeScroll[0]);
                before[0] = homeScroll[0].getScrollY();
                ViewGroup dock = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_bottom_nav");
                assertNotNull(dock);
                ViewGroup slots = (ViewGroup) dock.getChildAt(0);
                slots.getChildAt(4).performClick();
                assertNotNull(home.getSettingsDialogForTest());
                assertTrue(home.getSettingsDialogForTest().isShowing());
            });

            // Wait for the Settings Dialog window (not the Activity) to own
            // input focus; API35 emulators can dispatch Back to Home if sent
            // during the initial window attachment.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            final boolean[] focused = {false};
            for (int attempt = 0; attempt < 22; attempt++) {
                scene.onActivity(home -> {
                    NativeUiSettingsDialog dialog = home.getSettingsDialogForTest();
                    focused[0] = dialog != null && dialog.isShowing()
                            && dialog.getWindow() != null
                            && dialog.getWindow().getDecorView().hasWindowFocus();
                });
                if (focused[0]) break;
                Thread.sleep(65L);
            }
            assertTrue("Settings dialog must receive Back focus", focused[0]);
            InstrumentationRegistry.getInstrumentation()
                    .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);

            // Observe the final async dismiss state, not an intermediate frame.
            final boolean[] closed = {false};
            for (int attempt = 0; attempt < 25; attempt++) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scene.onActivity(home -> {
                    NativeUiSettingsDialog dialog = home.getSettingsDialogForTest();
                    closed[0] = dialog == null || !dialog.isShowing();
                });
                if (closed[0]) break;
                Thread.sleep(55L);
            }
            assertTrue("Settings must be dismissed after system Back", closed[0]);

            scene.onActivity(home -> {
                assertSame("No second Home Activity must be created", original[0], home);
                assertSame("The original scroll view must be retained",
                        homeScroll[0], home.getWindow().getDecorView()
                                .findViewWithTag("native_home_scroll"));
                assertEquals("Home must return to precisely the same scroll offset",
                        before[0], homeScroll[0].getScrollY());
                NativeUiSettingsDialog dialog = home.getSettingsDialogForTest();
                assertTrue("Settings should be completely dismissed after Back",
                        dialog == null || !dialog.isShowing());
                assertNotNull(home.getWindow().getDecorView()
                        .findViewWithTag("native_home_recent"));
            });
        }
    }
}
