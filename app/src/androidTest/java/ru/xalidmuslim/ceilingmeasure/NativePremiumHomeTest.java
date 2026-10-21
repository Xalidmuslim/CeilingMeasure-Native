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
                assertNotNull(root.findViewWithTag("native_home_saved"));
                assertNotNull(root.findViewWithTag("native_home_drawing"));
                assertNotNull(root.findViewWithTag("native_home_materials"));
                assertNotNull(root.findViewWithTag("native_home_license"));
                assertNotNull(root.findViewWithTag("native_home_settings"));
                assertNotNull(root.findViewWithTag("native_home_recent"));
                ViewGroup dock = root.findViewWithTag("native_home_bottom_nav");
                assertNotNull(dock);
                ViewGroup slots = (ViewGroup) dock.getChildAt(0);
                assertEquals("One compact home dock with five destinations", 5,
                        slots.getChildCount());
                assertNotNull(root.findViewWithTag("native_home_nav_new"));
                assertNull("No top-level export shortcut", root.findViewWithTag("native_home_export"));
                assertNull("No top-level estimate shortcut", root.findViewWithTag("native_home_estimate"));
                assertEquals("Замер потолков",
                        ((TextView) root.findViewWithTag("native_home_title")).getText().toString());
            });
        }
    }
}
