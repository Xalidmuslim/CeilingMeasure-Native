package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.graphics.PointF;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class NativeFactualGeometryTest {
    @Test
    public void factualGeometryRequiresFieldMeasuredDiagonal() {
        NativeProjectStore.RoomData room = rectangleRoom();
        NativeProjectStore.DiagonalData diagonal =
                new NativeProjectStore.DiagonalData("p0", "p2");
        diagonal.value = 500f;
        room.diagonals.add(diagonal);

        NativeGeometry.markDiagonalAuto(diagonal);
        NativeMeasurementSolver.Result autoOnly =
                NativeMeasurementSolver.solveFactual(room);
        assertFalse(autoOnly.ok);
        assertTrue(autoOnly.message.contains("измеренных диагоналей"));

        NativeGeometry.markDiagonalMeasured(diagonal);
        NativeMeasurementSolver.Result measured =
                NativeMeasurementSolver.solveFactual(room);
        assertTrue(measured.ok);
        assertTrue(measured.uniquelySupported);
        assertEquals(0f, measured.maxError, .01f);
        assertDistance(measured.positions, "p0", "p2", 500f);
    }

    @Test
    public void conflictingExtraMeasuredDiagonalIsReportedNotHidden() {
        NativeProjectStore.RoomData room = rectangleRoom();

        NativeProjectStore.DiagonalData required =
                new NativeProjectStore.DiagonalData("p0", "p2");
        required.value = 500f;
        NativeGeometry.markDiagonalMeasured(required);
        room.diagonals.add(required);

        NativeProjectStore.DiagonalData conflict =
                new NativeProjectStore.DiagonalData("p1", "p3");
        // 5.09 m instead of the factual 5.00 m: this is below the old
        // 0.10 m working tolerance but must still be treated as a conflict.
        conflict.value = 509f;
        NativeGeometry.markDiagonalMeasured(conflict);
        room.diagonals.add(conflict);

        NativeMeasurementSolver.Result result =
                NativeMeasurementSolver.solveFactual(room);
        assertTrue(result.ok);
        assertTrue(result.maxError > NativeMeasurementSolver.FACTUAL_CONFLICT_TOL);
        assertTrue(result.maxError < NativeMeasurementSolver.FIELD_MEASURE_TOL);
        assertTrue(result.message.contains("Замеры конфликтуют"));
    }

    @Test
    public void sketchGeometryCanBeRestoredExactlyAfterFactualShape() {
        NativeProjectStore.RoomData room = rectangleRoom();
        assertTrue(NativeGeometry.captureSketchGeometry(room));

        float[] before = coordinates(room);
        room.points.get(1).x += 31f;
        room.points.get(2).y -= 47f;
        NativeGeometry.markFactualGeometry(room);
        assertTrue(NativeGeometry.isFactualGeometry(room));

        assertTrue(NativeGeometry.restoreSketchGeometry(room));
        assertArrayEquals(before, coordinates(room), .0001f);
        assertFalse(NativeGeometry.isFactualGeometry(room));
        assertFalse(NativeGeometry.hasSketchGeometryBackup(room));
    }

    @Test
    public void applyingFactualGeometryPreservesZoomAndPan() {
        Context context = ApplicationProvider.getApplicationContext();
        NativeProjectStore.RoomData room = rectangleRoom();
        // Deliberately skew only the sketch coordinates.
        room.points.get(2).x = 435f;
        room.points.get(2).y = 475f;
        room.points.get(3).x = 92f;
        room.points.get(3).y = 525f;
        room.viewScale = 1.82f;
        room.panX = 37f;
        room.panY = -21f;

        NativeProjectStore.DiagonalData diagonal =
                new NativeProjectStore.DiagonalData("p0", "p2");
        diagonal.value = 500f;
        NativeGeometry.markDiagonalMeasured(diagonal);
        room.diagonals.add(diagonal);

        NativeMeasurementSolver.Result result =
                NativeMeasurementSolver.solveFactual(room);
        assertTrue(result.ok);

        AtomicReference<CeilingDrawingView> ref = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CeilingDrawingView view = new CeilingDrawingView(context);
            view.layout(0, 0, 900, 900);
            view.setRoom(room);
            view.applySolvedGeometry(result.positions);
            ref.set(view);
        });

        CeilingDrawingView view = ref.get();
        assertNotNull(view);
        assertEquals(1.82f, view.getScaleForTest(), .0001f);
        assertEquals(37f, view.getPanXForTest(), .0001f);
        assertEquals(-21f, view.getPanYForTest(), .0001f);

        // The factual 3-4-5 rectangle is straight again in world coordinates.
        NativeProjectStore.PointData p0 = NativeGeometry.pointById(room, "p0");
        NativeProjectStore.PointData p1 = NativeGeometry.pointById(room, "p1");
        NativeProjectStore.PointData p2 = NativeGeometry.pointById(room, "p2");
        NativeProjectStore.PointData p3 = NativeGeometry.pointById(room, "p3");
        assertNotNull(p0);
        assertNotNull(p1);
        assertNotNull(p2);
        assertNotNull(p3);
        assertEquals(p0.y, p1.y, .01f);
        assertEquals(p1.x, p2.x, .01f);
        assertEquals(p2.y, p3.y, .01f);
        assertEquals(p3.x, p0.x, .01f);
    }

    private static NativeProjectStore.RoomData rectangleRoom() {
        NativeProjectStore.RoomData room = new NativeProjectStore.RoomData();
        room.points.add(new NativeProjectStore.PointData("p0", 100f, 100f));
        room.points.add(new NativeProjectStore.PointData("p1", 400f, 100f));
        room.points.add(new NativeProjectStore.PointData("p2", 400f, 500f));
        room.points.add(new NativeProjectStore.PointData("p3", 100f, 500f));

        room.edges.add(edge("p0", "p1", 300f));
        room.edges.add(edge("p1", "p2", 400f));
        room.edges.add(edge("p2", "p3", 300f));
        room.edges.add(edge("p3", "p0", 400f));
        return room;
    }

    private static NativeProjectStore.EdgeData edge(
            String a,
            String b,
            float value) {
        NativeProjectStore.EdgeData edge = new NativeProjectStore.EdgeData(a, b);
        edge.value = value;
        return edge;
    }

    private static void assertDistance(
            Map<String, PointF> positions,
            String a,
            String b,
            float expected) {
        PointF pa = positions.get(a);
        PointF pb = positions.get(b);
        assertNotNull(pa);
        assertNotNull(pb);
        assertEquals(expected, Math.hypot(pb.x - pa.x, pb.y - pa.y), .01d);
    }

    private static float[] coordinates(NativeProjectStore.RoomData room) {
        float[] out = new float[room.points.size() * 2];
        for (int i = 0; i < room.points.size(); i++) {
            out[i * 2] = room.points.get(i).x;
            out[i * 2 + 1] = room.points.get(i).y;
        }
        return out;
    }
}
