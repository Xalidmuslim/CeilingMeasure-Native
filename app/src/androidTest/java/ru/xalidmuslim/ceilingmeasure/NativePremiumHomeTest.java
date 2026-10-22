package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Regression guard: premium launcher remains native and preserves every action. */
@RunWith(AndroidJUnit4.class)
public final class NativePremiumHomeTest {
    @Test
    public void launcherRendersApprovedCompactSectionsAndRealAppIcon() {
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            scenario.onActivity(activity -> {
                View root = activity.getWindow().getDecorView();
                assertNotNull(root.findViewWithTag("native_home_root"));
                assertNotNull(root.findViewWithTag("native_home_app_icon"));
                assertNotNull(root.findViewWithTag("native_home_new"));
                View ceilingArt = root.findViewWithTag("native_home_ceiling_art");
                assertNotNull("Native architectural ceiling illustration is present", ceilingArt);
                assertEquals("Ceiling illustration must not force an extra GPU layer",
                        View.LAYER_TYPE_NONE, ceilingArt.getLayerType());
                assertNotNull(root.findViewWithTag("native_home_saved"));
                assertNotNull(root.findViewWithTag("native_home_drawing"));
                assertNull("Materials shortcut was removed from home cards",
                        root.findViewWithTag("native_home_materials"));
                assertNotNull(root.findViewWithTag("native_home_license"));
                assertNotNull(root.findViewWithTag("native_home_settings"));
                assertNotNull(root.findViewWithTag("native_home_recent"));
                ViewGroup dock = root.findViewWithTag("native_home_bottom_nav");
                assertNotNull(dock);
                ViewGroup slots = (ViewGroup) dock.getChildAt(0);
                assertEquals("One compact home dock with five destinations", 5,
                        slots.getChildCount());
                assertNotNull(root.findViewWithTag("native_home_nav_new"));
                assertEquals("Materials remains accessible in the bottom navigation",
                        "Материалы", String.valueOf(slots.getChildAt(3).getContentDescription()));
                assertNoExplicitHardwareLayers(root.findViewWithTag("native_home_root"));
                assertNull("No top-level export shortcut", root.findViewWithTag("native_home_export"));
                assertNull("No top-level estimate shortcut", root.findViewWithTag("native_home_estimate"));
                assertEquals("Замер потолков",
                        ((TextView) root.findViewWithTag("native_home_title")).getText().toString());
            });
        }
    }

    @Test
    public void recentProjectIntentUsesStableIdAndNotSavedListAction() {
        assertNotEquals(NativeWorkspaceActivity.ACTION_SAVED_MEASUREMENTS,
                NativeWorkspaceActivity.ACTION_RECENT_PROJECT);
        assertNotEquals(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.EXTRA_PROJECT_ID);
    }

    private static void assertNoExplicitHardwareLayers(View view) {
        assertNotEquals("Unnecessary forced GPU layer: " + view.getTag(),
                View.LAYER_TYPE_HARDWARE, view.getLayerType());
        if (view instanceof ViewGroup) {
            ViewGroup children = (ViewGroup) view;
            for (int n = 0; n < children.getChildCount(); n++) {
                assertNoExplicitHardwareLayers(children.getChildAt(n));
            }
        }
    }
}
