package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

/** Regression: taps navigate synchronously; full-screen sheets do not scatter. */
@RunWith(AndroidJUnit4.class)
public final class NativeFastNavigationAndOrdinaryDialogsTest {
    @Test public void bottomNavCallbackRunsBeforeClickReturnsWithoutDoubleRouting() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assumeTrue(NativeMotion.animationsEnabled());
        boolean previous = NativeTransformerMode.enabled(context);
        NativeTransformerMode.setEnabled(context, true);
        try {
            AtomicInteger routes = new AtomicInteger();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                LinearLayout button = new LinearLayout(context);
                ImageView icon = new ImageView(context);
                button.addView(icon);
                NativeTransformerMotion.bindNavigation(button, icon, 1, routes::incrementAndGet);
                assertTrue(button.performClick());
                assertEquals("Routing must not wait for the icon animation", 1, routes.get());
                button.performClick();
                assertEquals("Rapid second tap must not duplicate the route", 1, routes.get());
            });
        } finally {
            NativeTransformerMode.setEnabled(context, previous);
        }
    }

    @Test public void estimateAndMaterialsShellKeepsEveryElementAtRest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assumeTrue(NativeMotion.animationsEnabled());
        boolean previous = NativeTransformerMode.enabled(context);
        NativeTransformerMode.setEnabled(context, true);
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            scenario.onActivity(activity -> {
                Dialog dialog = new Dialog(activity);
                ScrollView body = new ScrollView(activity);
                LinearLayout rows = new LinearLayout(activity);
                rows.setOrientation(LinearLayout.VERTICAL);
                for (int i = 0; i < 3; i++) {
                    TextView row = new TextView(activity);
                    row.setText("Смета / материалы " + i);
                    rows.addView(row);
                }
                body.addView(rows);
                NativeFullScreenDialogShell.create(dialog, body);
                assertEquals("Ordinary fade belongs to the dialog window", 1f, body.getAlpha(), .001f);
                assertEquals(0f, body.getTranslationX(), .001f);
                assertEquals(0f, body.getTranslationY(), .001f);
                assertEquals(1f, body.getScaleX(), .001f);
                for (int i = 0; i < rows.getChildCount(); i++) {
                    View row = rows.getChildAt(i);
                    assertEquals("No tree-like scatter inside full-screen sheets",
                            1f, row.getAlpha(), .001f);
                    assertEquals(0f, row.getTranslationX(), .001f);
                    assertEquals(0f, row.getRotationY(), .001f);
                }
                dialog.dismiss();
            });
        } finally {
            NativeTransformerMode.setEnabled(context, previous);
        }
    }
}
