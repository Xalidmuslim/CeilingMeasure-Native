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
import android.widget.GridLayout;
import android.widget.FrameLayout;
import android.view.MotionEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
            assertNotNull(root.findViewWithTag("native_nav_home"));
            assertNotNull(root.findViewWithTag("native_nav_redo"));
            assertNotNull(root.findViewWithTag("native_nav_more"));
            assertNotNull(root.findViewWithTag("native_nav_undo"));
            assertNotNull(root.findViewWithTag("native_drawing_hint"));
            View retiredScrollStrip = root.findViewWithTag("native_wall_scroll_strip");
            assertNotNull(retiredScrollStrip);
            assertEquals(View.GONE, retiredScrollStrip.getVisibility());
            assertNotNull(root.findViewWithTag("native_editor_scroll"));
            assertNotNull(root.findViewWithTag("native_top_home"));
            assertNotNull(root.findViewWithTag("native_top_saved"));
            assertNotNull(root.findViewWithTag("native_top_new_order"));
            assertNotNull(root.findViewWithTag("native_top_settings"));
            assertNotNull(root.findViewWithTag("native_open_client_estimate"));
            assertNotNull(root.findViewWithTag("native_open_internal_estimate"));
            assertNotNull(root.findViewWithTag("native_room_notes"));
            assertFalse(containsWebView(root));
        }
    }

    @Test
    public void wallAndDiagonalParityDialogsExposeWebControls() {
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
                NativeWallMeasureDialog wall = new NativeWallMeasureDialog(
                        activity,
                        "Стена 1 из 4",
                        500f,
                        true,
                        new NativeWallMeasureDialog.Listener(){
                            public void onSave(float v){}
                            public void onDelete(){}
                            public void onBack(){}
                            public void onClose(){}
                        });
                wall.show();
                View root = wall.findViewById(android.R.id.content).getRootView();
                assertNotNull(root.findViewWithTag("native_wall_value"));
                assertNotNull(root.findViewWithTag("native_wall_decimal"));
                assertNotNull(root.findViewWithTag("native_wall_meter_hint"));
                assertNotNull(root.findViewWithTag("native_wall_done"));
                wall.dismiss();

                NativeDiagonalMeasureDialog diag = new NativeDiagonalMeasureDialog(
                        activity,
                        "Диагональ AC",
                        0f,
                        707f,
                        new NativeDiagonalMeasureDialog.Listener(){
                            public void onSave(float v){}
                            public void onUseAuto(float v){}
                            public void onDelete(){}
                            public void onClose(){}
                        });
                diag.show();
                View droot = diag.findViewById(android.R.id.content).getRootView();
                assertNotNull(droot.findViewWithTag("native_diag_auto"));
                assertNotNull(droot.findViewWithTag("native_diag_use_auto"));
                assertNotNull(droot.findViewWithTag("native_diag_delete"));
                diag.dismiss();
            });
        }
    }

    @Test
    public void wallKeypadMatchesWebEntryRules() {
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
                NativeWallMeasureDialog dialog = new NativeWallMeasureDialog(
                        activity,
                        "Стена 1 из 4",
                        0f,
                        true,
                        new NativeWallMeasureDialog.Listener(){
                            public void onSave(float value){}
                            public void onDelete(){}
                            public void onBack(){}
                            public void onClose(){}
                        });
                dialog.show();

                GridLayout keypad = (GridLayout) dialog.findViewById(
                        android.R.id.content).getRootView()
                        .findViewWithTag("native_wall_keypad");
                assertNotNull(keypad);
                assertEquals(3, keypad.getColumnCount());

                View five = dialog.findViewById(android.R.id.content).getRootView()
                        .findViewWithTag("native_wall_key_5");
                View eight = dialog.findViewById(android.R.id.content).getRootView()
                        .findViewWithTag("native_wall_key_8");
                assertNotNull(five);
                assertNotNull(eight);
                five.performClick();
                eight.performClick();
                assertEquals("58", dialog.rawValueForTest());
                assertEquals(58f, dialog.currentUnitsOrNull(), .01f);

                View backspace = dialog.findViewById(android.R.id.content).getRootView()
                        .findViewWithTag("native_wall_key_⌫");
                View decimal = dialog.findViewById(android.R.id.content).getRootView()
                        .findViewWithTag("native_wall_decimal");
                assertNotNull(backspace);
                assertNotNull(decimal);
                backspace.performClick();
                backspace.performClick();
                decimal.performClick();
                five.performClick();
                eight.performClick();
                assertEquals("0,58", dialog.rawValueForTest());
                assertEquals(58f, dialog.currentUnitsOrNull(), .01f);
                dialog.dismiss();
            });
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
    public void openingRoomFitsWholeContourInsteadOfRestoringStaleZoom() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicReference<CeilingDrawingView> viewRef = new AtomicReference<>();
        AtomicReference<NativeProjectStore.RoomData> roomRef = new AtomicReference<>();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            FrameLayout parent = new FrameLayout(context);
            CeilingDrawingView view = new CeilingDrawingView(context);
            parent.addView(view, new FrameLayout.LayoutParams(800, 800));
            parent.layout(0, 0, 800, 800);
            view.layout(0, 0, 800, 800);

            NativeProjectStore.RoomData room = measuredSquare(500f);
            room.viewScale = 6f;
            room.panX = -1800f;
            room.panY = 900f;
            view.setRoom(room);
            viewRef.set(view);
            roomRef.set(room);
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CeilingDrawingView view = viewRef.get();
            NativeProjectStore.RoomData room = roomRef.get();
            assertNotNull(view);
            assertNotNull(room);
            assertTrue("stale saved zoom must be replaced by fit", view.getScaleForTest() < 6f);

            for (NativeProjectStore.PointData point : room.points) {
                float sx = point.x * view.getScaleForTest() + view.getPanXForTest();
                float sy = point.y * view.getScaleForTest() + view.getPanYForTest();
                assertTrue("point must be visible horizontally", sx >= 35f && sx <= 765f);
                assertTrue("point must be visible vertically", sy >= 35f && sy <= 765f);
            }
        });
    }

    @Test
    public void nativePinchZoomUsesTwoFingersAndKeepsCentroidStable() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicReference<CeilingDrawingView> viewRef = new AtomicReference<>();
        AtomicReference<float[]> beforeRef = new AtomicReference<>();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            FrameLayout parent = new FrameLayout(context);
            CeilingDrawingView view = new CeilingDrawingView(context);
            parent.addView(view, new FrameLayout.LayoutParams(800, 800));
            parent.layout(0, 0, 800, 800);
            view.layout(0, 0, 800, 800);

            NativeProjectStore.RoomData room = measuredSquare(500f);
            view.setRoom(room);
            view.fitView();

            float scale = view.getScaleForTest();
            float worldCx = (400f - view.getPanXForTest()) / scale;
            float worldCy = (400f - view.getPanYForTest()) / scale;
            beforeRef.set(new float[]{scale, worldCx, worldCy});

            long now = android.os.SystemClock.uptimeMillis();
            dispatch(view, now, MotionEvent.ACTION_DOWN, 250f, 400f);
            dispatchTwo(
                    view,
                    now,
                    now + 20,
                    MotionEvent.ACTION_POINTER_DOWN
                            | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    250f, 400f,
                    550f, 400f);
            dispatchTwo(
                    view,
                    now,
                    now + 60,
                    MotionEvent.ACTION_MOVE,
                    180f, 400f,
                    620f, 400f);
            dispatchTwo(
                    view,
                    now,
                    now + 90,
                    MotionEvent.ACTION_POINTER_UP
                            | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    180f, 400f,
                    620f, 400f);
            dispatch(view, now, MotionEvent.ACTION_UP, 180f, 400f);
            viewRef.set(view);
        });

        CeilingDrawingView view = viewRef.get();
        float[] before = beforeRef.get();
        assertNotNull(view);
        assertNotNull(before);
        float afterScale = view.getScaleForTest();
        assertTrue("pinch must zoom in", afterScale > before[0] * 1.25f);

        float afterWorldCx = (400f - view.getPanXForTest()) / afterScale;
        float afterWorldCy = (400f - view.getPanYForTest()) / afterScale;
        assertEquals(before[1], afterWorldCx, 1.5f);
        assertEquals(before[2], afterWorldCy, 1.5f);
    }

    @Test
    public void numbersModeLongPressOnDisplayedNumberOpensMeasurementEdit() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CountDownLatch edited = new CountDownLatch(1);
        AtomicReference<String> editedEdge = new AtomicReference<>();
        AtomicReference<CeilingDrawingView> viewRef = new AtomicReference<>();
        AtomicReference<float[]> touchRef = new AtomicReference<>();
        AtomicReference<Long> downRef = new AtomicReference<>();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            FrameLayout parent = new FrameLayout(context);
            CeilingDrawingView view = new CeilingDrawingView(context);
            parent.addView(view, new FrameLayout.LayoutParams(800, 800));
            parent.layout(0, 0, 800, 800);
            view.layout(0, 0, 800, 800);

            NativeProjectStore.RoomData room = measuredSquare(500f);
            view.setRoom(room);
            view.fitView();
            view.setMode(CeilingDrawingView.Mode.LABEL);
            view.setListener(new CeilingDrawingView.Listener() {
                @Override public void onChanged() {}
                @Override public void onHistoryCheckpoint() {}
                @Override public void onVertexLongPressed(String pointId) {}
                @Override public void onWallLongPressed(String edgeId, float currentValue) {
                    editedEdge.set(edgeId);
                    edited.countDown();
                }
                @Override public void onWallSequencePick(String edgeId) {}
                @Override public void onDiagonalCreated(String diagonalId, float currentValue) {}
                @Override public void onDiagonalLongPressed(String diagonalId, float currentValue) {}
                @Override public void onContourClosed() {}
            });

            NativeLabelEngine.Placement placement = null;
            for (NativeLabelEngine.Placement item : view.labelPlacementsForTest()) {
                if (item.kind == NativeLabelEngine.Kind.EDGE) {
                    placement = item;
                    break;
                }
            }
            assertNotNull(placement);

            float sx = placement.x * view.getScaleForTest() + view.getPanXForTest();
            float sy = placement.y * view.getScaleForTest() + view.getPanYForTest();
            long now = android.os.SystemClock.uptimeMillis();
            touchRef.set(new float[]{sx, sy});
            downRef.set(now);
            dispatch(view, now, MotionEvent.ACTION_DOWN, sx, sy);
            viewRef.set(view);
        });

        assertTrue("holding a number must trigger measurement edit",
                edited.await(1200, TimeUnit.MILLISECONDS));

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            float[] p = touchRef.get();
            dispatch(
                    viewRef.get(),
                    downRef.get(),
                    MotionEvent.ACTION_UP,
                    p[0],
                    p[1]);
        });

        assertNotNull(editedEdge.get());
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
    public void savedBackupRoundTripPreservesCompletedMeasurements() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("native_ceiling_projects", Context.MODE_PRIVATE)
                .edit().clear().commit();

        NativeProjectStore store = new NativeProjectStore(context);
        NativeProjectStore.ProjectData project = store.newProject();
        project.name = "Квартира 189";
        project.address = "Тестовый адрес";
        store.saveSnapshot(project);

        String backup = store.exportBackup();
        assertTrue(backup.contains("Квартира 189"));

        context.getSharedPreferences("native_ceiling_projects", Context.MODE_PRIVATE)
                .edit().clear().commit();
        NativeProjectStore restoredStore = new NativeProjectStore(context);
        assertTrue(restoredStore.importBackup(backup));
        List<NativeProjectStore.ProjectData> saved = restoredStore.loadSaved();
        assertEquals(1, saved.size());
        assertEquals("Квартира 189", saved.get(0).name);
        assertTrue(saved.get(0).savedAt > 0);
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
    public void defaultPriceListsAndCustomMaterialsFollowNewRooms() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("native_ceiling_projects", Context.MODE_PRIVATE)
                .edit().clear().commit();

        NativeProjectStore store = new NativeProjectStore(context);
        store.saveDefaultPrice(false,"canvas",222f);
        store.saveDefaultPrice(true,"canvas",555f);

        NativeProjectStore.MaterialData purchase = new NativeProjectStore.MaterialData();
        purchase.id = "preset_purchase";
        purchase.name = "Клей";
        purchase.unit = "шт";
        purchase.price = 90f;
        store.upsertDefaultCustom(false,purchase);

        NativeProjectStore.MaterialData client = new NativeProjectStore.MaterialData();
        client.id = "preset_client";
        client.name = "Монтаж";
        client.unit = "м";
        client.price = 130f;
        store.upsertDefaultCustom(true,client);

        NativeProjectStore.RoomData room = store.createRoom("Комната 2");
        assertEquals(222f,NativeEstimateEngine.value(room.pricing,"canvas"),.01f);
        assertEquals(555f,NativeEstimateEngine.value(room.clientPricing,"canvas"),.01f);
        assertEquals(1,room.customItems.size());
        assertEquals("Клей",room.customItems.get(0).name);
        assertEquals(1,room.clientCustomItems.size());
        assertEquals("Монтаж",room.clientCustomItems.get(0).name);
    }

    @Test
    public void customMaterialsAndMergedOrderSelectionPersist() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("native_ceiling_projects", Context.MODE_PRIVATE)
                .edit().clear().commit();

        NativeProjectStore store = new NativeProjectStore(context);
        NativeProjectStore.ProjectData project = store.newProject();
        project.combinedEstimateOrderIds.add("project_other");
        NativeProjectStore.MaterialData item = new NativeProjectStore.MaterialData();
        item.name = "Допматериал";
        item.qty = 3f;
        item.price = 100f;
        project.rooms.get(0).clientCustomItems.add(item);
        store.saveCurrent(project);

        NativeProjectStore.ProjectData restored = new NativeProjectStore(context).loadCurrent();
        assertTrue(restored.combinedEstimateOrderIds.contains("project_other"));
        assertEquals(1,restored.rooms.get(0).clientCustomItems.size());
        assertEquals("Допматериал",restored.rooms.get(0).clientCustomItems.get(0).name);
    }

    @Test
    public void clientEstimatePdfBuildsOfflineLikeWebV184() {
        NativeProjectStore.ProjectData project = new NativeProjectStore.ProjectData();
        project.name = "Квартира 189";
        project.phone = "+7 900 000-00-00";
        project.address = "Тестовый адрес";
        NativeProjectStore.RoomData room = measuredSquare(500f);
        room.name = "Комната";
        room.notes = "Скрытый карниз";
        room.clientPricing.put("canvas", 500f);
        project.rooms.add(room);

        NativeClientEstimatePdf.Result pdf =
                NativeClientEstimatePdf.build(java.util.Collections.singletonList(project));

        assertTrue(pdf.pricedLines > 0);
        assertTrue(pdf.bytes.length > 1000);
        assertEquals('%', (char) pdf.bytes[0]);
        assertEquals('P', (char) pdf.bytes[1]);
        assertEquals('D', (char) pdf.bytes[2]);
        assertEquals('F', (char) pdf.bytes[3]);
        assertTrue(pdf.fileName.startsWith("smeta_"));
        assertTrue(pdf.fileName.endsWith(".pdf"));
    }

    @Test
    public void estimateEngineMatchesWebStandardMaterialRules() {
        NativeProjectStore.RoomData room = measuredSquare(500f);
        room.pricing.put("canvas", 100f);
        room.pricing.put("baguette", 50f);
        room.pricing.put("dowel", 2f);
        room.clientPricing.put("canvas", 500f);
        room.clientPricing.put("insert", 100f);
        room.extraQty.put("light", 4f);
        room.clientPricing.put("light", 300f);

        NativeEstimateEngine.RoomCost internal =
                NativeEstimateEngine.internalCost(room);
        assertEquals(25d, internal.metrics.areaM2, .001d);
        assertEquals(20d, internal.metrics.perimeterM, .001d);
        assertEquals(120, internal.dowelQty);
        assertEquals(3740d, internal.total, .001d);

        NativeEstimateEngine.RoomCost client =
                NativeEstimateEngine.clientCost(room);
        assertEquals(15700d, client.total, .001d);

        room.hiddenInternalKeys.add("dowel");
        NativeEstimateEngine.RoomCost hidden =
                NativeEstimateEngine.internalCost(room);
        assertEquals(3500d, hidden.total, .001d);
    }

    @Test
    public void estimateFieldsPersistWithRoom() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("native_ceiling_projects", Context.MODE_PRIVATE)
                .edit().clear().commit();
        NativeProjectStore store = new NativeProjectStore(context);
        NativeProjectStore.ProjectData project = store.newProject();
        NativeProjectStore.RoomData room = project.rooms.get(0);
        room.notes = "Скрытый карниз";
        room.pricing.put("canvas", 215f);
        room.clientPricing.put("canvas", 550f);
        room.extraQty.put("light", 7f);
        room.clientEstimateQty.put("shadow", 12f);
        room.clientEstimateOverride.add("shadow");
        room.hiddenInternalKeys.add("insert");
        store.saveCurrent(project);

        NativeProjectStore.RoomData restored =
                new NativeProjectStore(context).loadCurrent().rooms.get(0);
        assertEquals("Скрытый карниз", restored.notes);
        assertEquals(215f, NativeEstimateEngine.value(restored.pricing,"canvas"), .01f);
        assertEquals(550f, NativeEstimateEngine.value(restored.clientPricing,"canvas"), .01f);
        assertEquals(7f, NativeEstimateEngine.value(restored.extraQty,"light"), .01f);
        assertTrue(restored.clientEstimateOverride.contains("shadow"));
        assertTrue(restored.hiddenInternalKeys.contains("insert"));
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
    public void labelPosePersistsAbsoluteWorldCoordinatesAngleAndSize() {
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
        edge.labelX = 117f;
        edge.labelY = 91f;
        edge.labelDx = 0f;
        edge.labelDy = 0f;
        edge.labelAngle = 37f;
        edge.labelSize = 1.4f;
        edge.labelManual = true;
        store.saveCurrent(project);

        NativeProjectStore.EdgeData restored =
                new NativeProjectStore(context).loadCurrent().rooms.get(0).edges.get(0);
        assertNotNull(restored.labelX);
        assertNotNull(restored.labelY);
        assertEquals(117f, restored.labelX, .01f);
        assertEquals(91f, restored.labelY, .01f);
        assertEquals(0f, restored.labelDx, .01f);
        assertEquals(0f, restored.labelDy, .01f);
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

    private static void dispatchTwo(
            CeilingDrawingView view,
            long downTime,
            long eventTime,
            int action,
            float x0,
            float y0,
            float x1,
            float y1) {
        MotionEvent.PointerProperties[] properties = {
                new MotionEvent.PointerProperties(),
                new MotionEvent.PointerProperties()
        };
        properties[0].id = 0;
        properties[0].toolType = MotionEvent.TOOL_TYPE_FINGER;
        properties[1].id = 1;
        properties[1].toolType = MotionEvent.TOOL_TYPE_FINGER;

        MotionEvent.PointerCoords[] coords = {
                new MotionEvent.PointerCoords(),
                new MotionEvent.PointerCoords()
        };
        coords[0].x = x0;
        coords[0].y = y0;
        coords[0].pressure = 1f;
        coords[0].size = 1f;
        coords[1].x = x1;
        coords[1].y = y1;
        coords[1].pressure = 1f;
        coords[1].size = 1f;

        MotionEvent event = MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                2,
                properties,
                coords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                0,
                0);
        view.dispatchTouchEvent(event);
        event.recycle();
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
