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
            SystemClock.sleep(440L);
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
                                    + actualScreenY + " > " + visibleBottom,
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
                if (NativeMotion.animationsEnabled()) {
                    assertTrue("Camera must not snap back on dialog dismiss",
                            drawing.isWallInputViewportRestorePendingForTest());
                }
            });
            SystemClock.sleep(510L);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity -> {
                CeilingDrawingView drawing = activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertFalse("Camera return animation must finish",
                        activity.isWallReturnAnimationRunningForTest());
                assertFalse(drawing.isWallInputViewportRestorePendingForTest());
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

    @Test public void changingEditedWallMovesDrawingUpOrDownToThatWallAboveKeypad()
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
                drawing.setRoom(room);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            final int[] keypadTop = new int[1];
            final float[] upperWallScreenY = new float[1];
            final float[] lowerWallScreenY = new float[1];
            final float[] storedRoomState = new float[3];
            scene.onActivity(activity -> {
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                ScrollView scroll = activity.getWindow().getDecorView()
                        .findViewWithTag("native_editor_scroll");
                int[] pos = new int[2];
                scroll.getLocationOnScreen(pos);
                keypadTop[0] = pos[1] + Math.round(
                        330f * activity.getResources().getDisplayMetrics().density);
                storedRoomState[0] = room.viewScale;
                storedRoomState[1] = room.panX;
                storedRoomState[2] = room.panY;
                activity.onWallLongPressed(room.edges.get(0).id, 300f);
                activity.adjustWallCanvasToDialogTopForTest(keypadTop[0]);
            });
            Thread.sleep(320L);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity -> {
                CeilingDrawingView drawing = activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertTrue("Upper selected wall must be visible above keypad",
                        activity.isSelectedWallVisibleAboveKeypadForTest(keypadTop[0]));
                upperWallScreenY[0] = activity.activeWallCenterScreenYForTest();
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                // Select the low horizontal wall. View should now follow it
                // instead of leaving the user looking at the former wall.
                activity.onWallLongPressed(room.edges.get(2).id, 300f);
                activity.adjustWallCanvasToDialogTopForTest(keypadTop[0]);
            });
            Thread.sleep(320L);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity -> {
                assertTrue("Lower wall must come back into view without manually scrolling",
                        activity.isSelectedWallVisibleAboveKeypadForTest(keypadTop[0]));
                lowerWallScreenY[0] = activity.activeWallCenterScreenYForTest();
                CeilingDrawingView drawing = activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertTrue("Wall input must keep a temporary editing camera",
                        drawing.isWallInputViewportActiveForTest());
                assertTrue("Focus must remain in stable visible region",
                        Math.abs(upperWallScreenY[0] - lowerWallScreenY[0])
                                < 85f * activity.getResources().getDisplayMetrics().density);
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();
                assertEquals(storedRoomState[0], room.viewScale, 0.0001f);
                assertEquals(storedRoomState[1], room.panX, 0.0001f);
                assertEquals(storedRoomState[2], room.panY, 0.0001f);
                activity.dismissWallInputForTest();
            });
        }
    }

    @Test public void keypadEditingZoomsOutWholeContourWithoutSavingCamera()
            throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,START);
        try(ActivityScenario<NativeWorkspaceActivity> scene=ActivityScenario.launch(intent)){
            scene.onActivity(activity->{
                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                addRectangularRoom(room);
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                drawing.setRoom(room);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity->{
                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertTrue(drawing.getWidth()>0 && drawing.getHeight()>0);
                float roomScale=room.viewScale;
                float roomPanX=room.panX;
                float roomPanY=room.panY;
                drawing.setEditingEdgeId(room.edges.get(0).id);
                drawing.beginWallInputViewport();
                float initialFit=drawing.getScaleForTest();
                drawing.focusEditingWallInVisibleSlice(0f,drawing.getHeight());
                assertTrue("Entering measurements must zoom OUT noticeably from initial fit",
                        drawing.getScaleForTest()<=initialFit*.89f);
                assertTrue("Selected wall remains visible",drawing.isEditingWallFullyVisibleForTest());
                assertEquals(roomScale,room.viewScale,.0001f);
                assertEquals(roomPanX,room.panX,.0001f);
                assertEquals(roomPanY,room.panY,.0001f);
                drawing.endWallInputViewport();
                drawing.setEditingEdgeId(null);
                assertEquals(roomScale,room.viewScale,.0001f);
                assertEquals(roomPanX,room.panX,.0001f);
                assertEquals(roomPanY,room.panY,.0001f);
            });
        }
    }

    @Test public void returnFromWallNumbersInterpolatesViewportBeforeRestoringSavedCamera()
            throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,START);
        try(ActivityScenario<NativeWorkspaceActivity> scene=ActivityScenario.launch(intent)){
            scene.onActivity(activity->{
                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                addRectangularRoom(room);
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                drawing.setRoom(room);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity->{
                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                drawing.fitView();
                drawing.zoomBy(1.8f);
                float savedScale=drawing.getScaleForTest();
                float savedPanX=drawing.getPanXForTest();
                float savedPanY=drawing.getPanYForTest();
                float persistentScale=room.viewScale;
                float persistentPanX=room.panX;
                float persistentPanY=room.panY;

                drawing.setEditingEdgeId(room.edges.get(0).id);
                drawing.beginWallInputViewport();
                drawing.focusEditingWallInVisibleSlice(0f,drawing.getHeight());
                float editingScale=drawing.getScaleForTest();
                float editingPanX=drawing.getPanXForTest();
                float editingPanY=drawing.getPanYForTest();
                assertTrue("The wall editor must use a different temporary camera",
                        Math.abs(savedScale-editingScale)>.05f);

                drawing.beginWallInputViewportRestore();
                assertTrue("Restore must stay pending during smooth transition",
                        drawing.isWallInputViewportRestorePendingForTest());
                // Do not snap at t=0.
                assertEquals(editingScale,drawing.getScaleForTest(),.0001f);
                drawing.applyWallInputViewportRestoreFraction(.5f);
                assertEquals((editingScale+savedScale)/2f,
                        drawing.getScaleForTest(),.0001f);
                assertEquals((editingPanX+savedPanX)/2f,
                        drawing.getPanXForTest(),.0001f);
                assertEquals((editingPanY+savedPanY)/2f,
                        drawing.getPanYForTest(),.0001f);
                assertEquals("Preview animation must not persist zoom",
                        persistentScale,room.viewScale,.0001f);
                assertEquals(persistentPanX,room.panX,.0001f);
                assertEquals(persistentPanY,room.panY,.0001f);

                drawing.finishWallInputViewportRestore();
                drawing.setEditingEdgeId(null);
                assertFalse(drawing.isWallInputViewportRestorePendingForTest());
                assertEquals(savedScale,drawing.getScaleForTest(),.0001f);
                assertEquals(savedPanX,drawing.getPanXForTest(),.0001f);
                assertEquals(savedPanY,drawing.getPanYForTest(),.0001f);
            });
        }
    }

    @Test public void unfinishedContourNeverOpensWallNumberEditor()
            throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,START);
        try(ActivityScenario<NativeWorkspaceActivity> scene=ActivityScenario.launch(intent)){
            scene.onActivity(activity->{
                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                room.points.clear();
                room.edges.clear();
                room.points.add(new NativeProjectStore.PointData("open_a",80f,80f));
                room.points.add(new NativeProjectStore.PointData("open_b",350f,80f));
                room.points.add(new NativeProjectStore.PointData("open_c",350f,300f));
                room.edges.add(new NativeProjectStore.EdgeData("open_a","open_b"));
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                drawing.setRoom(room);
                assertFalse(NativeGeometry.isClosed(room));
                activity.onWallLongPressed(room.edges.get(0).id,0f);
                assertFalse("One wall must NOT request numbers",
                        activity.isWallEditorShowingForTest());
                assertFalse(drawing.isWallInputViewportActiveForTest());
                room.edges.add(new NativeProjectStore.EdgeData("open_b","open_c"));
                activity.onWallLongPressed(room.edges.get(0).id,0f);
                assertFalse("Open two-wall polyline must NOT request numbers",
                        activity.isWallEditorShowingForTest());
                assertFalse(drawing.isWallInputViewportActiveForTest());
            });
        }
    }

    @Test public void openingMeasurementsZoomsSmoothlyAndUsesThinBlinkingLine()
            throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,START);
        try(ActivityScenario<NativeWorkspaceActivity> scene=ActivityScenario.launch(intent)){
            scene.onActivity(activity->{
                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                addRectangularRoom(room);
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                drawing.setRoom(room);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            final float[] before=new float[1];
            scene.onActivity(activity->{
                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                drawing.fitView();
                drawing.zoomBy(2.4f);
                before[0]=drawing.getScaleForTest();
                activity.onWallLongPressed(room.edges.get(0).id,room.edges.get(0).value);
                assertTrue(activity.isWallEditorShowingForTest());
                if(NativeMotion.animationsEnabled()){
                    assertEquals("Opening keypad must not jump camera in same frame",
                            before[0],drawing.getScaleForTest(),.0001f);
                }
            });
            SystemClock.sleep(550L);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(activity->{
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertTrue("Camera must zoom out to fit the selected wall",
                        drawing.getScaleForTest()<before[0]*.9f);
                android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(
                        Math.max(1,drawing.getWidth()),Math.max(1,drawing.getHeight()),
                        android.graphics.Bitmap.Config.ARGB_8888);
                drawing.draw(new android.graphics.Canvas(image));
                float density=activity.getResources().getDisplayMetrics().density;
                assertTrue("Blinking blue selected line must be subtle",
                        drawing.getEditingLineWidthPxForTest()<=2.8f*density);
                assertTrue("Wide glow obscures the precise measurement line",
                        drawing.getEditingHaloWidthPxForTest()<=7f*density);
                image.recycle();
                activity.dismissWallInputForTest();
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
