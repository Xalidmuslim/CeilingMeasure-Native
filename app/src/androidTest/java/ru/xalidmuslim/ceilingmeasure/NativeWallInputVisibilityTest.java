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
                                (location[1]+canvas.getHeight()) + " vs "+ expectedTop[0]
                                +" | "+ activity.wallViewportDebugForTest(expectedTop[0]),
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

    /**
     * Reproduction of user screenshot: wall 4 is above the clipped drawing
     * when the keyboard compresses the canvas. Temporary visual fitting must
     * place BOTH endpoints of the active wall inside the actual drawing View.
     * The user's persisted pan/zoom and all wall geometry must be unchanged.
     */
    @Test public void fourthWallRemainsFullyVisibleWhenNumberKeyboardShrinksCanvas()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION, START);
        try (ActivityScenario<NativeWorkspaceActivity> scene = ActivityScenario.launch(intent)) {
            scene.onActivity(activity -> {
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                addRectangularRoom(room);
                CeilingDrawingView drawing = activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertNotNull(drawing);
                drawing.setRoom(room);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            final float[] before = new float[3];
            final float[] roomBefore = new float[3];
            final int[] inputDialogTop = new int[1];
            scene.onActivity(activity -> {
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                CeilingDrawingView drawing = activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                drawing.fitView();
                drawing.zoomBy(2.4f);
                before[0] = drawing.getScaleForTest();
                before[1] = drawing.getPanXForTest();
                before[2] = drawing.getPanYForTest();
                roomBefore[0] = room.viewScale;
                roomBefore[1] = room.panX;
                roomBefore[2] = room.panY;

                // Wall 4 of 4 is the one most likely to be clipped above.
                NativeProjectStore.EdgeData fourth = room.edges.get(3);
                activity.onWallLongPressed(fourth.id, fourth.value);
                assertTrue(activity.isWallEditorShowingForTest());
                int[] top = new int[2];
                View scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                scroll.getLocationOnScreen(top);
                inputDialogTop[0] = top[1] +
                        Math.round(300f * activity.getResources().getDisplayMetrics().density);
                activity.adjustWallCanvasToDialogTopForTest(inputDialogTop[0]);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scene.onActivity(activity -> {
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                CeilingDrawingView drawing = activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertTrue("The selected fourth wall has disappeared beyond the " +
                                "top/bottom edge of the compact drawing",
                        drawing.isEditingWallFullyVisibleForTest());

                // The screenshot bug can also occur when the ScrollView itself
                // clips the top of an otherwise correctly fitted Canvas.
                // Assert actual device SCREEN coordinates against both the
                // ScrollView clip and the top of the numeric dialog/keyboard.
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                assertNotNull(scroll);
                int[] drawOnScreen = new int[2];
                int[] scrollOnScreen = new int[2];
                drawing.getLocationOnScreen(drawOnScreen);
                scroll.getLocationOnScreen(scrollOnScreen);
                float density = activity.getResources().getDisplayMetrics().density;
                float visibleTop = scrollOnScreen[1] + 8f * density;
                float visibleBottom = Math.min(inputDialogTop[0],
                        scrollOnScreen[1] + scroll.getHeight()) - 8f * density;
                NativeProjectStore.EdgeData edited = room.edges.get(3);
                for (String endpointId : new String[]{edited.a, edited.b}) {
                    NativeProjectStore.PointData point = null;
                    for (NativeProjectStore.PointData candidate : room.points) {
                        if (endpointId.equals(candidate.id)) {
                            point = candidate;
                            break;
                        }
                    }
                    assertNotNull(point);
                    float actualScreenY = drawOnScreen[1]
                            + point.y * drawing.getScaleForTest()
                            + drawing.getPanYForTest();
                    assertTrue("Edited wall is still hidden by ScrollView top: "
                                    + actualScreenY + " < " + visibleTop,
                            actualScreenY >= visibleTop);
                    assertTrue("Edited wall still overlaps dialog/IME: "
                                    + actualScreenY + " > " + visibleBottom
                                    + " | " + activity.wallViewportDebugForTest(inputDialogTop[0]),
                            actualScreenY <= visibleBottom);
                }
                assertTrue("View must use a temporary fitted wall-editor viewport",
                        drawing.isWallInputViewportActiveForTest());
                assertEquals("Temporary fitting must not rewrite stored zoom",
                        roomBefore[0], room.viewScale, .0001f);
                assertEquals("Temporary fitting must not rewrite stored pan X",
                        roomBefore[1], room.panX, .0001f);
                assertEquals("Temporary fitting must not rewrite stored pan Y",
                        roomBefore[2], room.panY, .0001f);
                activity.dismissWallInputForTest();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity -> {
                CeilingDrawingView drawing = activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertFalse(drawing.isWallInputViewportActiveForTest());
                assertEquals("Original zoom must return after keyboard closes",
                        before[0], drawing.getScaleForTest(), .0001f);
                assertEquals("Original panX must return after keyboard closes",
                        before[1], drawing.getPanXForTest(), .0001f);
                assertEquals("Original panY must return after keyboard closes",
                        before[2], drawing.getPanYForTest(), .0001f);
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
