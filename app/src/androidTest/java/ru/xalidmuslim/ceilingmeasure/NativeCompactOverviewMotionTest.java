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
                TextView roomLines = activity.findViewById(android.R.id.content)
                        .findViewWithTag("native_project_room_metrics");
                assertNotNull(area);
                assertNotNull(perimeter);
                assertNotNull(roomLines);
                float density = activity.getResources().getDisplayMetrics().scaledDensity;
                float userScale = NativeUiPrefs.load(activity).scale;
                assertTrue("Combined area must not use headline sized type",
                        area.getTextSize() / density <= 16.5f * userScale);
                assertTrue("Combined perimeter must not use headline sized type",
                        perimeter.getTextSize() / density <= 16.5f * userScale);
                assertTrue("Per-room measurements remain immediately visible",
                        roomLines.getText().toString().contains("Комната 1"));
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

    @Test public void estimateAndMaterialsUseVisibleButOrdinaryFadeInAndOut() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Animation enter = AnimationUtils.loadAnimation(context, R.anim.native_fade_in);
        Animation exit = AnimationUtils.loadAnimation(context, R.anim.native_fade_out);
        assertTrue("Entrance fade should be perceptible", enter.getDuration() >= 170);
        assertTrue("Exit fade should be perceptible", exit.getDuration() >= 140);
        assertTrue("No long theatrical entry", enter.getDuration() <= 230);
        assertTrue("No long theatrical exit", exit.getDuration() <= 210);
    }
}
