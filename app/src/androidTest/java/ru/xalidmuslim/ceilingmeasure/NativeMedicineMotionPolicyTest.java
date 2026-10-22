package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.view.View;
import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Architectural parity with the golden Medicine Smooth Motion profile. */
@RunWith(AndroidJUnit4.class)
public final class NativeMedicineMotionPolicyTest {
    @Test
    public void motionTimingMatchesMedicineProfileWithoutDisplacementOrLayers() {
        assertEquals(215L, NativeMotion.ENTER_MS);
        assertEquals(115L, NativeMotion.COMPACT_ENTER_MS);
        assertEquals(110L, NativeMotion.MICRO_ENTER_MS);
        assertEquals(215L, NativeMotion.DIALOG_ENTER_MS);
        assertEquals(165L, NativeMotion.DIALOG_EXIT_MS);

        try (ActivityScenario<NativeHomeActivity> home =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            home.onActivity(activity -> {
                View root = activity.getWindow().getDecorView()
                        .findViewWithTag("native_home_root");
                assertNotNull(root);
                assertEquals(0f, root.getTranslationX(), .001f);
                assertEquals(0f, root.getTranslationY(), .001f);
                assertEquals(1f, root.getScaleX(), .001f);
                assertEquals(1f, root.getScaleY(), .001f);
                assertEquals(View.LAYER_TYPE_NONE, root.getLayerType());
            });
        }
    }

    @Test
    public void respectsSystemReducedMotionWhenAnimatorsDisabled() {
        if (NativeMotion.animationsEnabled()) return; // Run also with animator_duration_scale=0.
        try (ActivityScenario<NativeHomeActivity> home =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            home.onActivity(activity -> {
                View root = activity.getWindow().getDecorView()
                        .findViewWithTag("native_home_root");
                assertNotNull(root);
                assertEquals("Disabled system animations must not leave root faded",
                        1f, root.getAlpha(), .001f);
            });
        }
    }
}
