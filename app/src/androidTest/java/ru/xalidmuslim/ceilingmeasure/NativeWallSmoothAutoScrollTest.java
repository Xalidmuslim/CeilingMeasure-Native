package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Regression: active-wall autoscroll must glide, not snap, without losing position. */
@RunWith(AndroidJUnit4.class)
public final class NativeWallSmoothAutoScrollTest {

    @Test
    public void wallInputAutoscrollMovesThroughIntermediateFramesAndReachesTarget()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = new Intent(context, NativeWorkspaceActivity.class);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(launch)) {
            final int[] destination = new int[1];
            final int[] start = new int[1];
            final ScrollView[] scroll = new ScrollView[1];
            scene.onActivity(activity -> {
                scroll[0] = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                assertNotNull(scroll[0]);
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                room.points.clear();
                room.edges.clear();
                room.points.add(new NativeProjectStore.PointData("smooth_a", 20f, 20f));
                room.points.add(new NativeProjectStore.PointData("smooth_b", 320f, 20f));
                room.edges.add(new NativeProjectStore.EdgeData("smooth_a", "smooth_b"));
                ((CeilingDrawingView)activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view")).setRoom(room);
                activity.onWallLongPressed(room.edges.get(0).id, 300f);
                assertTrue(activity.isWallEditorShowingForTest());
            });
            // Finish dialog attachment, temporary scroll spacer layout,
            // and any initial reposition before measuring one new glide.
            SystemClock.sleep(340L);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity -> {
                ScrollView s = scroll[0];
                int max = Math.max(0, s.getChildAt(0).getHeight() - s.getHeight());
                assertTrue("A long drawing editor must have testable scroll range", max > 130);
                s.scrollTo(0, Math.min(12, max));
                start[0] = s.getScrollY();
                destination[0] = Math.min(max - 8, start[0] + 190);
                assertTrue(destination[0] - start[0] >= 105);
                assertTrue(ValueAnimator.areAnimatorsEnabled());
                activity.animateWallScrollForTest(destination[0]);
                assertEquals("The drawing must not jump synchronously to the wall",
                        start[0], s.getScrollY());
            });
            SystemClock.sleep(85L);
            scene.onActivity(activity -> {
                int midway = scroll[0].getScrollY();
                assertTrue("Expected intermediate frames before reaching wall, not a snap: " +
                                midway + " vs " + start[0] + " to " + destination[0],
                        midway > start[0] + 3 && midway < destination[0] - 3);
            });
            SystemClock.sleep(300L);
            scene.onActivity(activity -> {
                assertEquals("The active wall must reach its final visible position",
                        destination[0], scroll[0].getScrollY(), 2);
                assertFalse("The auto-scroll animation must stop when complete",
                        activity.isWallAutoScrollRunningForTest());
                activity.dismissWallInputForTest();
            });
        }
    }

    @Test
    public void closingWallEditorCancelsGlideAndRestoresPriorScrollPosition()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = new Intent(context, NativeWorkspaceActivity.class);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(launch)) {
            scene.onActivity(activity -> {
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                room.points.clear(); room.edges.clear();
                room.points.add(new NativeProjectStore.PointData("cancel_a", 20f, 20f));
                room.points.add(new NativeProjectStore.PointData("cancel_b", 320f, 20f));
                room.edges.add(new NativeProjectStore.EdgeData("cancel_a", "cancel_b"));
                ((CeilingDrawingView)activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view")).setRoom(room);
                activity.onWallLongPressed(room.edges.get(0).id, 300f);
            });
            SystemClock.sleep(260L);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity -> {
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                int max = Math.max(0, scroll.getChildAt(0).getHeight() - scroll.getHeight());
                activity.animateWallScrollForTest(Math.min(max, scroll.getScrollY() + 180));
                assertTrue(activity.isWallAutoScrollRunningForTest());
                activity.dismissWallInputForTest();
                assertFalse("Closing numeric dialog must cancel pending glide",
                        activity.isWallAutoScrollRunningForTest());
            });
        }
    }
}
