package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Exact project identity and no intermediate Materials animation regressions. */
@RunWith(AndroidJUnit4.class)
public final class NativeHomeDirectNavigationTest {
    @Test
    public void recentProjectOpensRequestedSavedDrawingNotTheGeneralList() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NativeProjectStore store = new NativeProjectStore(context);
        NativeProjectStore.ProjectData first = store.newProject();
        first.name = "Тест первый замер";
        store.saveSnapshot(first);
        NativeProjectStore.ProjectData selected = store.newProject();
        selected.name = "Тест нужный замер";
        store.saveSnapshot(selected);
        try {
            Intent launch = new Intent(context, NativeWorkspaceActivity.class);
            launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                    NativeWorkspaceActivity.ACTION_RECENT_PROJECT);
            launch.putExtra(NativeWorkspaceActivity.EXTRA_PROJECT_ID, first.id);
            try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(launch)) {
                scene.onActivity(activity -> {
                    assertEquals("Tap must open the selected saved ID, not the newest order",
                            first.id, activity.currentProjectIdForTest());
                    assertNotNull("Exact saved project goes directly to drawing",
                            activity.getWindow().getDecorView()
                                    .findViewWithTag("native_drawing_view"));
                    assertNull("General saved list must not be displayed",
                            activity.getWindow().getDecorView()
                                    .findViewWithTag("native_saved_page"));
                });
            }
        } finally {
            store.deleteSaved(first.id);
            store.deleteSaved(selected.id);
        }
    }

    @Test
    public void missingSavedIdFallsBackToSavedListWithoutPickingAnotherProject() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = new Intent(context, NativeWorkspaceActivity.class);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_RECENT_PROJECT);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_PROJECT_ID, "nonexistent-saved-id-244");
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(launch)) {
            scene.onActivity(activity -> {
                assertNotNull("Deleted projects must lead to the list, not a different drawing",
                        activity.getWindow().getDecorView()
                                .findViewWithTag("native_saved_page"));
                assertNull(activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view"));
            });
        }
    }

    @Test
    public void closingMaterialsReturnsToExistingHomeRatherThanRevealingDrawing() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = new Intent(context, NativeWorkspaceActivity.class);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_MATERIALS);
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(launch)) {
            final NativeWorkspaceActivity[] host = {null};
            scene.onActivity(activity -> {
                host[0] = activity;
                assertTrue(activity.isHomeMaterialsVisibleForTest());
                activity.dismissHomeMaterialsForTest();
            });
            // Dialog dismissal is asynchronous. The correct condition is
            // finish() requested; Android can leave the Activity in STARTED
            // until its next lifecycle dispatch.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertNotNull(host[0]);
            assertTrue("Closing Materials must finish its hidden editor host",
                    host[0].isFinishing());
        }
    }

    @Test
    public void androidBackFromHomeMaterialsRestoresOriginalHomeInstanceAndScrollPosition() {
        try (ActivityScenario<NativeHomeActivity> homeScenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            final NativeHomeActivity[] original = {null};
            final int[] originalScroll = {0};
            homeScenario.onActivity(home -> {
                original[0] = home;
                ScrollView scroll = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_scroll");
                assertNotNull(scroll);
                originalScroll[0] = scroll.getScrollY();
                ViewGroup dock = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_bottom_nav");
                assertNotNull(dock);
                ViewGroup actions = (ViewGroup) dock.getChildAt(0);
                assertEquals("Материалы",
                        String.valueOf(actions.getChildAt(3).getContentDescription()));
                actions.getChildAt(3).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation()
                    .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            homeScenario.onActivity(home -> {
                assertSame("Back must reveal the original Home Activity", original[0], home);
                ScrollView scroll = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_scroll");
                assertNotNull(scroll);
                assertEquals("Home scroll position must be preserved",
                        originalScroll[0], scroll.getScrollY());
                assertNotNull(home.getWindow().getDecorView()
                        .findViewWithTag("native_home_recent"));
                assertNull("Drawing must not replace the Home screen",
                        home.getWindow().getDecorView()
                                .findViewWithTag("native_drawing_view"));
            });
        }
    }

    @Test
    public void homeMaterialsOpenDirectlyWithoutStartingDrawingAndBackPreservesHome() {
        try (ActivityScenario<NativeHomeActivity> scene =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            final NativeHomeActivity[] initial = new NativeHomeActivity[1];
            final int[] scrollBefore = new int[1];
            scene.onActivity(home -> {
                initial[0] = home;
                ScrollView scroll = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_scroll");
                assertNotNull(scroll);
                scrollBefore[0] = scroll.getScrollY();
                ViewGroup dock = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_bottom_nav");
                ViewGroup options = (ViewGroup) dock.getChildAt(0);
                options.getChildAt(3).performClick();
                assertTrue("Catalog must be hosted by Home itself, not hidden editor",
                        home.isHomeMaterialsVisibleForTest());
                assertNull("Never construct a drawing editor behind the catalog",
                        home.getWindow().getDecorView()
                                .findViewWithTag("native_drawing_view"));
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation()
                    .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(home -> {
                assertSame("Return must preserve the existing Home Activity", initial[0], home);
                assertFalse("Catalog must close after system Back",
                        home.isHomeMaterialsVisibleForTest());
                ScrollView scroll = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_scroll");
                assertEquals("Return must preserve Home scroll position",
                        scrollBefore[0], scroll.getScrollY());
                assertNotNull(home.getWindow().getDecorView()
                        .findViewWithTag("native_home_recent"));
            });
        }
    }

    @Test
    public void materialsDialogOpensImmediatelyWithoutEditorFadeFlash() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = new Intent(context, NativeWorkspaceActivity.class);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_MATERIALS);
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(launch)) {
            scene.onActivity(activity -> {
                assertTrue("Materials window should be presented before first Activity frame",
                        activity.isHomeMaterialsVisibleForTest());
                assertEquals("Do not fade the underlying drawing editor into the catalog",
                        0, activity.homeMaterialsWindowAnimationForTest());
            });
        }
    }
}
