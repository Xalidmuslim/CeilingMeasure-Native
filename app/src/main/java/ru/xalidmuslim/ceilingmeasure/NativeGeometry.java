package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NativeGeometry {
    private static final String SKETCH_GEOMETRY_KEY = "_nativeSketchGeometryV1";
    private static final String GEOMETRY_MODE_KEY = "_nativeGeometryMode";
    private static final String GEOMETRY_MODE_SKETCH = "sketch";
    private static final String GEOMETRY_MODE_ACTUAL = "actual";
    private static final String GEOMETRY_MODE_ACTUAL_DIRTY = "actual_dirty";
    private static final String GEOMETRY_MODE_MEASUREMENTS = "measurements";
    private static final String GEOMETRY_MODE_MEASUREMENTS_DIRTY = "measurements_dirty";
    private static final String DIAGONAL_SOURCE_KEY = "_nativeMeasurementSource";
    private static final String DIAGONAL_SOURCE_MEASURED = "measured";
    private static final String DIAGONAL_SOURCE_AUTO = "auto";

    private NativeGeometry() {}

    static void sanitize(NativeProjectStore.RoomData room) {
        Set<String> pointIds = new HashSet<>();
        for (NativeProjectStore.PointData p : room.points) {
            if (p.id == null || p.id.isEmpty() || pointIds.contains(p.id)) {
                p.id = NativeProjectStore.newId("p");
            }
            pointIds.add(p.id);
        }

        Set<String> edgePairs = new HashSet<>();
        room.edges.removeIf(e -> {
            if (e.id == null || e.id.isEmpty()) e.id = NativeProjectStore.newId("e");
            if (!pointIds.contains(e.a) || !pointIds.contains(e.b) || e.a.equals(e.b)) return true;
            String key = pairKey(e.a, e.b);
            if (edgePairs.contains(key)) return true;
            edgePairs.add(key);
            return false;
        });

        Set<String> diagonalPairs = new HashSet<>();
        room.diagonals.removeIf(d -> {
            if (d.id == null || d.id.isEmpty()) d.id = NativeProjectStore.newId("d");
            if (!pointIds.contains(d.a) || !pointIds.contains(d.b) || d.a.equals(d.b)) return true;
            String key = pairKey(d.a, d.b);
            if (diagonalPairs.contains(key)) return true;
            diagonalPairs.add(key);
            return false;
        });
    }

    @Nullable
    static NativeProjectStore.PointData pointById(NativeProjectStore.RoomData room, String id) {
        return NativeProjectStore.pointById(room, id);
    }

    static int pointIndex(NativeProjectStore.RoomData room, String id) {
        for (int i = 0; i < room.points.size(); i++) {
            if (room.points.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    static int vertexDegree(NativeProjectStore.RoomData room, String id) {
        int degree = 0;
        for (NativeProjectStore.EdgeData e : room.edges) {
            if (e.a.equals(id) || e.b.equals(id)) degree++;
        }
        return degree;
    }

    static List<NativeProjectStore.EdgeData> edgesAt(NativeProjectStore.RoomData room, String id) {
        ArrayList<NativeProjectStore.EdgeData> result = new ArrayList<>();
        for (NativeProjectStore.EdgeData e : room.edges) {
            if (e.a.equals(id) || e.b.equals(id)) result.add(e);
        }
        return result;
    }

    @Nullable
    static NativeProjectStore.EdgeData edgeBetween(
            NativeProjectStore.RoomData room,
            String a,
            String b) {
        for (NativeProjectStore.EdgeData e : room.edges) {
            if ((e.a.equals(a) && e.b.equals(b)) || (e.a.equals(b) && e.b.equals(a))) return e;
        }
        return null;
    }

    @Nullable
    static NativeProjectStore.DiagonalData diagonalBetween(
            NativeProjectStore.RoomData room,
            String a,
            String b) {
        for (NativeProjectStore.DiagonalData d : room.diagonals) {
            if ((d.a.equals(a) && d.b.equals(b)) || (d.a.equals(b) && d.b.equals(a))) return d;
        }
        return null;
    }

    @Nullable
    static List<String> cycleOrder(NativeProjectStore.RoomData room) {
        LinkedHashSet<String> used = new LinkedHashSet<>();
        for (NativeProjectStore.EdgeData e : room.edges) {
            used.add(e.a);
            used.add(e.b);
        }
        if (used.size() < 3 || room.edges.size() != used.size()) return null;

        Map<String, ArrayList<String>> adjacency = new HashMap<>();
        for (String id : used) adjacency.put(id, new ArrayList<>());
        for (NativeProjectStore.EdgeData e : room.edges) {
            adjacency.get(e.a).add(e.b);
            adjacency.get(e.b).add(e.a);
        }
        for (ArrayList<String> list : adjacency.values()) if (list.size() != 2) return null;

        String start = used.iterator().next();
        ArrayList<String> order = new ArrayList<>();
        order.add(start);
        String previous = null;
        String current = start;

        for (int guard = 0; guard < used.size() + 2; guard++) {
            ArrayList<String> nexts = adjacency.get(current);
            String next = nexts.get(0).equals(previous) ? nexts.get(1) : nexts.get(0);
            if (next.equals(start)) break;
            if (order.contains(next)) return null;
            order.add(next);
            previous = current;
            current = next;
        }

        return order.size() == used.size() ? order : null;
    }

    static boolean isClosed(NativeProjectStore.RoomData room) {
        return cycleOrder(room) != null;
    }

    static List<NativeProjectStore.EdgeData> orderedEdges(NativeProjectStore.RoomData room) {
        ArrayList<NativeProjectStore.EdgeData> ordered = new ArrayList<>();
        List<String> cycle = cycleOrder(room);
        if (cycle != null) {
            for (int i = 0; i < cycle.size(); i++) {
                NativeProjectStore.EdgeData edge = edgeBetween(
                        room,
                        cycle.get(i),
                        cycle.get((i + 1) % cycle.size()));
                if (edge != null) ordered.add(edge);
            }
            if (!ordered.isEmpty() && !room.edges.isEmpty()) {
                String firstId = room.edges.get(0).id;
                int pivot = -1;
                for (int i = 0; i < ordered.size(); i++) {
                    if (ordered.get(i).id.equals(firstId)) {
                        pivot = i;
                        break;
                    }
                }
                if (pivot > 0) {
                    ArrayList<NativeProjectStore.EdgeData> rotated = new ArrayList<>();
                    rotated.addAll(ordered.subList(pivot, ordered.size()));
                    rotated.addAll(ordered.subList(0, pivot));
                    return rotated;
                }
            }
            return ordered;
        }

        if (room.edges.isEmpty()) return ordered;
        Map<String, ArrayList<NativeProjectStore.EdgeData>> adjacency = new HashMap<>();
        for (NativeProjectStore.EdgeData e : room.edges) {
            adjacency.computeIfAbsent(e.a, k -> new ArrayList<>()).add(e);
            adjacency.computeIfAbsent(e.b, k -> new ArrayList<>()).add(e);
        }

        String preferred = room.edges.get(0).a;
        String endpoint = null;
        if (adjacency.get(preferred) != null && adjacency.get(preferred).size() == 1) endpoint = preferred;
        if (endpoint == null) {
            for (NativeProjectStore.PointData p : room.points) {
                ArrayList<NativeProjectStore.EdgeData> list = adjacency.get(p.id);
                if (list != null && list.size() == 1) {
                    endpoint = p.id;
                    break;
                }
            }
        }
        if (endpoint == null) endpoint = room.edges.get(0).a;

        Set<String> seen = new HashSet<>();
        String vertex = endpoint;
        while (vertex != null) {
            NativeProjectStore.EdgeData next = null;
            List<NativeProjectStore.EdgeData> list = adjacency.get(vertex);
            if (list != null) {
                for (NativeProjectStore.EdgeData e : list) {
                    if (!seen.contains(e.id)) {
                        next = e;
                        break;
                    }
                }
            }
            if (next == null) break;
            ordered.add(next);
            seen.add(next.id);
            vertex = next.a.equals(vertex) ? next.b : next.a;
        }
        for (NativeProjectStore.EdgeData e : room.edges) if (!seen.contains(e.id)) ordered.add(e);
        return ordered;
    }

    static boolean repairNearlyClosedContour(NativeProjectStore.RoomData room, float maxWorld) {
        if (room.edges.size() < 3 || isClosed(room)) return false;

        Set<String> used = new LinkedHashSet<>();
        Map<String, ArrayList<String>> adjacency = new HashMap<>();
        for (NativeProjectStore.EdgeData e : room.edges) {
            used.add(e.a);
            used.add(e.b);
            adjacency.computeIfAbsent(e.a, k -> new ArrayList<>()).add(e.b);
            adjacency.computeIfAbsent(e.b, k -> new ArrayList<>()).add(e.a);
        }
        if (used.size() != room.edges.size() + 1) return false;

        ArrayList<String> ends = new ArrayList<>();
        for (String id : used) {
            int degree = adjacency.get(id) == null ? 0 : adjacency.get(id).size();
            if (degree == 1) ends.add(id);
            else if (degree < 1 || degree > 2) return false;
        }
        if (ends.size() != 2) return false;

        NativeProjectStore.PointData p1 = pointById(room, ends.get(0));
        NativeProjectStore.PointData p2 = pointById(room, ends.get(1));
        if (p1 == null || p2 == null) return false;
        if (Math.hypot(p2.x - p1.x, p2.y - p1.y) > maxWorld) return false;

        int i1 = pointIndex(room, p1.id);
        int i2 = pointIndex(room, p2.id);
        NativeProjectStore.PointData keep = i1 <= i2 ? p1 : p2;
        NativeProjectStore.PointData drop = keep == p1 ? p2 : p1;

        for (NativeProjectStore.EdgeData e : room.edges) {
            if (e.a.equals(drop.id)) e.a = keep.id;
            if (e.b.equals(drop.id)) e.b = keep.id;
        }
        for (NativeProjectStore.DiagonalData d : room.diagonals) {
            if (d.a.equals(drop.id)) d.a = keep.id;
            if (d.b.equals(drop.id)) d.b = keep.id;
        }
        room.points.remove(drop);
        sanitize(room);
        return isClosed(room);
    }

    @Nullable
    static NativeProjectStore.EdgeData straightContinuationEdge(
            NativeProjectStore.RoomData room,
            String startId,
            float endX,
            float endY) {
        List<NativeProjectStore.EdgeData> adjacent = edgesAt(room, startId);
        if (adjacent.size() != 1) return null;

        NativeProjectStore.EdgeData edge = adjacent.get(0);
        String otherId = edge.a.equals(startId) ? edge.b : edge.a;
        NativeProjectStore.PointData start = pointById(room, startId);
        NativeProjectStore.PointData other = pointById(room, otherId);
        if (start == null || other == null) return null;

        float v1x = start.x - other.x;
        float v1y = start.y - other.y;
        float v2x = endX - start.x;
        float v2y = endY - start.y;
        float l1 = (float) Math.hypot(v1x, v1y);
        float l2 = (float) Math.hypot(v2x, v2y);
        if (l1 < 1f || l2 < 1f) return null;

        float dot = (v1x * v2x + v1y * v2y) / (l1 * l2);
        float cross = Math.abs(v1x * v2y - v1y * v2x) / (l1 * l2);
        return dot > .992f && cross < .125f ? edge : null;
    }

    static boolean extendStraightWall(
            NativeProjectStore.RoomData room,
            String startId,
            float endX,
            float endY,
            @Nullable NativeProjectStore.PointData target) {
        NativeProjectStore.EdgeData edge =
                straightContinuationEdge(room, startId, endX, endY);
        if (edge == null) return false;

        NativeProjectStore.PointData start = pointById(room, startId);
        if (start == null) return false;

        if (target != null && !target.id.equals(startId)) {
            if (edge.a.equals(startId)) edge.a = target.id;
            else if (edge.b.equals(startId)) edge.b = target.id;
            else return false;

            room.diagonals.removeIf(d -> d.a.equals(startId) || d.b.equals(startId));
            room.points.removeIf(p -> p.id.equals(startId));
        } else {
            start.x = endX;
            start.y = endY;
        }

        // As in the web reference: changing a wall length invalidates its old size.
        edge.value = 0f;
        sanitize(room);
        return true;
    }

    @Nullable
    private static OpenChain openChainOrder(NativeProjectStore.RoomData room) {
        if (room == null || room.edges.size() < 2) return null;

        LinkedHashSet<String> used = new LinkedHashSet<>();
        Map<String, ArrayList<Step>> adjacency = new HashMap<>();
        for (NativeProjectStore.EdgeData edge : room.edges) {
            used.add(edge.a);
            used.add(edge.b);
            adjacency.computeIfAbsent(edge.a, k -> new ArrayList<>())
                    .add(new Step(edge.b, edge));
            adjacency.computeIfAbsent(edge.b, k -> new ArrayList<>())
                    .add(new Step(edge.a, edge));
        }
        if (used.size() != room.edges.size() + 1) return null;

        ArrayList<String> ends = new ArrayList<>();
        for (String id : used) {
            List<Step> list = adjacency.get(id);
            int size = list == null ? 0 : list.size();
            if (size == 1) ends.add(id);
            else if (size < 1 || size > 2) return null;
        }
        if (ends.size() != 2) return null;

        OpenChain first = buildOpenChain(ends.get(0), used.size(), adjacency);
        OpenChain second = buildOpenChain(ends.get(1), used.size(), adjacency);
        if (first == null || second == null) return null;

        String firstStart = room.edges.get(0).a;
        if (first.vertices.get(0).equals(firstStart)) return first;
        if (second.vertices.get(0).equals(firstStart)) return second;
        return first;
    }

    @Nullable
    private static OpenChain buildOpenChain(
            String start,
            int vertexCount,
            Map<String, ArrayList<Step>> adjacency) {
        ArrayList<String> vertices = new ArrayList<>();
        ArrayList<NativeProjectStore.EdgeData> edges = new ArrayList<>();
        vertices.add(start);

        String previous = null;
        String current = start;
        for (int guard = 0; guard < vertexCount + 2; guard++) {
            List<Step> raw = adjacency.get(current);
            if (raw == null) break;
            ArrayList<Step> nexts = new ArrayList<>();
            for (Step step : raw) if (!step.id.equals(previous)) nexts.add(step);
            if (nexts.isEmpty()) break;
            if (nexts.size() != 1) return null;

            Step next = nexts.get(0);
            edges.add(next.edge);
            previous = current;
            current = next.id;
            vertices.add(current);
            if (vertices.size() > vertexCount + 1) return null;
        }
        return vertices.size() == vertexCount ? new OpenChain(vertices, edges) : null;
    }

    static boolean repairOvershotClosedContour(
            NativeProjectStore.RoomData room,
            float maxWorld) {
        OpenChain chain = openChainOrder(room);
        if (chain == null || chain.edges.size() < 3) return false;

        String startId = chain.vertices.get(0);
        NativeProjectStore.PointData start = pointById(room, startId);
        if (start == null) return false;

        for (int i = 2; i < chain.edges.size(); i++) {
            NativeProjectStore.EdgeData edge = chain.edges.get(i);
            String fromId = chain.vertices.get(i);
            String toId = chain.vertices.get(i + 1);
            NativeProjectStore.PointData a = pointById(room, fromId);
            NativeProjectStore.PointData b = pointById(room, toId);
            if (a == null || b == null) continue;

            Projection hit = projection(start.x, start.y, a.x, a.y, b.x, b.y);
            if (hit == null) continue;
            if (hit.t < .10f || hit.t > 1.08f || hit.distance > maxWorld) continue;

            boolean measuredTail = false;
            for (int j = i + 1; j < chain.edges.size(); j++) {
                if (chain.edges.get(j).value > 0f) {
                    measuredTail = true;
                    break;
                }
            }
            if (measuredTail) continue;

            if (edge.a.equals(toId)) edge.a = startId;
            else if (edge.b.equals(toId)) edge.b = startId;
            else continue;

            Set<String> keepEdges = new HashSet<>();
            for (int j = 0; j <= i; j++) keepEdges.add(chain.edges.get(j).id);
            room.edges.removeIf(e -> !keepEdges.contains(e.id));

            Set<String> keepPoints = new HashSet<>();
            for (NativeProjectStore.EdgeData kept : room.edges) {
                keepPoints.add(kept.a);
                keepPoints.add(kept.b);
            }
            room.points.removeIf(p -> !keepPoints.contains(p.id));
            room.diagonals.removeIf(
                    d -> !keepPoints.contains(d.a) || !keepPoints.contains(d.b));
            sanitize(room);
            return isClosed(room);
        }
        return false;
    }

    @Nullable
    private static Projection projection(
            float px,
            float py,
            float ax,
            float ay,
            float bx,
            float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float length2 = dx * dx + dy * dy;
        if (length2 < 1e-9f) return null;
        float t = ((px - ax) * dx + (py - ay) * dy) / length2;
        float x = ax + t * dx;
        float y = ay + t * dy;
        return new Projection(t, (float) Math.hypot(px - x, py - y));
    }

    private static final class Projection {
        final float t;
        final float distance;

        Projection(float t, float distance) {
            this.t = t;
            this.distance = distance;
        }
    }

    private static final class Step {
        final String id;
        final NativeProjectStore.EdgeData edge;

        Step(String id, NativeProjectStore.EdgeData edge) {
            this.id = id;
            this.edge = edge;
        }
    }

    private static final class OpenChain {
        final ArrayList<String> vertices;
        final ArrayList<NativeProjectStore.EdgeData> edges;

        OpenChain(
                ArrayList<String> vertices,
                ArrayList<NativeProjectStore.EdgeData> edges) {
            this.vertices = vertices;
            this.edges = edges;
        }
    }


    static boolean hasSketchGeometryBackup(@Nullable NativeProjectStore.RoomData room) {
        return room != null && room.raw.optJSONArray(SKETCH_GEOMETRY_KEY) != null;
    }

    static boolean isFactualGeometry(@Nullable NativeProjectStore.RoomData room) {
        return room != null
                && GEOMETRY_MODE_ACTUAL.equals(room.raw.optString(GEOMETRY_MODE_KEY, ""));
    }

    static boolean isFactualGeometryDirty(@Nullable NativeProjectStore.RoomData room) {
        return room != null
                && GEOMETRY_MODE_ACTUAL_DIRTY.equals(
                        room.raw.optString(GEOMETRY_MODE_KEY, ""));
    }

    static boolean isMeasurementsGeometry(@Nullable NativeProjectStore.RoomData room) {
        return room != null
                && GEOMETRY_MODE_MEASUREMENTS.equals(
                        room.raw.optString(GEOMETRY_MODE_KEY, ""));
    }

    static boolean isMeasurementsGeometryDirty(@Nullable NativeProjectStore.RoomData room) {
        return room != null
                && GEOMETRY_MODE_MEASUREMENTS_DIRTY.equals(
                        room.raw.optString(GEOMETRY_MODE_KEY, ""));
    }

    static Map<String, float[]> sketchCoordinates(
            @Nullable NativeProjectStore.RoomData room) {
        HashMap<String, float[]> result = new HashMap<>();
        if (room == null) return result;

        JSONArray stored = room.raw.optJSONArray(SKETCH_GEOMETRY_KEY);
        if (stored != null && stored.length() > 0) {
            for (int i = 0; i < stored.length(); i++) {
                JSONObject item = stored.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                float x = (float) item.optDouble("x", Float.NaN);
                float y = (float) item.optDouble("y", Float.NaN);
                if (!id.isEmpty() && Float.isFinite(x) && Float.isFinite(y)) {
                    result.put(id, new float[]{x, y});
                }
            }
            boolean complete = result.size() == room.points.size();
            if (complete) {
                for (NativeProjectStore.PointData point : room.points) {
                    if (!result.containsKey(point.id)) {
                        complete = false;
                        break;
                    }
                }
            }
            if (complete) return result;
            result.clear();
        }

        for (NativeProjectStore.PointData point : room.points) {
            result.put(point.id, new float[]{point.x, point.y});
        }
        return result;
    }

    static boolean captureSketchGeometry(@Nullable NativeProjectStore.RoomData room) {
        if (room == null || room.points.isEmpty()) return false;
        if (hasSketchGeometryBackup(room)) return true;

        JSONArray points = new JSONArray();
        try {
            for (NativeProjectStore.PointData point : room.points) {
                JSONObject item = new JSONObject();
                item.put("id", point.id);
                item.put("x", point.x);
                item.put("y", point.y);
                points.put(item);
            }
            room.raw.put(SKETCH_GEOMETRY_KEY, points);
            if (!room.raw.has(GEOMETRY_MODE_KEY)) {
                room.raw.put(GEOMETRY_MODE_KEY, GEOMETRY_MODE_SKETCH);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean restoreSketchGeometry(@Nullable NativeProjectStore.RoomData room) {
        if (room == null) return false;
        JSONArray stored = room.raw.optJSONArray(SKETCH_GEOMETRY_KEY);
        if (stored == null || stored.length() == 0) return false;

        HashMap<String, float[]> saved = new HashMap<>();
        for (int i = 0; i < stored.length(); i++) {
            JSONObject item = stored.optJSONObject(i);
            if (item == null) continue;
            String id = item.optString("id", "");
            if (id.isEmpty()) continue;
            saved.put(id, new float[]{
                    (float) item.optDouble("x", Float.NaN),
                    (float) item.optDouble("y", Float.NaN)});
        }

        // Never partially restore a different topology.
        for (NativeProjectStore.PointData point : room.points) {
            float[] xy = saved.get(point.id);
            if (xy == null || !Float.isFinite(xy[0]) || !Float.isFinite(xy[1])) {
                return false;
            }
        }
        if (saved.size() != room.points.size()) return false;

        for (NativeProjectStore.PointData point : room.points) {
            float[] xy = saved.get(point.id);
            point.x = xy[0];
            point.y = xy[1];
        }

        try {
            room.raw.put(GEOMETRY_MODE_KEY, GEOMETRY_MODE_SKETCH);
            room.raw.remove(SKETCH_GEOMETRY_KEY);
        } catch (Exception ignored) {
        }
        return true;
    }

    static void markFactualGeometry(@Nullable NativeProjectStore.RoomData room) {
        if (room == null) return;
        try {
            room.raw.put(GEOMETRY_MODE_KEY, GEOMETRY_MODE_ACTUAL);
        } catch (Exception ignored) {
        }
    }

    static void markMeasurementsGeometry(@Nullable NativeProjectStore.RoomData room) {
        if (room == null) return;
        try {
            room.raw.put(GEOMETRY_MODE_KEY, GEOMETRY_MODE_MEASUREMENTS);
        } catch (Exception ignored) {
        }
    }

    static void markFactualGeometryDirty(@Nullable NativeProjectStore.RoomData room) {
        if (room == null || !hasSketchGeometryBackup(room)) return;
        String mode = room.raw.optString(GEOMETRY_MODE_KEY, "");
        try {
            if (GEOMETRY_MODE_ACTUAL.equals(mode)
                    || GEOMETRY_MODE_ACTUAL_DIRTY.equals(mode)) {
                room.raw.put(GEOMETRY_MODE_KEY, GEOMETRY_MODE_ACTUAL_DIRTY);
            } else if (GEOMETRY_MODE_MEASUREMENTS.equals(mode)
                    || GEOMETRY_MODE_MEASUREMENTS_DIRTY.equals(mode)) {
                room.raw.put(GEOMETRY_MODE_KEY, GEOMETRY_MODE_MEASUREMENTS_DIRTY);
            }
        } catch (Exception ignored) {
        }
    }

    static void discardFactualGeometrySession(@Nullable NativeProjectStore.RoomData room) {
        if (room == null) return;
        try {
            room.raw.put(GEOMETRY_MODE_KEY, GEOMETRY_MODE_SKETCH);
            room.raw.remove(SKETCH_GEOMETRY_KEY);
        } catch (Exception ignored) {
        }
    }

    static void markDiagonalMeasured(@Nullable NativeProjectStore.DiagonalData diagonal) {
        if (diagonal == null) return;
        try {
            diagonal.raw.put(DIAGONAL_SOURCE_KEY, DIAGONAL_SOURCE_MEASURED);
        } catch (Exception ignored) {
        }
    }

    static void markDiagonalAuto(@Nullable NativeProjectStore.DiagonalData diagonal) {
        if (diagonal == null) return;
        try {
            diagonal.raw.put(DIAGONAL_SOURCE_KEY, DIAGONAL_SOURCE_AUTO);
        } catch (Exception ignored) {
        }
    }

    static boolean isMeasuredDiagonal(@Nullable NativeProjectStore.DiagonalData diagonal) {
        if (diagonal == null || diagonal.value <= 0f) return false;
        // Existing v184/legacy values have no provenance marker. They were user-entered,
        // so they remain factual unless this native app explicitly marked them as auto.
        return !DIAGONAL_SOURCE_AUTO.equals(
                diagonal.raw.optString(DIAGONAL_SOURCE_KEY, DIAGONAL_SOURCE_MEASURED));
    }

    static String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "~" + b : b + "~" + a;
    }
}
