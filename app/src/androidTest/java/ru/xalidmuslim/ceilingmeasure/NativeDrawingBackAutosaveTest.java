package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Unfinished room geometry must survive every way out of the drawing editor. */
@RunWith(AndroidJUnit4.class)
public final class NativeDrawingBackAutosaveTest {
    private static final String NOTE = "Сохранить заметку без потери фокуса";

    private Intent newOrderIntent(Context context) {
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        return intent;
    }

    private String addUnfinishedDrawing(NativeWorkspaceActivity activity) {
        NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
        room.points.add(new NativeProjectStore.PointData("autosave-A", 10f, 20f));
        room.points.add(new NativeProjectStore.PointData("autosave-B", 210f, 20f));
        room.edges.add(new NativeProjectStore.EdgeData(
                "autosave-edge", "autosave-A", "autosave-B", 200f));
        EditText notes = activity.getWindow().getDecorView()
                .findViewWithTag("native_room_notes");
        assertNotNull(notes);
        notes.setText(NOTE);
        activity.onChanged();
        return new NativeProjectStore(activity).loadCurrent().id;
    }

    private NativeProjectStore.ProjectData savedProject(Context context, String id) {
        List<NativeProjectStore.ProjectData> saved = new NativeProjectStore(context).loadSaved();
        for (NativeProjectStore.ProjectData project : saved) {
            if (id.equals(project.id)) return project;
        }
        return null;
    }

    private void assertDrawingRestored(NativeProjectStore.ProjectData project) {
        assertNotNull("Unfinished drawing must appear in Saved Measurements", project);
        assertFalse(project.rooms.isEmpty());
        NativeProjectStore.RoomData room = project.rooms.get(0);
        assertEquals(2, room.points.size());
        assertEquals(1, room.edges.size());
        assertEquals("autosave-edge", room.edges.get(0).id);
        assertEquals(200f, room.edges.get(0).value, .001f);
        assertEquals(NOTE, room.notes);
        assertTrue("Project must be marked saved", project.savedAt > 0L);
    }

    @Test
    public void systemBackImmediatelySavesUnfinishedDrawingAndUnfocusedNotes() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicReference<String> id = new AtomicReference<>();
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent(context))) {
            scenario.onActivity(activity -> {
                id.set(addUnfinishedDrawing(activity));
                activity.getOnBackPressedDispatcher().onBackPressed();
            });
        }
        assertDrawingRestored(savedProject(context, id.get()));
    }

    @Test
    public void savedNavigationShowsFreshUnfinishedDrawingWithoutWaitingForDebounce() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicReference<String> id = new AtomicReference<>();
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent(context))) {
            scenario.onActivity(activity -> {
                id.set(addUnfinishedDrawing(activity));
                View saved = activity.getWindow().getDecorView()
                        .findViewWithTag("native_top_saved");
                assertNotNull(saved);
                saved.performClick();
                assertNotNull("Fresh drawing must already be present in saved list",
                        activity.getWindow().getDecorView()
                                .findViewWithTag("native_saved_item_" + id.get()));
            });
        }
        assertDrawingRestored(savedProject(context, id.get()));
    }

    @Test
    public void backgroundingEditorAlsoPreservesUnfinishedDrawing() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicReference<String> id = new AtomicReference<>();
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent(context))) {
            scenario.onActivity(activity -> id.set(addUnfinishedDrawing(activity)));
            scenario.moveToState(Lifecycle.State.CREATED);
            assertDrawingRestored(savedProject(context, id.get()));
        }
    }
}
