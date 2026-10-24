package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Regression: editing a wall must show a blinking edge and keep its canvas above IME. */
@RunWith(AndroidJUnit4.class)
public final class NativeWallInputVisibilityTest {
    private static final String START = NativeWorkspaceActivity.ACTION_NEW_ORDER;

    @Test public void selectedWallBlinksClearlyAndStopsImmediatelyWhenEditorCloses()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION, START);
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(intent)) {
            final CeilingDrawingView[] drawing = {null};
            final String[] selectedId = {null};
            final boolean[] firstPhase = {false};
            scene.onActivity(activity -> {
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                addRectangularRoom(room);
                drawing[0] = activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertNotNull(drawing[0]);
                drawing[0].setRoom(room);
                selectedId[0] = room.edges.get(0).id;
                drawing[0].setEditingEdgeId(selectedId[0]);
                assertEquals(selectedId[0], drawing[0].getEditingEdgeIdForTest());
                assertTrue("selected wall must have visible blinking while measuring",
                        drawing[0].isWallPulseActiveForTest());
                firstPhase[0] = drawing[0].isWallPulseBrightForTest();
            });
            SystemClock.sleep(480L);
            scene.onActivity(activity -> {
                assertTrue("Blink changes phase without recomputing measurements",
                        firstPhase[0] != drawing[0].isWallPulseBrightForTest());
                drawing[0].setEditingEdgeId(null);
                assertFalse("Pulse must stop on editor close", drawing[0].isWallPulseActiveForTest());
                assertNull(drawing[0].getEditingEdgeIdForTest());
            });
        }
    }

    @Test public void wallEditorMovesEntireCanvasAboveDialogAndRestoresHeight()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION, START);
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(intent)) {
            final int[] expectedTop = {0};
            final int[] normalHeight = {0};
            final int[][] originalPoints = {new int[4]};
            scene.onActivity(activity -> {
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                addRectangularRoom(room);
                View drawing = activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                View canvas = activity.getWindow().getDecorView()
                        .findViewWithTag("native_canvas_wrap");
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                assertNotNull(drawing);
                assertNotNull(canvas);
                assertNotNull(scroll);
                ((CeilingDrawingView) drawing).setRoom(room);
                normalHeight[0] = canvas.getLayoutParams().height;
                assertTrue(normalHeight[0] > 0);
                originalPoints[0][0] = (int)room.points.get(0).x;
                originalPoints[0][1] = (int)room.points.get(0).y;
                originalPoints[0][2] = (int)room.points.get(2).x;
                originalPoints[0][3] = (int)room.points.get(2).y;

                activity.onWallLongPressed(room.edges.get(0).id, 300f);
                assertTrue(activity.isWallEditorShowingForTest());
                // Simulates dialog relocated immediately above an Android IME.
                int[] screen = new int[2];
                scroll.getLocationOnScreen(screen);
                expectedTop[0] = screen[1] + Math.round(
                        340f * activity.getResources().getDisplayMetrics().density);
                activity.adjustWallCanvasToDialogTopForTest(expectedTop[0]);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity -> {
                View canvas = activity.getWindow().getDecorView()
                        .findViewWithTag("native_canvas_wrap");
                assertNotNull(canvas);
                assertTrue("Canvas becomes compact in the available IME space",
                        canvas.getHeight() < normalHeight[0]);
                int[] location = new int[2];
                canvas.getLocationOnScreen(location);
                assertTrue("Wall drawing bottom must stay above number input window: " +
                                (location[1]+canvas.getHeight()) + " vs "+ expectedTop[0],
                        location[1] + canvas.getHeight() <= expectedTop[0] - 2);
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                assertEquals(originalPoints[0][0], (int)room.points.get(0).x);
                assertEquals(originalPoints[0][1], (int)room.points.get(0).y);
                assertEquals(originalPoints[0][2], (int)room.points.get(2).x);
                assertEquals(originalPoints[0][3], (int)room.points.get(2).y);
                activity.dismissWallInputForTest();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity -> {
                View canvas = activity.getWindow().getDecorView()
                        .findViewWithTag("native_canvas_wrap");
                assertEquals("Original canvas height must return after input",
                        normalHeight[0], canvas.getLayoutParams().height);
            });
        }
    }

    private static void addRectangularRoom(NativeProjectStore.RoomData room) {
        room.points.clear();
        room.edges.clear();
        room.points.add(new NativeProjectStore.PointData("v249_a",90f,90f));
        room.points.add(new NativeProjectStore.PointData("v249_b",390f,90f));
        room.points.add(new NativeProjectStore.PointData("v249_c",390f,365f));
        room.points.add(new NativeProjectStore.PointData("v249_d",90f,365f));
        room.edges.add(new NativeProjectStore.EdgeData("v249_a","v249_b"));
        room.edges.add(new NativeProjectStore.EdgeData("v249_b","v249_c"));
        room.edges.add(new NativeProjectStore.EdgeData("v249_c","v249_d"));
        room.edges.add(new NativeProjectStore.EdgeData("v249_d","v249_a"));
        room.edges.get(0).value=300f;
        room.edges.get(1).value=275f;
        room.edges.get(2).value=300f;
        room.edges.get(3).value=275f;
    }
}
