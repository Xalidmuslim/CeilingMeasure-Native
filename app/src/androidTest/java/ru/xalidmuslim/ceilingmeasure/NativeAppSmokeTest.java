package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class NativeAppSmokeTest {

    @Test
    public void launcherResolvesToNativeHome() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(context.getPackageName());

        ResolveInfo resolved = context.getPackageManager().resolveActivity(intent, 0);
        assertNotNull(resolved);
        assertNotNull(resolved.activityInfo);
        assertEquals(NativeHomeActivity.class.getName(), resolved.activityInfo.name);
    }

    @Test
    public void nativeHomeHasCoreActionsAndNoWebView() {
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            AtomicReference<NativeHomeActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeHomeActivity activity = ref.get();
            assertNotNull(activity);

            View root = activity.getWindow().getDecorView();
            assertNotNull(root.findViewWithTag("native_home_new"));
            assertNotNull(root.findViewWithTag("native_home_saved"));
            assertNotNull(root.findViewWithTag("native_home_drawing"));
            assertNotNull(root.findViewWithTag("native_home_settings"));
            assertFalse(containsWebView(root));
        }
    }

    @Test
    public void nativeWorkspaceLoadsCanvasAndModesWithoutWebView() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(
                NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(intent)) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);

            View root = activity.getWindow().getDecorView();
            assertNotNull(root.findViewWithTag("native_order_card"));
            assertNotNull(root.findViewWithTag("native_drawing_card"));
            assertNotNull(root.findViewWithTag("native_drawing_view"));
            assertNotNull(root.findViewWithTag("native_mode_wall"));
            assertNotNull(root.findViewWithTag("native_mode_points"));
            assertNotNull(root.findViewWithTag("native_mode_diagonal"));
            assertNotNull(root.findViewWithTag("native_mode_labels"));
            assertNotNull(root.findViewWithTag("native_save_measurement"));
            assertNotNull(root.findViewWithTag("native_tool_wall_magnet"));
            assertNotNull(root.findViewWithTag("native_tool_point90"));
            assertNotNull(root.findViewWithTag("native_tool_letters"));
            assertNotNull(root.findViewWithTag("native_tool_wall_sequence"));
            assertFalse(containsWebView(root));
        }
    }

    @Test
    public void nativeNumberPadUsesCustomKeysWithZeroBelowEight() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(
                NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(intent)) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                NativeNumberPadDialog dialog = new NativeNumberPadDialog(
                        activity,
                        "Размер стены",
                        0f,
                        value -> {});
                dialog.show();

                View zero = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_number_pad_0");
                View eight = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_number_pad_8");
                View display = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_number_pad_display");
                assertNotNull(zero);
                assertNotNull(eight);
                assertNotNull(display);
                assertTrue(Math.abs(zero.getX() - eight.getX()) < activity.getResources()
                        .getDisplayMetrics().density * 6f);
                dialog.dismiss();
            });
        }
    }

    @Test
    public void nativeRoomControlsUseCompactButtonsAndCustomTextPad() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(
                NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(intent)) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                View root = activity.getWindow().getDecorView();
                View room = root.findViewWithTag("native_room_0");
                View add = root.findViewWithTag("native_room_add");
                assertNotNull(room);
                assertNotNull(add);
                assertTrue(room.isLongClickable());
                assertTrue(room.getLayoutParams().width <= activity.getResources()
                        .getDisplayMetrics().density * 60f);

                NativeTextPadDialog dialog = new NativeTextPadDialog(
                        activity,
                        "Переименовать комнату",
                        "Комната 1",
                        value -> {});
                dialog.show();

                View keyA = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_text_pad_key_А");
                View space = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_text_pad_space");
                View done = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_text_pad_done");
                assertNotNull(keyA);
                assertNotNull(space);
                assertNotNull(done);
                dialog.dismiss();
            });
        }
    }

    @Test
    public void nativeDrawingUsesWebStyleWallGestureRules() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(
                NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(intent)) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                CeilingDrawingView view = (CeilingDrawingView) activity.getWindow()
                        .getDecorView()
                        .findViewWithTag("native_drawing_view");
                assertNotNull(view);
                NativeProjectStore.RoomData room = activity.getActiveRoomForTest();

                long now = android.os.SystemClock.uptimeMillis();
                dispatch(view, now, MotionEvent.ACTION_DOWN, 100, 100);
                dispatch(view, now + 40, MotionEvent.ACTION_UP, 100, 100);
                assertEquals(0, room.points.size());
                assertEquals(0, room.edges.size());

                drag(view, now + 100, 100, 100, 260, 100);
                assertEquals(2, room.points.size());
                assertEquals(1, room.edges.size());

                drag(view, now + 300, 260, 100, 260, 250);
                assertEquals(3, room.points.size());
                assertEquals(2, room.edges.size());

                // Blank space cannot start a disconnected wall after drawing begins.
                drag(view, now + 500, 400, 300, 520, 300);
                assertEquals(2, room.edges.size());

                drag(view, now + 700, 260, 250, 100, 250);
                drag(view, now + 900, 100, 250, 100, 100);

                assertEquals(4, room.edges.size());
                assertTrue(NativeGeometry.isClosed(room));
            });
        }
    }

    @Test
    public void nativeProjectStorePersistsGraphGeometry() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("native_ceiling_projects", Context.MODE_PRIVATE)
                .edit().clear().commit();

        NativeProjectStore first = new NativeProjectStore(context);
        NativeProjectStore.ProjectData project = first.newProject();
        project.name = "Тестовый объект";
        project.phone = "+7 900 000-00-00";
        project.address = "Тестовый адрес";
        NativeProjectStore.RoomData room = project.rooms.get(0);

        NativeProjectStore.PointData a = new NativeProjectStore.PointData(50f, 50f);
        NativeProjectStore.PointData b = new NativeProjectStore.PointData(250f, 50f);
        NativeProjectStore.PointData c = new NativeProjectStore.PointData(250f, 250f);
        NativeProjectStore.PointData d = new NativeProjectStore.PointData(50f, 250f);
        room.points.add(a);
        room.points.add(b);
        room.points.add(c);
        room.points.add(d);

        NativeProjectStore.EdgeData ab = new NativeProjectStore.EdgeData(a.id, b.id);
        NativeProjectStore.EdgeData bc = new NativeProjectStore.EdgeData(b.id, c.id);
        NativeProjectStore.EdgeData cd = new NativeProjectStore.EdgeData(c.id, d.id);
        NativeProjectStore.EdgeData da = new NativeProjectStore.EdgeData(d.id, a.id);
        ab.value = 500f;
        bc.value = 500f;
        cd.value = 500f;
        da.value = 500f;
        room.edges.add(ab);
        room.edges.add(bc);
        room.edges.add(cd);
        room.edges.add(da);

        NativeProjectStore.DiagonalData ac = new NativeProjectStore.DiagonalData(a.id, c.id);
        ac.value = 707f;
        room.diagonals.add(ac);
        first.saveCurrent(project);

        NativeProjectStore second = new NativeProjectStore(context);
        NativeProjectStore.ProjectData restored = second.loadCurrent();
        NativeProjectStore.RoomData restoredRoom = restored.rooms.get(0);

        assertEquals("Тестовый объект", restored.name);
        assertEquals("+7 900 000-00-00", restored.phone);
        assertEquals("Тестовый адрес", restored.address);
        assertEquals(4, restoredRoom.points.size());
        assertEquals(4, restoredRoom.edges.size());
        assertEquals(1, restoredRoom.diagonals.size());
        assertTrue(NativeGeometry.isClosed(restoredRoom));
        assertEquals(500f, restoredRoom.edges.get(0).value, 0.01f);
        assertEquals(707f, restoredRoom.diagonals.get(0).value, 0.01f);
        restoredRoom.points.get(0).customLabel = "A";
        restoredRoom.showAutoLetters = true;
        second.saveCurrent(restored);

        NativeProjectStore.ProjectData third = new NativeProjectStore(context).loadCurrent();
        assertEquals("A", third.rooms.get(0).points.get(0).customLabel);
        assertTrue(third.rooms.get(0).showAutoLetters);
    }

    @Test
    public void geometryExtendsStraightWallInsteadOfCreatingExtraCorner() {
        NativeProjectStore.RoomData room = new NativeProjectStore.RoomData();
        NativeProjectStore.PointData a = new NativeProjectStore.PointData(100f, 100f);
        NativeProjectStore.PointData b = new NativeProjectStore.PointData(250f, 100f);
        room.points.add(a);
        room.points.add(b);
        NativeProjectStore.EdgeData edge = new NativeProjectStore.EdgeData(a.id, b.id);
        edge.value = 500f;
        room.edges.add(edge);

        boolean extended = NativeGeometry.extendStraightWall(
                room,
                b.id,
                400f,
                100f,
                null);

        assertTrue(extended);
        assertEquals(2, room.points.size());
        assertEquals(1, room.edges.size());
        assertEquals(400f, NativeGeometry.pointById(room, b.id).x, 0.01f);
        assertEquals(0f, room.edges.get(0).value, 0.01f);
    }

    @Test
    public void magnifierAppearsDuringNativeWallDrag() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(
                NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(intent)) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                CeilingDrawingView view = (CeilingDrawingView) activity.getWindow()
                        .getDecorView()
                        .findViewWithTag("native_drawing_view");
                long now = android.os.SystemClock.uptimeMillis();
                dispatch(view, now, MotionEvent.ACTION_DOWN, 120, 120);
                dispatch(view, now + 35, MotionEvent.ACTION_MOVE, 270, 120);
                assertTrue(view.isMagnifierVisibleForTest());
                dispatch(view, now + 70, MotionEvent.ACTION_UP, 270, 120);
                assertFalse(view.isMagnifierVisibleForTest());
            });
        }
    }

    private static void drag(
            CeilingDrawingView view,
            long downTime,
            float x1,
            float y1,
            float x2,
            float y2) {
        dispatch(view, downTime, MotionEvent.ACTION_DOWN, x1, y1);
        dispatch(view, downTime + 35, MotionEvent.ACTION_MOVE, x2, y2);
        dispatch(view, downTime + 70, MotionEvent.ACTION_UP, x2, y2);
    }

    private static void dispatch(
            CeilingDrawingView view,
            long downTime,
            int action,
            float x,
            float y) {
        MotionEvent event = MotionEvent.obtain(
                downTime,
                android.os.SystemClock.uptimeMillis(),
                action,
                x,
                y,
                0);
        view.dispatchTouchEvent(event);
        event.recycle();
    }

    private static boolean containsWebView(View view) {
        if ("android.webkit.WebView".equals(view.getClass().getName())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsWebView(group.getChildAt(i))) return true;
            }
        }
        return false;
    }
}
