package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
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
            scene.onActivity(activity -> {
                assertTrue(activity.isHomeMaterialsVisibleForTest());
                activity.dismissHomeMaterialsForTest();
                assertTrue("Closing Materials must finish its workspace host, never show drawing",
                        activity.isFinishing());
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
