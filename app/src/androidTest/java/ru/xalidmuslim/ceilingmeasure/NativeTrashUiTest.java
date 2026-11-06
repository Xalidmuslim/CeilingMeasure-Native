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

/** User-visible recovery of a real saved order from the Trash filter. */
@RunWith(AndroidJUnit4.class)
public final class NativeTrashUiTest {
    @Test public void recoveryButtonBringsTrashedDrawingBackToSavedList() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        NativeProjectStore store=new NativeProjectStore(context);
        NativeProjectStore.ProjectData p=store.newProject();
        p.name="Проверка корзины";
        p.rooms.get(0).notes="Данные не терять";
        assertTrue(store.saveSnapshot(p));
        assertTrue(store.deleteSaved(p.id));

        Intent start=new Intent(context,NativeWorkspaceActivity.class);
        start.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_SAVED_MEASUREMENTS);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(start)) {
            scenario.onActivity(activity->{
                View root=activity.getWindow().getDecorView();
                View trash=root.findViewWithTag("native_saved_trash");
                assertNotNull("Trash tab must exist",trash);
                trash.performClick();
                View deleted=root.findViewWithTag("native_trash_item_"+p.id);
                assertNotNull("Deleted order must appear in trash",deleted);
                View recover=root.findViewWithTag("native_trash_restore_"+p.id);
                assertNotNull(recover);
                recover.performClick();
                assertNotNull("Restored order must appear in saved list",
                        activity.getWindow().getDecorView()
                                .findViewWithTag("native_saved_item_"+p.id));
            });
        }
        NativeProjectStore.ProjectData restored=new NativeProjectStore(context)
                .loadSaved().stream().filter(x->p.id.equals(x.id)).findFirst().orElse(null);
        assertNotNull(restored);
        assertEquals("Данные не терять",restored.rooms.get(0).notes);
    }
}
