package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Two navigation layouts share actions and motion; selection survives navigation. */
public final class NativeBottomNavModeTest {
    @Test public void existingUsersKeepContextualBottomButtonsByDefault() {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        assertFalse(NativeBottomNavMode.isFixed(prefs));
    }

    @Test public void choicePersistsAndCanBeSwitchedBack() {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeBottomNavMode.setFixed(prefs, true);
        assertTrue(NativeBottomNavMode.isFixed(prefs));
        NativeBottomNavMode.setFixed(prefs, false);
        assertFalse(NativeBottomNavMode.isFixed(prefs));
    }
}
