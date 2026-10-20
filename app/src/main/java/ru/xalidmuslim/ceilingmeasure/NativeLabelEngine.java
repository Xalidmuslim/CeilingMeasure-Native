package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class NativeLabelEngine {
    enum Kind { EDGE, DIAGONAL }

    static final class Placement {
        final Kind kind;
        final String id;
        final String text;
        final float x;
        final float y;
        final float width;
        final float height;
        final float fontDp;
        final float angle;
        final float size;

        Placement(
                Kind kind,
                String id,
                String text,
                float x,
                float y,
                float width,
                float height,
                float fontDp,
                float angle,
                float size) {
            this.kind = kind;
            this.id = id;
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.fontDp = fontDp;
            this.angle = angle;
            this.size = size;
        }
    }

    static final class Box {
        final float x1;
        final float y1;
        final float x2;
        final float y2;

        Box(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        float centerX() { return (x1 + x2) / 2f; }
        float centerY() { return (y1 + y2) / 2f; }
    }

    private static final class WallCandidate {
        final float x;
        final float y;
        final Box box;
        final float fontDp;
        final float penalty;

        WallCandidate(float x, float y, Box box, float fontDp, float penalty) {
            this.x = x;
            this.y = y;
            this.box = box;
            this.fontDp = fontDp;
            this.penalty = penalty;
        }
    }

    private static final class DiagSegment {
        final String id;
        final NativeProjectStore.PointData a;
        final NativeProjectStore.PointData b;

        DiagSegment(
                String id,
                NativeProjectStore.PointData a,
                NativeProjectStore.PointData b) {
            this.id = id;
            this.a = a;
            this.b = b;
        }
    }

    private NativeLabelEngine() {}

    static List<Placement> layout(
            @NonNull NativeProjectStore.RoomData room,
            float scale,
            float density) {
        float safeScale = Math.max(.45f, scale);
        float worldPerDp = Math.max(.1f, density) / safeScale;

        ArrayList<Placement> result = new ArrayList<>();
        ArrayList<Box> wallBoxes = new ArrayList<>();

        ArrayList<NativeProjectStore.EdgeData> walls = new ArrayList<>();
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value > 0f || room.showAutoLetters) walls.add(edge);
        }
        walls.sort(Comparator
                .comparingDouble((NativeProjectStore.EdgeData e) -> -edgeLength(room, e))
                .thenComparing(e -> e.id));

        for (NativeProjectStore.EdgeData edge : walls) {
            String text = edgeText(room, edge);
            if (text.isEmpty()) continue;
            WallCandidate auto = placeWall(room, edge, text, safeScale, worldPerDp, wallBoxes);
            if (auto == null) continue;

            float size = NativeProjectStore.normalizedLabelSize(edge.labelSize);
            boolean absoluteManual = edge.labelManual
                    && edge.labelX != null
                    && edge.labelY != null
                    && Float.isFinite(edge.labelX)
                    && Float.isFinite(edge.labelY);
            float x = absoluteManual ? edge.labelX : auto.x + edge.labelDx;
            float y = absoluteManual ? edge.labelY : auto.y + edge.labelDy;
            float width = auto.box.x2 - auto.box.x1;
            float height = auto.box.y2 - auto.box.y1;
            Box finalBox = centeredBox(x, y, width * size, height * size);
            wallBoxes.add(finalBox);

            result.add(new Placement(
                    Kind.EDGE,
                    edge.id,
                    text,
                    x,
                    y,
                    width,
                    height,
                    auto.fontDp,
                    NativeProjectStore.normalizeAngle(edge.labelAngle),
                    size));
        }

        ArrayList<Box> diagBoxes = new ArrayList<>();
        ArrayList<DiagSegment> segments = new ArrayList<>();
        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, diagonal.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, diagonal.b);
            if (a != null && b != null) segments.add(new DiagSegment(diagonal.id, a, b));
        }

        for (int i = 0; i < room.diagonals.size(); i++) {
            NativeProjectStore.DiagonalData diagonal = room.diagonals.get(i);
            if (diagonal.value <= 0f && !room.showAutoLetters) continue;
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, diagonal.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, diagonal.b);
            if (a == null || b == null) continue;

            String text = diagonalText(room, diagonal);
            if (text.isEmpty()) continue;

            WallCandidate auto = placeDiagonal(
                    room,
                    diagonal,
                    text,
                    i,
                    worldPerDp,
                    diagBoxes,
                    segments);
            if (auto == null) continue;

            float size = NativeProjectStore.normalizedLabelSize(diagonal.labelSize);
            boolean absoluteManual = diagonal.labelManual
                    && diagonal.labelX != null
                    && diagonal.labelY != null
                    && Float.isFinite(diagonal.labelX)
                    && Float.isFinite(diagonal.labelY);
            float x = absoluteManual ? diagonal.labelX : auto.x + diagonal.labelDx;
            float y = absoluteManual ? diagonal.labelY : auto.y + diagonal.labelDy;
            float width = auto.box.x2 - auto.box.x1;
            float height = auto.box.y2 - auto.box.y1;
            Box finalBox = centeredBox(x, y, width * size, height * size);
            diagBoxes.add(finalBox);

            result.add(new Placement(
                    Kind.DIAGONAL,
                    diagonal.id,
                    text,
                    x,
                    y,
                    width,
                    height,
                    15.5f,
                    NativeProjectStore.normalizeAngle(diagonal.labelAngle),
                    size));
        }

        return result;
    }

    @Nullable
    static Placement find(List<Placement> placements, Kind kind, String id) {
        if (placements == null || id == null) return null;
        for (Placement p : placements) {
            if (p.kind == kind && id.equals(p.id)) return p;
        }
        return null;
    }

    @Nullable
    static Placement nearest(
            List<Placement> placements,
            float x,
            float y,
            float maxWorldDistance) {
        Placement best = null;
        float bestDistance = maxWorldDistance;
        for (Placement p : placements) {
            float distance = (float) Math.hypot(x - p.x, y - p.y);
            float halfDiag = (float) Math.hypot(
                    p.width * p.size / 2f,
                    p.height * p.size / 2f);
            float allowed = Math.max(maxWorldDistance, halfDiag);
            if (distance <= allowed && distance < bestDistance + halfDiag) {
                best = p;
                bestDistance = Math.max(0f, distance - halfDiag);
            }
        }
        return best;
    }

    static boolean overlaps(Box a, Box b, float pad) {
        return !(a.x2 + pad < b.x1
                || b.x2 + pad < a.x1
                || a.y2 + pad < b.y1
                || b.y2 + pad < a.y1);
    }

    private static WallCandidate placeWall(
            NativeProjectStore.RoomData room,
            NativeProjectStore.EdgeData edge,
            String text,
            float scale,
            float worldPerDp,
            List<Box> occupied) {
        NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
        NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
        if (a == null || b == null) return null;

        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float length = (float) Math.hypot(dx, dy);
        if (length < 1f) return null;

        float tx = dx / length;
        float ty = dy / length;
        float nx = -ty;
        float ny = tx;
        float mx = (a.x + b.x) / 2f;
        float my = (a.y + b.y) / 2f;

        float probe = 24f * worldPerDp;
        float firstX = mx + nx * probe;
        float firstY = my + ny * probe;
        float secondX = mx - nx * probe;
        float secondY = my - ny * probe;
        List<String> cycle = NativeGeometry.cycleOrder(room);
        if (cycle != null) {
            boolean firstInside = pointInPolygon(room, firstX, firstY, cycle);
            boolean secondInside = pointInPolygon(room, secondX, secondY, cycle);
            if (firstInside != secondInside && firstInside) {
                nx = -nx;
                ny = -ny;
            }
        } else {
            float cx = 0f;
            float cy = 0f;
            if (!room.points.isEmpty()) {
                for (NativeProjectStore.PointData p : room.points) {
                    cx += p.x;
                    cy += p.y;
                }
                cx /= room.points.size();
                cy /= room.points.size();
                float firstDistance = sq(firstX - cx) + sq(firstY - cy);
                float secondDistance = sq(secondX - cx) + sq(secondY - cy);
                if (secondDistance > firstDistance) {
                    nx = -nx;
                    ny = -ny;
                }
            }
        }

        float screenLengthDp = length * scale / Math.max(.1f, worldPerDp * scale);
        float fontDp = screenLengthDp < 28f ? 14.5f : (screenLengthDp < 55f ? 16f : 17.5f);
        int chars = Math.max(1, text.length());
        float width = Math.max(22f * worldPerDp, chars * fontDp * .60f * worldPerDp + 7f * worldPerDp);
        float height = (fontDp + 9f) * worldPerDp;

        float baseOffsetDp = screenLengthDp < 28f ? 40f : (screenLengthDp < 55f ? 34f : 27f);
        float[] outwardDp = new float[]{
                baseOffsetDp,
                baseOffsetDp + 12f,
                baseOffsetDp + 24f,
                baseOffsetDp + 38f,
                baseOffsetDp + 54f
        };

        float maxShift = Math.min(length * .18f, 14f * worldPerDp);
        float[] shifts = maxShift > 1f
                ? new float[]{0f, maxShift, -maxShift, maxShift * .55f, -maxShift * .55f}
                : new float[]{0f};

        WallCandidate best = null;
        for (int oi = 0; oi < outwardDp.length; oi++) {
            float outward = outwardDp[oi] * worldPerDp;
            for (int si = 0; si < shifts.length; si++) {
                float x = mx + nx * outward + tx * shifts[si];
                float y = my + ny * outward + ty * shifts[si];
                Box box = centeredBox(x, y, width, height);
                float penalty = oi * 12f + si * 2f;

                for (Box previous : occupied) {
                    if (overlaps(box, previous, 7f * worldPerDp)) penalty += 1_000_000f;
                    float distance = (float) Math.hypot(
                            x - previous.centerX(),
                            y - previous.centerY());
                    float safe = 44f * worldPerDp;
                    if (distance < safe) penalty += (safe - distance) * 1200f;
                }

                float endpointSafe = (22f + chars * 2f) * worldPerDp;
                for (NativeProjectStore.PointData point : new NativeProjectStore.PointData[]{a, b}) {
                    float distance = (float) Math.hypot(x - point.x, y - point.y);
                    if (distance < endpointSafe) penalty += (endpointSafe - distance) * 900f;
                }

                if (best == null || penalty < best.penalty) {
                    best = new WallCandidate(x, y, box, fontDp, penalty);
                }
                if (penalty < 1f) return best;
            }
        }
        return best;
    }

    private static WallCandidate placeDiagonal(
            NativeProjectStore.RoomData room,
            NativeProjectStore.DiagonalData diagonal,
            String text,
            int index,
            float worldPerDp,
            List<Box> occupied,
            List<DiagSegment> segments) {
        NativeProjectStore.PointData a = NativeGeometry.pointById(room, diagonal.a);
        NativeProjectStore.PointData b = NativeGeometry.pointById(room, diagonal.b);
        if (a == null || b == null) return null;

        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float length = (float) Math.hypot(dx, dy);
        if (length < 1f) return null;
        float tx = dx / length;
        float ty = dy / length;

        float width = Math.max(70f * worldPerDp, text.length() * 11.4f * worldPerDp);
        float height = 30f * worldPerDp;
        Float fan = fanPreferredT(room, diagonal);
        float preferred = fan == null ? .5f : fan;
        // Dimension labels belong on the segment. Only move ALONG the line.
        float[] candidates = new float[]{
                preferred, .5f, preferred - .10f, preferred + .10f,
                .42f, .35f, .28f, .22f, .58f, .65f, .72f, .78f, .18f, .82f
        };
        List<String> contour = NativeGeometry.cycleOrder(room);
        float halfLabelLength = Math.min(width * .45f, length * .23f);
        float safeFromWalls = 35f * worldPerDp;
        float safeFromCrossings = 25f * worldPerDp;

        WallCandidate best = null;
        for (float candidate : candidates) {
            float t = Math.max(.16f, Math.min(.84f, candidate));
            float x = a.x + dx * t;
            float y = a.y + dy * t;
            Box box = centeredBox(x, y, width, height);
            float penalty = Math.abs(t - preferred) * 24f;
            // When similar space is available, favour the earlier section.
            if (t > .5f) penalty += (t - .5f) * 5f;

            for (Box previous : occupied) {
                if (overlaps(box, previous, 8f * worldPerDp)) penalty += 1_000_000f;
                float distance = (float) Math.hypot(
                        x - previous.centerX(), y - previous.centerY());
                float safe = 48f * worldPerDp;
                if (distance < safe) penalty += (safe - distance) * 140f;
            }

            // Score three samples over the text footprint so the number
            // cannot sit in a narrow bottleneck while its centre looks clear.
            for (int sample = -1; sample <= 1; sample++) {
                float sx = x + tx * halfLabelLength * sample;
                float sy = y + ty * halfLabelLength * sample;
                if (contour != null && !pointInPolygon(room, sx, sy, contour)) {
                    penalty += 100_000f;
                }
                float wallClearance = Float.POSITIVE_INFINITY;
                for (NativeProjectStore.EdgeData edge : room.edges) {
                    NativeProjectStore.PointData pa = NativeGeometry.pointById(room, edge.a);
                    NativeProjectStore.PointData pb = NativeGeometry.pointById(room, edge.b);
                    if (pa == null || pb == null) continue;
                    wallClearance = Math.min(wallClearance,
                            distancePointToSegment(sx, sy, pa.x, pa.y, pb.x, pb.y));
                }
                if (wallClearance < safeFromWalls) {
                    penalty += (safeFromWalls - wallClearance) * 65f;
                }
            }

            for (DiagSegment segment : segments) {
                if (segment.id.equals(diagonal.id)) continue;
                float distance = distancePointToSegment(x, y,
                        segment.a.x, segment.a.y, segment.b.x, segment.b.y);
                if (distance < safeFromCrossings) {
                    penalty += (safeFromCrossings - distance) * 210f;
                }
            }

            if (best == null || penalty < best.penalty) {
                best = new WallCandidate(x, y, box, 15.5f, penalty);
            }
        }
        return best;
    }

    private static String edgeText(
            NativeProjectStore.RoomData room,
            NativeProjectStore.EdgeData edge) {
        String label = pointLabel(room, edge.a) + pointLabel(room, edge.b);
        if (room.showAutoLetters) {
            return edge.value > 0f ? label + " · " + format(edge.value) : label;
        }
        return edge.value > 0f ? format(edge.value) : "";
    }

    private static String diagonalText(
            NativeProjectStore.RoomData room,
            NativeProjectStore.DiagonalData diagonal) {
        String label = pointLabel(room, diagonal.a) + pointLabel(room, diagonal.b);
        if (room.showAutoLetters) {
            return diagonal.value > 0f ? label + " · " + format(diagonal.value) : label;
        }
        return diagonal.value > 0f ? format(diagonal.value) : "";
    }

    private static String pointLabel(NativeProjectStore.RoomData room, String pointId) {
        NativeProjectStore.PointData point = NativeGeometry.pointById(room, pointId);
        if (point != null && point.customLabel != null && !point.customLabel.isEmpty()) {
            return point.customLabel;
        }
        int index = NativeGeometry.pointIndex(room, pointId);
        if (index >= 0 && index < 26) return String.valueOf((char) ('A' + index));
        return index >= 0 ? String.valueOf(index + 1) : "";
    }

    private static double edgeLength(
            NativeProjectStore.RoomData room,
            NativeProjectStore.EdgeData edge) {
        NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
        NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
        return a == null || b == null ? 0d : Math.hypot(b.x - a.x, b.y - a.y);
    }

    @Nullable
    private static Float fanPreferredT(
            NativeProjectStore.RoomData room,
            NativeProjectStore.DiagonalData diagonal) {
        int degreeA = diagonalDegree(room, diagonal.a);
        int degreeB = diagonalDegree(room, diagonal.b);
        String hub = Math.max(degreeA, degreeB) >= 2
                ? (degreeA >= degreeB ? diagonal.a : diagonal.b)
                : null;
        if (hub == null) return null;

        NativeProjectStore.PointData hubPoint = NativeGeometry.pointById(room, hub);
        if (hubPoint == null) return null;

        ArrayList<NativeProjectStore.DiagonalData> group = new ArrayList<>();
        for (NativeProjectStore.DiagonalData d : room.diagonals) {
            if (d.a.equals(hub) || d.b.equals(hub)) group.add(d);
        }
        group.sort(Comparator.comparingDouble(d -> {
            String otherId = d.a.equals(hub) ? d.b : d.a;
            NativeProjectStore.PointData other = NativeGeometry.pointById(room, otherId);
            return other == null ? 0d : Math.atan2(other.y - hubPoint.y, other.x - hubPoint.x);
        }));

        int rank = -1;
        for (int i = 0; i < group.size(); i++) {
            if (group.get(i).id.equals(diagonal.id)) {
                rank = i;
                break;
            }
        }
        if (rank < 0) return null;

        float fromHub = group.size() <= 1
                ? .5f
                : .24f + .50f * (rank / (float) Math.max(1, group.size() - 1));
        return diagonal.a.equals(hub) ? fromHub : 1f - fromHub;
    }

    private static int diagonalDegree(NativeProjectStore.RoomData room, String id) {
        int count = 0;
        for (NativeProjectStore.DiagonalData d : room.diagonals) {
            if (d.a.equals(id) || d.b.equals(id)) count++;
        }
        return count;
    }

    private static boolean pointInPolygon(
            NativeProjectStore.RoomData room,
            float x,
            float y,
            List<String> order) {
        boolean inside = false;
        for (int i = 0, j = order.size() - 1; i < order.size(); j = i++) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, order.get(i));
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, order.get(j));
            if (a == null || b == null) continue;
            boolean crosses = ((a.y > y) != (b.y > y))
                    && x < (b.x - a.x) * (y - a.y)
                    / ((b.y - a.y) == 0f ? 1e-9f : (b.y - a.y)) + a.x;
            if (crosses) inside = !inside;
        }
        return inside;
    }

    private static Box centeredBox(float x, float y, float width, float height) {
        return new Box(
                x - width / 2f,
                y - height / 2f,
                x + width / 2f,
                y + height / 2f);
    }

    private static float distancePointToSegment(
            float px, float py,
            float ax, float ay,
            float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float length2 = dx * dx + dy * dy;
        if (length2 < 1e-9f) return (float) Math.hypot(px - ax, py - ay);
        float t = ((px - ax) * dx + (py - ay) * dy) / length2;
        t = Math.max(0f, Math.min(1f, t));
        float cx = ax + dx * t;
        float cy = ay + dy * t;
        return (float) Math.hypot(px - cx, py - cy);
    }

    private static float sq(float value) {
        return value * value;
    }

    private static String format(float value) {
        return Math.abs(value - Math.round(value)) < .01f
                ? String.valueOf(Math.round(value))
                : String.format(java.util.Locale.US, "%.1f", value);
    }
}
