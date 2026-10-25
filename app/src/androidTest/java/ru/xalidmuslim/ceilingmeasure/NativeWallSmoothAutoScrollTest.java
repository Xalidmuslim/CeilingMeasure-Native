package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.ScrollView;
import java.util.ArrayList;
import java.util.List;

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
            final List<Integer> capturedFrames = new ArrayList<>();
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
                // Record actual native scroll callbacks on the Android UI
                // thread. ActivityScenario.onActivity may be queued for
                // seconds on a loaded emulator and cannot sample 85ms
                // animation frames reliably using host-side sleeps.
                s.setOnScrollChangeListener((View v, int x, int y,
                        int oldX, int oldY) -> capturedFrames.add(y));
                activity.animateWallScrollForTest(destination[0]);
                assertEquals("The drawing must not jump synchronously to the wall",
                        start[0], s.getScrollY());
            });
            SystemClock.sleep(450L);
            scene.onActivity(activity -> {
                boolean hadIntermediateFrame = false;
                for (int y : capturedFrames) {
                    if (y > start[0] + 3 && y < destination[0] - 3) {
                        hadIntermediateFrame = true;
                        break;
                    }
                }
                assertTrue("Expected intermediate scroll callbacks, not a " +
                                "single jump: " + capturedFrames,
                        hadIntermediateFrame);
                assertEquals("The active wall must reach its final visible position",
                        destination[0], scroll[0].getScrollY(), 2);
                scroll[0].setOnScrollChangeListener(null);
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
