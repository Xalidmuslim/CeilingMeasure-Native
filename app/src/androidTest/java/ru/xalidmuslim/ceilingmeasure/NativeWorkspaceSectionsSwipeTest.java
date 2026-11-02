package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Collapse UI uses the same transition for order, rooms and overview. */
@RunWith(AndroidJUnit4.class)
public final class NativeWorkspaceSectionsSwipeTest {
    @Test public void threeSectionsExpandAndCollapseWithoutLosingTheirContent() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try (ActivityScenario<NativeWorkspaceActivity> scenario = ActivityScenario.launch(intent)) {
            String[] heads = {"native_order_header", "native_rooms_header",
                    "native_project_overview_header"};
            String[] bodies = {"native_order_body", "native_rooms_body",
                    "native_project_overview_body"};
            for (int i = 0; i < heads.length; i++) {
                final int index = i;
                scenario.onActivity(activity -> {
                    View root = activity.getWindow().getDecorView();
                    View head = root.findViewWithTag(heads[index]);
                    View body = root.findViewWithTag(bodies[index]);
                    assertNotNull(head);
                    assertNotNull(body);
                    assertEquals(View.VISIBLE, body.getVisibility());
                    head.performClick();
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                android.os.SystemClock.sleep(NativeSectionMotion.DURATION_MS + 95);
                scenario.onActivity(activity -> {
                    View root = activity.getWindow().getDecorView();
                    View body = root.findViewWithTag(bodies[index]);
                    assertEquals("Block should collapse smoothly to GONE",
                            View.GONE, body.getVisibility());
                    assertEquals(1f, body.getAlpha(), .001f);
                    assertTrue("Content remains attached", ((LinearLayout) body).getChildCount() > 0);
                    root.findViewWithTag(heads[index]).performClick();
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                android.os.SystemClock.sleep(NativeSectionMotion.DURATION_MS + 95);
                scenario.onActivity(activity -> {
                    View root = activity.getWindow().getDecorView();
                    View body = root.findViewWithTag(bodies[index]);
                    assertEquals(View.VISIBLE, body.getVisibility());
                    assertEquals(1f, body.getAlpha(), .001f);
                    assertEquals(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                            body.getLayoutParams().height);
                });
            }
        }
    }

    @Test public void drawingHeaderRemainsASafeSwipeTarget() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try (ActivityScenario<NativeWorkspaceActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                View root = activity.getWindow().getDecorView();
                View swipe = root.findViewWithTag("native_drawing_room_swipe_target");
                View drawing = root.findViewWithTag("native_drawing_view");
                assertNotNull(swipe);
                assertNotNull(drawing);
                assertTrue(swipe.isClickable());
                assertTrue(swipe.getContentDescription().toString().contains("Свайп"));
            });
        }
    }
}
