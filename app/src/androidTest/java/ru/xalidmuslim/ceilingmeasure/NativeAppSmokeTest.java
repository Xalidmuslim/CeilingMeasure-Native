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

import java.util.List;
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
            assertNotNull(root.findViewWithTag("native_drawing_check"));
            assertNotNull(root.findViewWithTag("native_history_nav"));
            assertNotNull(root.findViewWithTag("native_nav_redo"));
            assertNotNull(root.findViewWithTag("native_nav_more"));
            assertNotNull(root.findViewWithTag("native_nav_undo"));
            assertNotNull(root.findViewWithTag("native_drawing_hint"));
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

    @Test
    public void roomSnapshotPreservesWebParityFlags() {
        NativeProjectStore.RoomData room = measuredSquare(500f);
        room.geometryLocked = true;
        room.hintCollapsed = true;
        room.wallAngleMagnet = false;
        room.point90Magnet = false;

        NativeProjectStore.RoomData restored =
                NativeProjectStore.roomFromSnapshot(
                        NativeProjectStore.snapshotRoom(room));

        assertTrue(restored.geometryLocked);
        assertTrue(restored.hintCollapsed);
        assertFalse(restored.wallAngleMagnet);
        assertFalse(restored.point90Magnet);
        assertTrue(NativeGeometry.isClosed(restored));
    }

    @Test
    public void exactSquareUsesEnteredMeasurementsForTwentyFiveSquareMetres() {
        NativeProjectStore.RoomData room = measuredSquare(500f);
        Double raw = NativeMeasurementSolver.exactRectAreaRaw(room);
        assertNotNull(raw);
        assertEquals(250000d, raw, .01d);
        assertEquals(25d, raw / 10000d, .0001d);
    }

    @Test
    public void freeAndOrthogonalSolversRespectMeasuredSquare() {
        NativeProjectStore.RoomData room = measuredSquare(500f);

        NativeMeasurementSolver.Result free =
                NativeMeasurementSolver.solveFree(room);
        assertTrue(free.ok);
        assertTrue(free.maxError <= NativeMeasurementSolver.FIELD_MEASURE_TOL);

        NativeMeasurementSolver.Result orthogonal =
                NativeMeasurementSolver.solveOrthogonal(room);
        assertTrue(orthogonal.ok);
        assertTrue(orthogonal.maxError <= NativeMeasurementSolver.FIELD_MEASURE_TOL);
    }

    @Test
    public void autoDiagonalsUseExistingCornersOnly() {
        NativeProjectStore.RoomData room = measuredSquare(500f);
        int beforePoints = room.points.size();
        int added = NativeMeasurementSolver.generateAutoDiagonals(room);
        assertEquals(1, added);
        assertEquals(beforePoints, room.points.size());
        assertEquals(1, room.diagonals.size());
        assertNotNull(NativeGeometry.pointById(room, room.diagonals.get(0).a));
        assertNotNull(NativeGeometry.pointById(room, room.diagonals.get(0).b));
    }

    @Test
    public void shortWallLabelsAvoidEachOtherAndKeepOutsidePlacement() {
        NativeProjectStore.RoomData room = measuredSquare(500f);
        // Make two adjacent walls visually short to stress automatic label placement.
        room.points.get(1).x = 145f;
        room.points.get(2).x = 145f;
        room.points.get(2).y = 145f;
        room.showAutoLetters = false;

        List<NativeLabelEngine.Placement> placements =
                NativeLabelEngine.layout(room, 1f, 3f);
        assertEquals(4, placements.size());

        for (int i = 0; i < placements.size(); i++) {
            NativeLabelEngine.Placement a = placements.get(i);
            NativeLabelEngine.Box boxA = new NativeLabelEngine.Box(
                    a.x - a.width * a.size / 2f,
                    a.y - a.height * a.size / 2f,
                    a.x + a.width * a.size / 2f,
                    a.y + a.height * a.size / 2f);
            for (int j = i + 1; j < placements.size(); j++) {
                NativeLabelEngine.Placement b = placements.get(j);
                NativeLabelEngine.Box boxB = new NativeLabelEngine.Box(
                        b.x - b.width * b.size / 2f,
                        b.y - b.height * b.size / 2f,
                        b.x + b.width * b.size / 2f,
                        b.y + b.height * b.size / 2f);
                assertFalse(NativeLabelEngine.overlaps(boxA, boxB, 0f));
            }
        }
    }

    @Test
    public void labelPosePersistsAngleSizeAndManualOffset() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("native_ceiling_projects", Context.MODE_PRIVATE)
                .edit().clear().commit();

        NativeProjectStore store = new NativeProjectStore(context);
        NativeProjectStore.ProjectData project = store.newProject();
        NativeProjectStore.RoomData room = project.rooms.get(0);
        NativeProjectStore.RoomData square = measuredSquare(500f);
        room.points.clear();
        room.points.addAll(square.points);
        room.edges.clear();
        room.edges.addAll(square.edges);

        NativeProjectStore.EdgeData edge = room.edges.get(0);
        edge.labelDx = 17f;
        edge.labelDy = -9f;
        edge.labelAngle = 37f;
        edge.labelSize = 1.4f;
        edge.labelManual = true;
        store.saveCurrent(project);

        NativeProjectStore.EdgeData restored =
                new NativeProjectStore(context).loadCurrent().rooms.get(0).edges.get(0);
        assertEquals(17f, restored.labelDx, .01f);
        assertEquals(-9f, restored.labelDy, .01f);
        assertEquals(37f, restored.labelAngle, .01f);
        assertEquals(1.4f, restored.labelSize, .01f);
        assertTrue(restored.labelManual);
    }

    @Test
    public void measurementValidationCatchesMissingWallsAndAcceptsMeasuredSquare() {
        NativeProjectStore.RoomData room = measuredSquare(500f);
        NativeMeasurementValidator.Result good = NativeMeasurementValidator.validate(room);
        assertTrue(good.ok());
        assertEquals(25d, good.areaM2, .001d);
        assertEquals(20d, good.perimeterM, .001d);

        room.edges.get(2).value = 0f;
        NativeMeasurementValidator.Result bad = NativeMeasurementValidator.validate(room);
        assertFalse(bad.ok());
        assertTrue(bad.message().contains("Не введены размеры стен"));
    }

    private static NativeProjectStore.RoomData measuredSquare(float size) {
        NativeProjectStore.RoomData room = new NativeProjectStore.RoomData();
        NativeProjectStore.PointData a = new NativeProjectStore.PointData(100f, 100f);
        NativeProjectStore.PointData b = new NativeProjectStore.PointData(300f, 100f);
        NativeProjectStore.PointData c = new NativeProjectStore.PointData(300f, 300f);
        NativeProjectStore.PointData d = new NativeProjectStore.PointData(100f, 300f);
        room.points.add(a);
        room.points.add(b);
        room.points.add(c);
        room.points.add(d);

        NativeProjectStore.EdgeData ab = new NativeProjectStore.EdgeData(a.id, b.id);
        NativeProjectStore.EdgeData bc = new NativeProjectStore.EdgeData(b.id, c.id);
        NativeProjectStore.EdgeData cd = new NativeProjectStore.EdgeData(c.id, d.id);
        NativeProjectStore.EdgeData da = new NativeProjectStore.EdgeData(d.id, a.id);
        ab.value = size;
        bc.value = size;
        cd.value = size;
        da.value = size;
        room.edges.add(ab);
        room.edges.add(bc);
        room.edges.add(cd);
        room.edges.add(da);
        return room;
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
