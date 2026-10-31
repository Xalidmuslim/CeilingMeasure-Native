package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Mode must survive screen changes and must not edit customer measurement data. */
@RunWith(AndroidJUnit4.class)
public final class NativeTransformerModeTest {
    @Test public void switchingToOrdinaryAndBackPersists() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean previous = NativeTransformerMode.enabled(context);
        try {
            NativeTransformerMode.setEnabled(context, false);
            assertFalse(NativeTransformerMode.enabled(context));
            NativeTransformerMode.setEnabled(context, true);
            assertTrue(NativeTransformerMode.enabled(context));
        } finally {
            NativeTransformerMode.setEnabled(context, previous);
        }
    }
}
