package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class NativeProjectOverviewUiTest {
    @Test public void newWorkspaceShowsRoomAndTotalsWithoutOpeningClientEstimate() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try (ActivityScenario<NativeWorkspaceActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                View root = activity.getWindow().getDecorView();
                assertNotNull(root.findViewWithTag("native_project_overview"));
                TextView area = root.findViewWithTag("native_project_total_area");
                TextView perimeter = root.findViewWithTag("native_project_total_perimeter");
                android.widget.LinearLayout rows = root.findViewWithTag("native_project_room_metrics");
                TextView status = root.findViewWithTag("native_project_summary_status");
                assertNotNull(area);
                assertNotNull(perimeter);
                assertNotNull(rows);
                assertNotNull(status);
                assertEquals("—", area.getText().toString());
                assertEquals("—", perimeter.getText().toString());
                assertEquals(1, rows.getChildCount());
                android.widget.LinearLayout row =
                        (android.widget.LinearLayout) rows.getChildAt(0);
                assertEquals(3, row.getChildCount());
                assertEquals("Комната 1", ((TextView) row.getChildAt(0)).getText().toString());
                assertEquals("—", ((TextView) row.getChildAt(1)).getText().toString());
                assertEquals("—", ((TextView) row.getChildAt(2)).getText().toString());
                assertTrue(status.getText().toString().contains("1"));
            });
        }
    }

    @Test public void transformerPreferenceDoesNotTransformSettingsSheet() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean previous = NativeTransformerMode.enabled(context);
        try {
            try (ActivityScenario<NativeHomeActivity> scenario =
                         ActivityScenario.launch(NativeHomeActivity.class)) {
                scenario.onActivity(activity -> {
                    NativeTransformerMode.setEnabled(activity, true);
                    NativeUiSettingsDialog dialog = new NativeUiSettingsDialog(
                            activity, NativeUiPrefs.load(activity), next -> {});
                    dialog.show();
                    assertEquals(R.style.CeilingNoWindowAnimation,
                            dialog.getWindow().getAttributes().windowAnimations);
                    View sheet = dialog.getWindow().getDecorView()
                            .findViewWithTag("native_ui_settings_sheet");
                    assertNotNull(sheet);
                    assertEquals(0f, sheet.getTranslationX(), .001f);
                    assertEquals(0f, sheet.getTranslationY(), .001f);
                    assertEquals(0f, sheet.getRotationY(), .001f);
                    assertEquals(1f, sheet.getScaleX(), .001f);
                    dialog.dismiss();
                });
            }
        } finally {
            NativeTransformerMode.setEnabled(context, previous);
        }
    }
}
