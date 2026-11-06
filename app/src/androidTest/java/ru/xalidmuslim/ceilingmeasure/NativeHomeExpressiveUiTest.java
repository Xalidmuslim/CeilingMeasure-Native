package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.app.Instrumentation;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Central plus is a direct measurement action; no legacy selection overlay. */
@RunWith(AndroidJUnit4.class)
public final class NativeHomeExpressiveUiTest {
    @Test public void plusOpensMeasurementDirectlyWithoutLabelOrSectionPicker() {
        boolean previous = NativeTransformerMode.enabled(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        NativeTransformerMode.setEnabled(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),true);
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(
                NativeWorkspaceActivity.class.getName(),null,false);
        try {
            try(ActivityScenario<NativeHomeActivity> scenario=
                        ActivityScenario.launch(NativeHomeActivity.class)) {
                scenario.onActivity(activity->{
                    View root=activity.findViewById(android.R.id.content);
                    View plus=root.findViewWithTag("native_home_nav_new");
                    assertNotNull(plus);
                    assertNull("Plus must not offer three route choices",
                            root.findViewWithTag("native_home_speed_dial"));
                    assertEquals("No 'Новый' caption below the plus",1,
                            ((ViewGroup)plus.getParent()).getChildCount());
                    plus.performClick();
                });
                Activity opened=instrumentation.waitForMonitorWithTimeout(monitor,8000);
                assertNotNull("Plus must navigate straight into the editor",opened);
                assertEquals(NativeWorkspaceActivity.class,opened.getClass());
            }
        } finally {
            instrumentation.removeMonitor(monitor);
            NativeTransformerMode.setEnabled(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(),previous);
        }
    }
}
