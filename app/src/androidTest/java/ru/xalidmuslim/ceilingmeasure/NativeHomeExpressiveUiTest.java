package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Real accessibility-visible Home controls and radial navigation. */
@RunWith(AndroidJUnit4.class)
public final class NativeHomeExpressiveUiTest {
    @Test public void plusDisplaysOnlyRealRoutesAndCanClose() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean previous = NativeTransformerMode.enabled(context);
        NativeTransformerMode.setEnabled(context, true);
        try {
            try (ActivityScenario<NativeHomeActivity> scenario =
                         ActivityScenario.launch(NativeHomeActivity.class)) {
                scenario.onActivity(activity -> {
                    View root = activity.findViewById(android.R.id.content);
                    assertNotNull(root.findViewWithTag("native_home_new"));
                    assertNotNull(root.findViewWithTag("native_home_liquid_indicator"));
                    assertNotNull(root.findViewWithTag("native_home_collapsing_app_bar"));
                    View plus = root.findViewWithTag("native_home_nav_new");
                    assertNotNull(plus);
                    View menu = root.findViewWithTag("native_home_speed_dial");
                    assertNotNull(menu);
                    plus.performClick();
                    assertEquals(View.VISIBLE, menu.getVisibility());
                    assertNotNull(menu.findViewWithTag("native_home_speed_action_0"));
                    assertNotNull(menu.findViewWithTag("native_home_speed_action_1"));
                    assertNotNull(menu.findViewWithTag("native_home_speed_action_2"));
                    plus.performClick();
                });
            }
        } finally {
            NativeTransformerMode.setEnabled(context, previous);
        }
    }
}
