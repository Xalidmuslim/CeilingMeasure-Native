package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** The selector must be reachable in the actual settings dialog, not only in prefs. */
@RunWith(AndroidJUnit4.class)
public final class NativeUiMotionSettingsTest {
    @Test public void dialogSwitchesBetweenBothModesAndCloses() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean previous = NativeTransformerMode.enabled(context);
        CountDownLatch closed = new CountDownLatch(1);
        try {
            try (ActivityScenario<NativeHomeActivity> scene =
                         ActivityScenario.launch(NativeHomeActivity.class)) {
                scene.onActivity(activity -> {
                    NativeTransformerMode.setEnabled(activity, true);
                    NativeUiSettingsDialog dialog = new NativeUiSettingsDialog(
                            activity, NativeUiPrefs.load(activity), next -> {});
                    dialog.setStableHomeTransition(true);
                    dialog.setOnDismissListener(d -> closed.countDown());
                    dialog.show();
                    View root = dialog.getWindow().getDecorView();
                    View ordinary = root.findViewWithTag("native_ui_motion_ordinary");
                    View transformer = root.findViewWithTag("native_ui_motion_transformer");
                    assertNotNull("Ordinary mode must be visible", ordinary);
                    assertNotNull("Transformer mode must be visible", transformer);
                    ordinary.performClick();
                    assertFalse(dialog.isTransformerModeForTest());
                    transformer.performClick();
                    assertTrue(dialog.isTransformerModeForTest());
                    dialog.dismiss();
                });
                assertTrue("Settings must dismiss after its reverse choreography",
                        closed.await(4, TimeUnit.SECONDS));
            }
        } finally {
            NativeTransformerMode.setEnabled(context, previous);
        }
    }
}
