package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** UI regressions for the compact overview and perceptible ordinary window fades. */
@RunWith(AndroidJUnit4.class)
public final class NativeCompactOverviewMotionTest {
    @Test public void overallAreaAndPerimeterStayCompactWithoutRemovingRoomMetrics() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try (ActivityScenario<NativeWorkspaceActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                TextView area = activity.findViewById(android.R.id.content)
                        .findViewWithTag("native_project_total_area");
                TextView perimeter = activity.findViewById(android.R.id.content)
                        .findViewWithTag("native_project_total_perimeter");
                android.widget.LinearLayout roomLines = activity.findViewById(android.R.id.content)
                        .findViewWithTag("native_project_room_metrics");
                assertNotNull(area);
                assertNotNull(perimeter);
                assertNotNull(roomLines);
                android.widget.LinearLayout first =
                        (android.widget.LinearLayout) roomLines.getChildAt(0);
                assertEquals("Every room is one aligned three-column row", 3,
                        first.getChildCount());
                assertEquals("Numeric area is right aligned",
                        android.view.Gravity.RIGHT,
                        ((TextView) first.getChildAt(1)).getGravity()
                                & android.view.Gravity.HORIZONTAL_GRAVITY_MASK);
                assertEquals("Numeric perimeter is right aligned",
                        android.view.Gravity.RIGHT,
                        ((TextView) first.getChildAt(2)).getGravity()
                                & android.view.Gravity.HORIZONTAL_GRAVITY_MASK);
                float density = activity.getResources().getDisplayMetrics().scaledDensity;
                float userScale = NativeUiPrefs.load(activity).scale;
                assertTrue("Combined area must not use headline sized type",
                        area.getTextSize() / density <= 16.5f * userScale);
                assertTrue("Combined perimeter must not use headline sized type",
                        perimeter.getTextSize() / density <= 16.5f * userScale);
                assertTrue("Per-room measurements remain immediately visible",
                        ((TextView) ((android.widget.LinearLayout) roomLines.getChildAt(0))
                                .getChildAt(0)).getText().toString().contains("Комната 1"));
            });
        }
    }

    @Test public void blueDockSelectionRemainsVisibleInOrdinaryMode() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean previous = NativeTransformerMode.enabled(context);
        NativeTransformerMode.setEnabled(context, false);
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            scenario.onActivity(activity -> {
                android.view.View indicator = activity.findViewById(android.R.id.content)
                        .findViewWithTag("native_home_liquid_indicator");
                assertNotNull(indicator);
                assertEquals("Ordinary mode retains the simple sliding selection",
                        android.view.View.VISIBLE, indicator.getVisibility());
            });
        } finally {
            NativeTransformerMode.setEnabled(context, previous);
        }
    }

    @Test public void fullscreenDialogsUseOnlyWindowEnterAnimation() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.res.TypedArray style = context.obtainStyledAttributes(
                R.style.CeilingFullscreenEnterOnly, new int[]{
                        android.R.attr.windowEnterAnimation,
                        android.R.attr.windowExitAnimation});
        try {
            assertTrue("Entry must be animated", style.getResourceId(0, 0) != 0);
            assertEquals("Exit must be handled by one smooth decor animation",
                    0, style.getResourceId(1, 0));
        } finally {
            style.recycle();
        }
    }
}
