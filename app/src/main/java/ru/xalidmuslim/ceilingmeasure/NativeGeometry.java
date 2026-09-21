package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NativeGeometry {
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

    static String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "~" + b : b + "~" + a;
    }
}
