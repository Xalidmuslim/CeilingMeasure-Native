package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Fixed dock must not discard drawing data or the contextual editor actions. */
@RunWith(AndroidJUnit4.class)
public final class NativeFixedNavigationUiTest {
    @Test public void fixedDockHasIdenticalDestinationsInDrawingAndSaved() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean before = NativeBottomNavMode.isFixed(context);
        try {
            NativeBottomNavMode.setFixed(context, true);
            Intent intent = new Intent(context, NativeWorkspaceActivity.class);
            intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                    NativeWorkspaceActivity.ACTION_NEW_ORDER);
            try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(intent)) {
                scene.onActivity(activity -> {
                    View root = activity.getWindow().getDecorView();
                    assertFixedDestinations(root);
                    View tools = root.findViewWithTag("native_fixed_editor_tools");
                    assertNotNull(tools);
                    assertEquals(View.VISIBLE, tools.getVisibility());
                    assertNotNull(root.findViewWithTag("native_fixed_tool_undo"));
                    assertNotNull(root.findViewWithTag("native_fixed_tool_redo"));
                    assertNotNull(root.findViewWithTag("native_fixed_tool_more"));
                    root.findViewWithTag("native_nav_fixed_saved").performClick();
                    assertFixedDestinations(activity.getWindow().getDecorView());
                });
            }
        } finally {
            NativeBottomNavMode.setFixed(context, before);
        }
    }

    @Test public void settingChangesDockLiveAndKeepsSameDrawingView() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean before = NativeBottomNavMode.isFixed(context);
        try {
            NativeBottomNavMode.setFixed(context, false);
            Intent intent = new Intent(context, NativeWorkspaceActivity.class);
            intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                    NativeWorkspaceActivity.ACTION_NEW_ORDER);
            try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(intent)) {
                scene.onActivity(activity -> {
                    View root = activity.getWindow().getDecorView();
                    View drawing = root.findViewWithTag("native_drawing_view");
                    assertNotNull(drawing);
                    assertNotNull(root.findViewWithTag("native_nav_undo"));
                    root.findViewWithTag("native_top_settings").performClick();

                    NativeUiSettingsDialog dialog = activity.getSettingsDialogForTest();
                    assertNotNull(dialog);
                    assertTrue(dialog.isShowing());
                    View fixed = dialog.getWindow().getDecorView()
                            .findViewWithTag("native_ui_nav_fixed");
                    View contextual = dialog.getWindow().getDecorView()
                            .findViewWithTag("native_ui_nav_contextual");
                    assertNotNull(fixed);
                    assertNotNull(contextual);
                    fixed.performClick();
                    assertTrue(dialog.isFixedNavigationForTest());
                    assertFixedDestinations(root);
                    assertSame(drawing, root.findViewWithTag("native_drawing_view"));
                    contextual.performClick();
                    assertTrue(!dialog.isFixedNavigationForTest());
                    assertNotNull(root.findViewWithTag("native_nav_undo"));
                    assertEquals(View.GONE, root.findViewWithTag(
                            "native_fixed_editor_tools").getVisibility());
                    assertSame(drawing, root.findViewWithTag("native_drawing_view"));
                    dialog.dismiss();
                });
            }
        } finally {
            NativeBottomNavMode.setFixed(context, before);
        }
    }

    private static void assertFixedDestinations(View root) {
        assertNotNull(root.findViewWithTag("native_nav_fixed_home"));
        assertNotNull(root.findViewWithTag("native_nav_fixed_saved"));
        assertNotNull(root.findViewWithTag("native_nav_fixed_new"));
        assertNotNull(root.findViewWithTag("native_nav_fixed_materials"));
        assertNotNull(root.findViewWithTag("native_nav_fixed_settings"));
    }
}
