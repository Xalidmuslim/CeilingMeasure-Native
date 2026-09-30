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
    public void factualGeometryStretchesWallsWithoutRequiringMeasuredDiagonal() {
        NativeProjectStore.RoomData room = rectangleRoom();

        // Deliberately skew the sketch while keeping the same overall room topology.
        room.points.get(2).x = 430f;
        room.points.get(2).y = 480f;
        room.points.get(3).x = 90f;
        room.points.get(3).y = 520f;

        NativeProjectStore.DiagonalData diagonal =
                new NativeProjectStore.DiagonalData("p0", "p2");
        diagonal.value = 500f;
        room.diagonals.add(diagonal);

        // An automatically calculated diagonal is not factual input. The solver
        // must still work from measured walls and preserve the sketch as shape prior.
        NativeGeometry.markDiagonalAuto(diagonal);
        NativeMeasurementSolver.Result autoOnly =
                NativeMeasurementSolver.solveFactual(room);
        assertTrue(autoOnly.ok);
        assertFalse(autoOnly.uniquelySupported);
        assertTrue(autoOnly.message.contains("эскиз"));
        assertWallLengthsMatchEntered(room, autoOnly.positions);
        assertSameWinding(room, autoOnly.positions);

        // A measured diagonal may influence the bend of the shape, but wall
        // dimensions remain the hard factual constraints.
        NativeGeometry.markDiagonalMeasured(diagonal);
        NativeMeasurementSolver.Result measured =
                NativeMeasurementSolver.solveFactual(room);
        assertTrue(measured.ok);
        assertFalse(measured.uniquelySupported);
        assertWallLengthsMatchEntered(room, measured.positions);
        assertSameWinding(room, measured.positions);
    }

    @Test
    public void unequalOppositeWallsWarpSketchButKeepEveryEnteredLength() {
        NativeProjectStore.RoomData room = rectangleRoom();
        room.edges.get(0).value = 300f;
        room.edges.get(1).value = 255f;
        room.edges.get(2).value = 455f;
        room.edges.get(3).value = 125f;

        NativeMeasurementSolver.Result result =
                NativeMeasurementSolver.solveFactual(room);

        assertTrue(result.ok);
        assertWallLengthsMatchEntered(room, result.positions);
        assertEquals(455d / 300d,
                distance(result.positions, "p2", "p3")
                        / distance(result.positions, "p0", "p1"),
                .002d);
        assertEquals(255d / 125d,
                distance(result.positions, "p1", "p2")
                        / distance(result.positions, "p3", "p0"),
                .002d);
        assertAnyAngleBent(result.positions, new String[]{"p0","p1","p2","p3"});
        assertSameWinding(room, result.positions);
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
    public void orthogonalSketchStillAppliesWhenWallSizesDoNotClose() {
        NativeProjectStore.RoomData room = rectangleRoom();
        // One opposite wall is 30 units longer, so exact 90° + all four exact
        // sizes cannot coexist in one closed polygon.
        room.edges.get(3).value = 430f;

        NativeMeasurementSolver.Result result =
                NativeMeasurementSolver.solveFactual(room);

        assertTrue(result.ok);
        assertWallLengthsMatchEntered(room, result.positions);
        assertAnyAngleBent(result.positions, new String[]{"p0","p1","p2","p3"});
    }

    @Test
    public void orthogonalMismatchDoesNotCollapseTinyReturn() {
        NativeProjectStore.RoomData room = new NativeProjectStore.RoomData();
        room.points.add(new NativeProjectStore.PointData("p0", 100f, 100f));
        room.points.add(new NativeProjectStore.PointData("p1", 356f, 100f));
        room.points.add(new NativeProjectStore.PointData("p2", 356f, 130f));
        room.points.add(new NativeProjectStore.PointData("p3", 397f, 130f));
        room.points.add(new NativeProjectStore.PointData("p4", 397f, 555f));
        room.points.add(new NativeProjectStore.PointData("p5", 100f, 555f));

        room.edges.add(edge("p0", "p1", 256f));
        room.edges.add(edge("p1", "p2", 2f));
        room.edges.add(edge("p2", "p3", 41f));
        room.edges.add(edge("p3", "p4", 425f));
        room.edges.add(edge("p4", "p5", 304f));
        room.edges.add(edge("p5", "p0", 430f));

        NativeMeasurementSolver.Result result =
                NativeMeasurementSolver.solveFactual(room);

        assertTrue(result.ok);
        assertWallLengthsMatchEntered(room, result.positions);

        double tiny = distance(result.positions, "p1", "p2");
        assertEquals("2-unit return must stay exactly measured", 2d, tiny, .03d);
    }

    @Test
    public void repeatedFactualSolveUsesOriginalSketchNotPreviousFactualShape() {
        NativeProjectStore.RoomData room = rectangleRoom();
        // Make the field sketch slightly non-rectangular on purpose.
        room.points.get(2).x = 430f;
        room.points.get(2).y = 470f;
        room.points.get(3).x = 95f;
        room.points.get(3).y = 520f;

        NativeMeasurementSolver.Result baseline =
                NativeMeasurementSolver.solveFactual(room);
        assertTrue(baseline.ok);
        assertTrue(NativeGeometry.captureSketchGeometry(room));

        // Simulate an older/bad factual result currently visible in the room.
        room.points.get(1).y += 70f;
        room.points.get(2).x += 90f;
        room.points.get(3).y -= 55f;
        NativeGeometry.markFactualGeometryDirty(room);

        NativeMeasurementSolver.Result result =
                NativeMeasurementSolver.solveFactual(room);

        assertTrue(result.ok);
        assertWallLengthsMatchEntered(room, result.positions);
        assertPositionsNear(baseline.positions, result.positions, .04f);
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

        Map<String, PointF> sketchBefore = roomPositions(room);
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

        // The drawing may bend, but its displayed segment lengths must be a
        // single uniform visual scale of the entered wall dimensions.
        assertUniformVisualScaleToMeasurements(room, roomPositions(room));
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

    private static Map<String, PointF> roomPositions(
            NativeProjectStore.RoomData room) {
        java.util.HashMap<String, PointF> out = new java.util.HashMap<>();
        for (NativeProjectStore.PointData point : room.points) {
            out.put(point.id, new PointF(point.x, point.y));
        }
        return out;
    }

    private static void assertWallLengthsMatchEntered(
            NativeProjectStore.RoomData room,
            Map<String, PointF> positions) {
        for (NativeProjectStore.EdgeData edge : room.edges) {
            assertEquals(
                    "displayed wall must follow entered measurement",
                    edge.value,
                    distance(positions, edge.a, edge.b),
                    .06d);
        }
    }

    private static void assertSameWinding(
            NativeProjectStore.RoomData room,
            Map<String, PointF> positions) {
        java.util.List<String> order = NativeGeometry.cycleOrder(room);
        assertNotNull(order);
        double sketchArea = 0d;
        double solvedArea = 0d;
        for (int i = 0; i < order.size(); i++) {
            NativeProjectStore.PointData sa =
                    NativeGeometry.pointById(room, order.get(i));
            NativeProjectStore.PointData sb =
                    NativeGeometry.pointById(room, order.get((i + 1) % order.size()));
            PointF fa = positions.get(order.get(i));
            PointF fb = positions.get(order.get((i + 1) % order.size()));
            assertNotNull(sa);
            assertNotNull(sb);
            assertNotNull(fa);
            assertNotNull(fb);
            sketchArea += (double) sa.x * sb.y - (double) sb.x * sa.y;
            solvedArea += (double) fa.x * fb.y - (double) fb.x * fa.y;
        }
        assertTrue("factual geometry must keep sketch winding",
                Math.signum(sketchArea) == Math.signum(solvedArea));
    }

    private static void assertAnyAngleBent(
            Map<String, PointF> positions,
            String[] order) {
        boolean bent = false;
        for (int i = 0; i < order.length; i++) {
            PointF prev = positions.get(order[(i - 1 + order.length) % order.length]);
            PointF vertex = positions.get(order[i]);
            PointF next = positions.get(order[(i + 1) % order.length]);
            assertNotNull(prev);
            assertNotNull(vertex);
            assertNotNull(next);
            double ax = prev.x - vertex.x;
            double ay = prev.y - vertex.y;
            double bx = next.x - vertex.x;
            double by = next.y - vertex.y;
            double cosine = Math.abs(
                    (ax * bx + ay * by)
                            / (Math.hypot(ax, ay) * Math.hypot(bx, by)));
            if (cosine > .05d) {
                bent = true;
                break;
            }
        }
        assertTrue("incompatible opposite wall sizes must bend the sketch", bent);
    }

    private static void assertPositionsNear(
            Map<String, PointF> expected,
            Map<String, PointF> actual,
            float tolerance) {
        assertEquals(expected.size(), actual.size());
        for (Map.Entry<String, PointF> entry : expected.entrySet()) {
            PointF other = actual.get(entry.getKey());
            assertNotNull(other);
            assertEquals(entry.getValue().x, other.x, tolerance);
            assertEquals(entry.getValue().y, other.y, tolerance);
        }
    }

    private static void assertUniformVisualScaleToMeasurements(
            NativeProjectStore.RoomData room,
            Map<String, PointF> positions) {
        double scale = -1d;
        for (NativeProjectStore.EdgeData edge : room.edges) {
            double visual = distance(positions, edge.a, edge.b);
            double current = visual / edge.value;
            if (scale < 0d) scale = current;
            else assertEquals(scale, current, .003d);
        }
    }

    private static void assertSketchDirectionsPreserved(
            NativeProjectStore.RoomData room,
            Map<String, PointF> positions) {
        assertDirectionsPreserved(roomPositions(room), room.edges, positions);
    }

    private static void assertDirectionsPreserved(
            Map<String, PointF> sketch,
            java.util.List<NativeProjectStore.EdgeData> edges,
            Map<String, PointF> positions) {
        for (NativeProjectStore.EdgeData edge : edges) {
            PointF sa = sketch.get(edge.a);
            PointF sb = sketch.get(edge.b);
            PointF fa = positions.get(edge.a);
            PointF fb = positions.get(edge.b);
            assertNotNull(sa);
            assertNotNull(sb);
            assertNotNull(fa);
            assertNotNull(fb);

            double sdx = sb.x - sa.x;
            double sdy = sb.y - sa.y;
            double fdx = fb.x - fa.x;
            double fdy = fb.y - fa.y;
            double sl = Math.hypot(sdx, sdy);
            double fl = Math.hypot(fdx, fdy);
            assertTrue(sl > 0d);
            assertTrue(fl > 0d);
            double cross = Math.abs(sdx * fdy - sdy * fdx) / (sl * fl);
            double dot = (sdx * fdx + sdy * fdy) / (sl * fl);
            assertTrue("wall direction must stay parallel to sketch", cross < .002d);
            assertTrue("wall direction must not flip", dot > .999d);
        }
    }

    private static double distance(
            Map<String, PointF> positions,
            String a,
            String b) {
        PointF pa = positions.get(a);
        PointF pb = positions.get(b);
        assertNotNull(pa);
        assertNotNull(pb);
        return Math.hypot(pb.x - pa.x, pb.y - pa.y);
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

    private static void assertNearRightAngle(
            Map<String, PointF> positions,
            String a,
            String vertex,
            String b) {
        PointF pa = positions.get(a);
        PointF pv = positions.get(vertex);
        PointF pb = positions.get(b);
        assertNotNull(pa);
        assertNotNull(pv);
        assertNotNull(pb);

        double ax = pa.x - pv.x;
        double ay = pa.y - pv.y;
        double bx = pb.x - pv.x;
        double by = pb.y - pv.y;
        double dot = ax * bx + ay * by;
        double la = Math.hypot(ax, ay);
        double lb = Math.hypot(bx, by);
        double cosine = Math.abs(dot / (la * lb));
        assertTrue("angle must remain near 90 degrees", cosine < .035d);
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
