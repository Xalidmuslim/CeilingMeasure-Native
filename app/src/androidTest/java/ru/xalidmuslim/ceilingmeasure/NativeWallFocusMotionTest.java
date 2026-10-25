package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

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
                assertTrue("Auto-focus must not teleport on the first frame",
                        scroll.getScrollY() < target[0] / 4);
            });
            SystemClock.sleep(165L);
            scene.onActivity(activity -> {
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                int y = scroll.getScrollY();
                assertTrue("Wall focus must actually glide during the transition: " + y,
                        y > 0 && y < target[0]);
            });
            SystemClock.sleep(550L);
            scene.onActivity(activity -> {
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
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
            });
            SystemClock.sleep(160L);
            scene.onActivity(activity ->
                    activity.animateWallFocusScrollToForTest(second[0]));
            SystemClock.sleep(620L);
            scene.onActivity(activity -> {
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                assertEquals("Old wall animation must not resume after retarget",
                        second[0], scroll.getScrollY());
            });
        }
    }
}
