package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Wall focus must glide to the edited segment, not jump in one frame. */
@RunWith(AndroidJUnit4.class)
public final class NativeWallFocusMotionTest {
    @Test public void wallFocusScrollMovesThroughIntermediateFramesAndReachesTarget()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = new Intent(context, NativeWorkspaceActivity.class);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(launch)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            final int[] target = {0};
            scene.onActivity(activity -> {
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                assertNotNull(scroll);
                assertNotNull(scroll.getChildAt(0));
                int extent = scroll.getChildAt(0).getHeight() - scroll.getHeight();
                assertTrue("Editor needs actual scroll content for this test", extent > 180);
                scroll.scrollTo(0,0);
                target[0] = Math.min(extent, 500);
                assertTrue("Wall auto-focus should be deliberately slower",
                        activity.getWallFocusScrollDurationForTest() >= 410L);
                activity.animateWallFocusScrollToForTest(target[0]);
                assertTrue("A real animation must run instead of a one-frame jump",
                        activity.isWallFocusScrollAnimatingForTest());
                assertTrue("Auto-focus must not teleport on the first frame",
                        scroll.getScrollY() < target[0] / 4);
            });
            // Run the emulator UI loop, then inspect the animator's actual
            // intermediate values recorded by frame callbacks. Android
            // instrumentation callbacks themselves can be delayed >460ms.
            SystemClock.sleep(850L);
            scene.onActivity(activity -> {
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                assertTrue("No interpolation frames: movement was an instant jump",
                        activity.getWallFocusIntermediateFramesForTest() > 0);
                assertEquals("Wall must land on exact intended position",
                        target[0], scroll.getScrollY());
            });
        }
    }

    @Test public void secondWallFocusSmoothlyRetargetsWithoutFirstScrollFinishing()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = new Intent(context, NativeWorkspaceActivity.class);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(launch)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            final int[] first = {0};
            final int[] second = {0};
            scene.onActivity(activity -> {
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                int extent = scroll.getChildAt(0).getHeight() - scroll.getHeight();
                assertTrue(extent > 240);
                scroll.scrollTo(0,0);
                first[0] = Math.min(extent, 560);
                second[0] = Math.min(extent, 205);
                activity.animateWallFocusScrollToForTest(first[0]);
                // Schedule the second wall selection on the same Android
                // frame loop while the first is still moving.
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> activity.animateWallFocusScrollToForTest(second[0]),
                        120L);
            });
            SystemClock.sleep(850L);
            scene.onActivity(activity -> {
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                assertEquals("Old wall animation must not resume after retarget",
                        second[0], scroll.getScrollY());
            });
        }
    }
}
