package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression for wall number input on a scrolled/clipped drawing:
 * follow any selected edge within the ACTUALLY VISIBLE part of the Canvas,
 * even when ScrollView is clamped. No room geometry or saved zoom changes.
 */
@RunWith(AndroidJUnit4.class)
public final class NativeWallFollowCameraTest {
    @Test public void followsTopThenBottomWallWithoutMovingPersistedRoom() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertNotNull(drawing);
                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                room.points.clear();
                room.edges.clear();
                room.points.add(new NativeProjectStore.PointData("a",90f,90f));
                room.points.add(new NativeProjectStore.PointData("b",390f,90f));
                room.points.add(new NativeProjectStore.PointData("c",390f,365f));
                room.points.add(new NativeProjectStore.PointData("d",90f,365f));
                room.edges.add(new NativeProjectStore.EdgeData("a","b"));
                room.edges.add(new NativeProjectStore.EdgeData("b","c"));
                room.edges.add(new NativeProjectStore.EdgeData("c","d"));
                room.edges.add(new NativeProjectStore.EdgeData("d","a"));
                drawing.setRoom(room);
                drawing.fitView();
                float originalScale=drawing.getScaleForTest();
                float originalX=drawing.getPanXForTest();
                float originalY=drawing.getPanYForTest();
                float storedScale=room.viewScale;
                float storedX=room.panX, storedY=room.panY;
                drawing.beginWallInputViewport();

                // Only a narrow central slice of the canvas is visible:
                // the top is clipped by scrolling, the bottom by keypad.
                float top=drawing.getHeight()*.35f;
                float bottom=drawing.getHeight()*.66f;
                assertTrue(bottom-top>25f);

                drawing.setEditingEdgeId(room.edges.get(0).id);
                assertTrue(drawing.focusEditingWallInVisibleBand(top,bottom,false));
                assertTrue(drawing.isEditingWallVisibleInBandForTest(top,bottom));
                float yTop=drawing.getPanYForTest();
                float topCenter=drawing.getEditingWallCenterYForTest();
                assertEquals((top+bottom)/2f,topCenter,2.5f);

                drawing.setEditingEdgeId(room.edges.get(2).id);
                assertTrue(drawing.focusEditingWallInVisibleBand(top,bottom,false));
                assertTrue(drawing.isEditingWallVisibleInBandForTest(top,bottom));
                float bottomCenter=drawing.getEditingWallCenterYForTest();
                assertEquals((top+bottom)/2f,bottomCenter,2.5f);
                assertTrue("Camera must move to follow the other wall",
                        Math.abs(yTop-drawing.getPanYForTest())>15f);

                assertEquals("No saved zoom mutation",storedScale,room.viewScale,.0001f);
                assertEquals("No saved horizontal pan mutation",storedX,room.panX,.0001f);
                assertEquals("No saved vertical pan mutation",storedY,room.panY,.0001f);

                drawing.setEditingEdgeId(null);
                drawing.endWallInputViewport();
                assertEquals("Original zoom restored",originalScale,
                        drawing.getScaleForTest(),.0001f);
                assertEquals("Original horizontal pan restored",originalX,
                        drawing.getPanXForTest(),.0001f);
                assertEquals("Original vertical pan restored",originalY,
                        drawing.getPanYForTest(),.0001f);
            });
        }
    }

    @Test public void verticallyLongWallFitsInsideVisibleBandWithoutChangingGeometry(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                CeilingDrawingView drawing=activity.getWindow().getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertNotNull(drawing);
                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                room.points.clear();
                room.edges.clear();
                room.points.add(new NativeProjectStore.PointData("high",120f,-850f));
                room.points.add(new NativeProjectStore.PointData("low",120f,850f));
                room.edges.add(new NativeProjectStore.EdgeData("high","low"));
                drawing.setRoom(room);
                drawing.fitView();
                drawing.beginWallInputViewport();
                drawing.setEditingEdgeId(room.edges.get(0).id);
                float top=drawing.getHeight()*.40f;
                float bottom=drawing.getHeight()*.60f;
                assertTrue(drawing.focusEditingWallInVisibleBand(top,bottom,false));
                assertTrue("Both endpoints must be visible, not only center",
                        drawing.isEditingWallVisibleInBandForTest(top,bottom));
                assertEquals(-850f,room.points.get(0).y,.0001f);
                assertEquals(850f,room.points.get(1).y,.0001f);
                drawing.setEditingEdgeId(null);
                drawing.endWallInputViewport();
            });
        }
    }
}
