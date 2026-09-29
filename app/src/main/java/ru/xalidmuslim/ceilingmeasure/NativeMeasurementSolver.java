package ru.xalidmuslim.ceilingmeasure;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class NativeMeasurementSolver {
    // Web reference v184: project scale is 100 units = 1 metre.
    // Field measurement tolerance is 10 units = 0.10 m.
    static final float FIELD_MEASURE_TOL = 10f;
    // Factual geometry must not silently absorb the legacy 0.10 m working tolerance.
    // One project unit is 1 cm, so > 1 cm residual is reported as a measurement conflict.
    static final float FACTUAL_CONFLICT_TOL = 1f;

    private NativeMeasurementSolver() {}

    static final class Result {
        final boolean ok;
        final String message;
        final Map<String, PointF> positions;
        final float maxError;
        final boolean uniquelySupported;
        final boolean closureAdjusted;

        Result(
                boolean ok,
                String message,
                Map<String, PointF> positions,
                float maxError,
                boolean uniquelySupported,
                boolean closureAdjusted) {
            this.ok = ok;
            this.message = message;
            this.positions = positions;
            this.maxError = maxError;
            this.uniquelySupported = uniquelySupported;
            this.closureAdjusted = closureAdjusted;
        }

        static Result fail(String message) {
            return new Result(false, message, new HashMap<>(), 0f, false, false);
        }
    }

    private static final class Constraint {
        final String a;
        final String b;
        final float target;

        Constraint(String a, String b, float target) {
            this.a = a;
            this.b = b;
            this.target = target;
        }
    }

    private static final class DirectionGuide {
        final String a;
        final String b;
        final float target;
        final float ux;
        final float uy;

        DirectionGuide(String a, String b, float target, float ux, float uy) {
            this.a = a;
            this.b = b;
            this.target = target;
            this.ux = ux;
            this.uy = uy;
        }
    }

    private static final class OrthoSegment {
        final String a;
        final String b;
        final boolean xAxis;
        final int sign;
        final float target;
        float adjusted;

        OrthoSegment(
                String a,
                String b,
                boolean xAxis,
                int sign,
                float target) {
            this.a = a;
            this.b = b;
            this.xAxis = xAxis;
            this.sign = sign;
            this.target = target;
            this.adjusted = target;
        }
    }

    @NonNull
    static Result solveFree(@NonNull NativeProjectStore.RoomData room) {
        List<String> order = NativeGeometry.cycleOrder(room);
        if (order == null) {
            return Result.fail("Сначала стены должны образовать один замкнутый контур.");
        }

        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value <= 0f) {
                return Result.fail("Сначала введи размеры всех стен.");
            }
        }

        ArrayList<Constraint> constraints = constraints(room);
        if (constraints.isEmpty()) return Result.fail("Нет введённых размеров.");

        ArrayList<Float> ratios = new ArrayList<>();
        for (NativeProjectStore.EdgeData edge : room.edges) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
            if (a == null || b == null || edge.value <= 0) continue;
            float drawn = (float) Math.hypot(b.x - a.x, b.y - a.y);
            if (drawn > 1f) ratios.add(edge.value / drawn);
        }
        float sketchToUnits = median(ratios);
        if (!(sketchToUnits > 0f)) sketchToUnits = 1f;

        float cx = 0f;
        float cy = 0f;
        for (String id : order) {
            NativeProjectStore.PointData p = NativeGeometry.pointById(room, id);
            if (p == null) return Result.fail("Повреждена геометрия комнаты.");
            cx += p.x;
            cy += p.y;
        }
        cx /= order.size();
        cy /= order.size();

        Map<String, PointF> raw = new HashMap<>();
        for (String id : order) {
            NativeProjectStore.PointData p = NativeGeometry.pointById(room, id);
            raw.put(id, new PointF(
                    (p.x - cx) * sketchToUnits,
                    (p.y - cy) * sketchToUnits));
        }

        String fixedA = order.get(0);
        String fixedB = order.get(1);
        NativeProjectStore.EdgeData firstEdge =
                NativeGeometry.edgeBetween(room, fixedA, fixedB);
        if (firstEdge == null || firstEdge.value <= 0f) {
            return Result.fail("Не удалось определить первую стену.");
        }

        PointF pa0 = raw.get(fixedA);
        PointF pb0 = raw.get(fixedB);
        float ux = pb0.x - pa0.x;
        float uy = pb0.y - pa0.y;
        float initialLength = (float) Math.hypot(ux, uy);
        if (initialLength < 1f) {
            ux = 1f;
            uy = 0f;
            initialLength = 1f;
        }
        ux /= initialLength;
        uy /= initialLength;
        float alignScale = firstEdge.value / Math.max(1f, initialLength);

        Map<String, PointF> positions = new HashMap<>();
        for (Map.Entry<String, PointF> entry : raw.entrySet()) {
            PointF p = entry.getValue();
            positions.put(entry.getKey(), new PointF(
                    (p.x - pa0.x) * alignScale,
                    (p.y - pa0.y) * alignScale));
        }

        PointF fixedPosA = new PointF(0f, 0f);
        PointF fixedPosB = new PointF(
                ux * firstEdge.value,
                uy * firstEdge.value);
        positions.put(fixedA, new PointF(fixedPosA.x, fixedPosA.y));
        positions.put(fixedB, new PointF(fixedPosB.x, fixedPosB.y));

        ArrayList<DirectionGuide> guides = new ArrayList<>();
        for (NativeProjectStore.EdgeData edge : room.edges) {
            PointF a = raw.get(edge.a);
            PointF b = raw.get(edge.b);
            if (a == null || b == null || edge.value <= 0f) continue;
            float dx = b.x - a.x;
            float dy = b.y - a.y;
            float length = (float) Math.hypot(dx, dy);
            if (length < 1e-6f) continue;
            guides.add(new DirectionGuide(
                    edge.a,
                    edge.b,
                    edge.value,
                    dx / length,
                    dy / length));
        }

        final int totalIterations = 3200;
        final int guidedIterations = 2700;

        for (int iter = 0; iter < totalIterations; iter++) {
            float maxError = 0f;

            for (Constraint constraint : constraints) {
                PointF a = positions.get(constraint.a);
                PointF b = positions.get(constraint.b);
                if (a == null || b == null) continue;

                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float distance = (float) Math.hypot(dx, dy);
                if (distance < 1e-6f) {
                    dx = 1f;
                    dy = 0f;
                    distance = 1f;
                }

                float error = distance - constraint.target;
                maxError = Math.max(maxError, Math.abs(error));
                float nx = dx / distance;
                float ny = dy / distance;
                boolean af = constraint.a.equals(fixedA);
                boolean bf = constraint.b.equals(fixedB);

                if (af && bf) continue;
                if (af) {
                    b.x -= nx * error;
                    b.y -= ny * error;
                } else if (bf) {
                    a.x += nx * error;
                    a.y += ny * error;
                } else {
                    float half = error * .5f;
                    a.x += nx * half;
                    a.y += ny * half;
                    b.x -= nx * half;
                    b.y -= ny * half;
                }
            }

            if (iter < guidedIterations) {
                float progress = iter / (float) guidedIterations;
                float weight = .075f * (1f - progress) + .012f * progress;

                for (DirectionGuide guide : guides) {
                    PointF a = positions.get(guide.a);
                    PointF b = positions.get(guide.b);
                    if (a == null || b == null) continue;

                    float desiredDx = guide.ux * guide.target;
                    float desiredDy = guide.uy * guide.target;
                    float diffX = (b.x - a.x) - desiredDx;
                    float diffY = (b.y - a.y) - desiredDy;
                    boolean af = guide.a.equals(fixedA);
                    boolean bf = guide.b.equals(fixedB);

                    if (af && bf) continue;
                    if (af) {
                        b.x -= diffX * weight;
                        b.y -= diffY * weight;
                    } else if (bf) {
                        a.x += diffX * weight;
                        a.y += diffY * weight;
                    } else {
                        float w = weight * .5f;
                        a.x += diffX * w;
                        a.y += diffY * w;
                        b.x -= diffX * w;
                        b.y -= diffY * w;
                    }
                }
            }

            positions.put(fixedA, new PointF(fixedPosA.x, fixedPosA.y));
            positions.put(fixedB, new PointF(fixedPosB.x, fixedPosB.y));

            if (iter >= guidedIterations && maxError < .03f) break;
        }

        float maxError = maxConstraintError(positions, constraints);

        float sketchSign = Math.signum(signedArea(raw, order));
        float solvedSign = Math.signum(signedArea(positions, order));
        if (sketchSign != 0f && solvedSign != 0f && sketchSign != solvedSign) {
            for (String id : order) {
                PointF p = positions.get(id);
                if (p != null) p.y = -p.y;
            }
        }

        int measuredDiagonals = 0;
        for (NativeProjectStore.DiagonalData d : room.diagonals) {
            if (d.value > 0f) measuredDiagonals++;
        }
        int needed = Math.max(0, order.size() - 3);
        boolean unique = order.size() <= 3 || measuredDiagonals >= needed;

        String message = unique
                ? "Форма уточнена по эскизу, стенам и контрольным диагоналям."
                : "Форма эскиза сохранена. Для точной кривизны нужны контрольные диагонали.";

        return new Result(
                true,
                message,
                positions,
                maxError,
                unique,
                false);
    }


    /**
     * Builds factual geometry from measured walls and measured diagonals only.
     *
     * The sketch is used solely to select the mirror branch of circle intersections.
     * It never contributes a distance, angle or soft direction constraint.
     */
    @NonNull
    static Result solveFactual(@NonNull NativeProjectStore.RoomData room) {
        List<String> order = NativeGeometry.cycleOrder(room);
        if (order == null) {
            return Result.fail("Сначала стены должны образовать один замкнутый контур.");
        }
        if (order.size() < 3) {
            return Result.fail("Для фактической геометрии нужно минимум три угла.");
        }
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value <= 0f) {
                return Result.fail("Сначала введи реальные размеры всех стен.");
            }
        }

        // Factual mode keeps the user's overall sketch as the shape prior,
        // while entered wall lengths and ONLY manually measured diagonals are
        // hard distance constraints. Calculated diagonals never become facts.
        ArrayList<Constraint> factual = factualConstraints(room);
        if (factual.isEmpty()) {
            return Result.fail("Нет фактических размеров для расчёта.");
        }

        ArrayList<Float> ratios = new ArrayList<>();
        for (NativeProjectStore.EdgeData edge : room.edges) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
            if (a == null || b == null) continue;
            float drawn = (float) Math.hypot(b.x - a.x, b.y - a.y);
            if (drawn > 1f) ratios.add(edge.value / drawn);
        }
        float sketchToUnits = median(ratios);
        if (!(sketchToUnits > 0f)) sketchToUnits = 1f;

        float cx = 0f;
        float cy = 0f;
        for (String id : order) {
            NativeProjectStore.PointData p = NativeGeometry.pointById(room, id);
            if (p == null) return Result.fail("Повреждена геометрия комнаты.");
            cx += p.x;
            cy += p.y;
        }
        cx /= order.size();
        cy /= order.size();

        Map<String, PointF> raw = new HashMap<>();
        for (String id : order) {
            NativeProjectStore.PointData p = NativeGeometry.pointById(room, id);
            raw.put(id, new PointF(
                    (p.x - cx) * sketchToUnits,
                    (p.y - cy) * sketchToUnits));
        }

        String fixedA = order.get(0);
        String fixedB = order.get(1);
        NativeProjectStore.EdgeData firstEdge =
                NativeGeometry.edgeBetween(room, fixedA, fixedB);
        if (firstEdge == null || firstEdge.value <= 0f) {
            return Result.fail("Не удалось определить первую стену.");
        }

        PointF pa0 = raw.get(fixedA);
        PointF pb0 = raw.get(fixedB);
        if (pa0 == null || pb0 == null) {
            return Result.fail("Не удалось прочитать исходную форму эскиза.");
        }

        float ux = pb0.x - pa0.x;
        float uy = pb0.y - pa0.y;
        float initialLength = (float) Math.hypot(ux, uy);
        if (initialLength < 1f) {
            ux = 1f;
            uy = 0f;
            initialLength = 1f;
        }
        ux /= initialLength;
        uy /= initialLength;

        float alignScale = firstEdge.value / Math.max(1f, initialLength);
        Map<String, PointF> positions = new HashMap<>();
        for (Map.Entry<String, PointF> entry : raw.entrySet()) {
            PointF p = entry.getValue();
            positions.put(entry.getKey(), new PointF(
                    (p.x - pa0.x) * alignScale,
                    (p.y - pa0.y) * alignScale));
        }

        PointF fixedPosA = new PointF(0f, 0f);
        PointF fixedPosB = new PointF(
                ux * firstEdge.value,
                uy * firstEdge.value);
        positions.put(fixedA, new PointF(fixedPosA.x, fixedPosA.y));
        positions.put(fixedB, new PointF(fixedPosB.x, fixedPosB.y));

        ArrayList<DirectionGuide> guides = new ArrayList<>();
        for (NativeProjectStore.EdgeData edge : room.edges) {
            PointF a = raw.get(edge.a);
            PointF b = raw.get(edge.b);
            if (a == null || b == null) continue;
            float dx = b.x - a.x;
            float dy = b.y - a.y;
            float length = (float) Math.hypot(dx, dy);
            if (length < 1e-6f) continue;
            guides.add(new DirectionGuide(
                    edge.a,
                    edge.b,
                    edge.value,
                    dx / length,
                    dy / length));
        }

        boolean uniquelySupported = hasExactFactualSupport(room, order);
        if (uniquelySupported) {
            // Once field measurements fully determine the polygon, do not leave
            // an iterative residual. Reconstruct it analytically from circle
            // intersections, using the sketch only to select the correct mirror
            // branch and first-wall orientation.
            return solveFactualExact(room, order);
        }

        // With incomplete diagonals the shape is mathematically underdetermined.
        // Keep the field sketch as the direction prior while wall lengths and
        // every manually measured diagonal remain hard constraints.
        final int totalIterations = 4200;
        final int guidedIterations = 3500;
        final float guideStart = .085f;
        final float guideEnd = .010f;

        for (int iter = 0; iter < totalIterations; iter++) {
            if (iter < guidedIterations) {
                float progress = iter / (float) Math.max(1, guidedIterations - 1);
                float weight = guideStart * (1f - progress) + guideEnd * progress;

                for (DirectionGuide guide : guides) {
                    PointF a = positions.get(guide.a);
                    PointF b = positions.get(guide.b);
                    if (a == null || b == null) continue;

                    float desiredDx = guide.ux * guide.target;
                    float desiredDy = guide.uy * guide.target;
                    float diffX = (b.x - a.x) - desiredDx;
                    float diffY = (b.y - a.y) - desiredDy;
                    boolean af = guide.a.equals(fixedA);
                    boolean bf = guide.b.equals(fixedB);

                    if (af && bf) continue;
                    if (af) {
                        b.x -= diffX * weight;
                        b.y -= diffY * weight;
                    } else if (bf) {
                        a.x += diffX * weight;
                        a.y += diffY * weight;
                    } else {
                        float half = weight * .5f;
                        a.x += diffX * half;
                        a.y += diffY * half;
                        b.x -= diffX * half;
                        b.y -= diffY * half;
                    }
                }
            }

            float maxError = 0f;
            for (Constraint constraint : factual) {
                PointF a = positions.get(constraint.a);
                PointF b = positions.get(constraint.b);
                if (a == null || b == null) continue;

                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float distance = (float) Math.hypot(dx, dy);
                if (distance < 1e-6f) {
                    dx = 1f;
                    dy = 0f;
                    distance = 1f;
                }

                float error = distance - constraint.target;
                maxError = Math.max(maxError, Math.abs(error));
                float nx = dx / distance;
                float ny = dy / distance;
                boolean af = constraint.a.equals(fixedA);
                boolean bf = constraint.b.equals(fixedB);

                if (af && bf) continue;
                if (af) {
                    b.x -= nx * error;
                    b.y -= ny * error;
                } else if (bf) {
                    a.x += nx * error;
                    a.y += ny * error;
                } else {
                    float half = error * .5f;
                    a.x += nx * half;
                    a.y += ny * half;
                    b.x -= nx * half;
                    b.y -= ny * half;
                }
            }

            positions.put(fixedA, new PointF(fixedPosA.x, fixedPosA.y));
            positions.put(fixedB, new PointF(fixedPosB.x, fixedPosB.y));

            if (iter >= guidedIterations && maxError < .02f) break;
        }

        float sketchSign = Math.signum(signedArea(raw, order));
        float solvedSign = Math.signum(signedArea(positions, order));
        if (sketchSign != 0f && solvedSign != 0f && sketchSign != solvedSign) {
            for (String id : order) {
                PointF p = positions.get(id);
                if (p != null) p.y = -p.y;
            }
        }

        float maxError = maxConstraintError(positions, factual);
        String message;
        if (maxError > FACTUAL_CONFLICT_TOL) {
            message = String.format(
                    Locale.getDefault(),
                    "Замеры конфликтуют. Максимальная невязка: %.2f м. "
                            + "Эскиз оставлен без изменений.",
                    maxError / 100f);
        } else if (uniquelySupported) {
            message = String.format(
                    Locale.getDefault(),
                    "Фактические размеры применены. Форма уточнена по измеренным "
                            + "стенам и диагоналям. Невязка: %.2f м.",
                    maxError / 100f);
        } else {
            message = String.format(
                    Locale.getDefault(),
                    "Фактические размеры применены с сохранением общей формы эскиза. "
                            + "Измеренные диагонали уточняют форму. Невязка: %.2f м.",
                    maxError / 100f);
        }

        return new Result(
                true,
                message,
                positions,
                maxError,
                uniquelySupported,
                false);
    }

    private static boolean hasExactFactualSupport(
            @NonNull NativeProjectStore.RoomData room,
            @NonNull List<String> order) {
        if (order.size() <= 3) return true;
        String anchorId = order.get(0);
        for (int i = 2; i < order.size() - 1; i++) {
            NativeProjectStore.DiagonalData diagonal =
                    NativeGeometry.diagonalBetween(room, anchorId, order.get(i));
            if (!NativeGeometry.isMeasuredDiagonal(diagonal)) return false;
        }
        return true;
    }

    @NonNull
    private static Result solveFactualExact(
            @NonNull NativeProjectStore.RoomData room,
            @NonNull List<String> order) {
        String anchorId = order.get(0);
        String secondId = order.get(1);

        NativeProjectStore.EdgeData firstEdge =
                NativeGeometry.edgeBetween(room, anchorId, secondId);
        if (firstEdge == null || firstEdge.value <= 0f) {
            return Result.fail("Не удалось прочитать первую стену.");
        }

        NativeProjectStore.PointData sketchA =
                NativeGeometry.pointById(room, anchorId);
        NativeProjectStore.PointData sketchB =
                NativeGeometry.pointById(room, secondId);

        float firstUx = 1f;
        float firstUy = 0f;
        if (sketchA != null && sketchB != null) {
            float sdx = sketchB.x - sketchA.x;
            float sdy = sketchB.y - sketchA.y;
            float sl = (float) Math.hypot(sdx, sdy);
            if (sl > 1e-6f) {
                firstUx = sdx / sl;
                firstUy = sdy / sl;
            }
        }

        Map<String, PointF> positions = new HashMap<>();
        positions.put(anchorId, new PointF(0f, 0f));
        positions.put(secondId, new PointF(
                firstUx * firstEdge.value,
                firstUy * firstEdge.value));

        for (int i = 2; i < order.size(); i++) {
            String previousId = order.get(i - 1);
            String currentId = order.get(i);

            NativeProjectStore.EdgeData wall =
                    NativeGeometry.edgeBetween(room, previousId, currentId);
            if (wall == null || wall.value <= 0f) {
                return Result.fail("Не удалось прочитать одну из стен.");
            }

            Float anchorDistance = factualDistance(room, anchorId, currentId);
            if (anchorDistance == null || anchorDistance <= 0f) {
                return Result.fail(
                        "Недостаточно фактических измерений для угла "
                                + (i + 1) + ".");
            }

            PointF anchor = positions.get(anchorId);
            PointF previous = positions.get(previousId);
            if (anchor == null || previous == null) {
                return Result.fail("Не удалось последовательно восстановить геометрию.");
            }

            float dx = previous.x - anchor.x;
            float dy = previous.y - anchor.y;
            float d = (float) Math.hypot(dx, dy);
            float rAnchor = anchorDistance;
            float rPrevious = wall.value;
            if (d < 1e-6f) {
                return Result.fail("Замеры конфликтуют: две опорные точки совпали.");
            }

            final float epsilon = .002f;
            if (d > rAnchor + rPrevious + epsilon
                    || d < Math.abs(rAnchor - rPrevious) - epsilon) {
                return Result.fail(
                        "Замеры конфликтуют: для угла "
                                + (i + 1)
                                + " измеренные расстояния не образуют треугольник.");
            }

            float along = (rAnchor * rAnchor - rPrevious * rPrevious + d * d)
                    / (2f * d);
            float h2 = rAnchor * rAnchor - along * along;
            if (h2 < -epsilon) {
                return Result.fail(
                        "Замеры конфликтуют: невозможно точно определить угол "
                                + (i + 1) + ".");
            }

            float h = (float) Math.sqrt(Math.max(0f, h2));
            float ux = dx / d;
            float uy = dy / d;
            float baseX = anchor.x + ux * along;
            float baseY = anchor.y + uy * along;

            int side = sketchSide(room, anchorId, previousId, currentId);
            float px = -uy;
            float py = ux;
            positions.put(currentId, new PointF(
                    baseX + side * px * h,
                    baseY + side * py * h));
        }

        ArrayList<Constraint> factual = factualConstraints(room);
        float maxError = maxConstraintError(positions, factual);
        String message;
        if (maxError > FACTUAL_CONFLICT_TOL) {
            message = String.format(
                    Locale.getDefault(),
                    "Замеры конфликтуют. Максимальная невязка: %.2f м. "
                            + "Эскиз оставлен без изменений.",
                    maxError / 100f);
        } else {
            message = String.format(
                    Locale.getDefault(),
                    "Фактические размеры применены точно по измеренным стенам "
                            + "и диагоналям. Невязка: %.2f м.",
                    maxError / 100f);
        }

        return new Result(
                true,
                message,
                positions,
                maxError,
                true,
                false);
    }

    @Nullable
    private static Float factualDistance(
            NativeProjectStore.RoomData room,
            String a,
            String b) {
        NativeProjectStore.EdgeData edge = NativeGeometry.edgeBetween(room, a, b);
        if (edge != null && edge.value > 0f) return edge.value;

        NativeProjectStore.DiagonalData diagonal =
                NativeGeometry.diagonalBetween(room, a, b);
        if (NativeGeometry.isMeasuredDiagonal(diagonal)) return diagonal.value;
        return null;
    }

    private static int sketchSide(
            NativeProjectStore.RoomData room,
            String anchorId,
            String previousId,
            String currentId) {
        NativeProjectStore.PointData anchor = NativeGeometry.pointById(room, anchorId);
        NativeProjectStore.PointData previous = NativeGeometry.pointById(room, previousId);
        NativeProjectStore.PointData current = NativeGeometry.pointById(room, currentId);
        if (anchor == null || previous == null || current == null) return 1;

        float cross = (previous.x - anchor.x) * (current.y - anchor.y)
                - (previous.y - anchor.y) * (current.x - anchor.x);
        if (Math.abs(cross) < 1e-5f) return 1;
        return cross >= 0f ? 1 : -1;
    }

    private static ArrayList<Constraint> factualConstraints(
            NativeProjectStore.RoomData room) {
        ArrayList<Constraint> result = new ArrayList<>();
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value > 0f) {
                result.add(new Constraint(edge.a, edge.b, edge.value));
            }
        }
        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            if (NativeGeometry.isMeasuredDiagonal(diagonal)) {
                result.add(new Constraint(diagonal.a, diagonal.b, diagonal.value));
            }
        }
        return result;
    }

    @NonNull
    static Result solveOrthogonal(@NonNull NativeProjectStore.RoomData room) {
        List<String> order = NativeGeometry.cycleOrder(room);
        if (order == null) {
            return Result.fail("Сначала стены должны образовать один замкнутый контур.");
        }
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value <= 0f) {
                return Result.fail("Сначала введи размеры всех стен.");
            }
        }

        double sx = 0d;
        double sy = 0d;
        for (int i = 0; i < order.size(); i++) {
            String aId = order.get(i);
            String bId = order.get((i + 1) % order.size());
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, aId);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, bId);
            NativeProjectStore.EdgeData edge = NativeGeometry.edgeBetween(room, aId, bId);
            if (a == null || b == null || edge == null || edge.value <= 0f) {
                return Result.fail("Не удалось прочитать одну из стен.");
            }
            double theta = Math.atan2(b.y - a.y, b.x - a.x);
            double weight = Math.pow(Math.max(1f, edge.value), 1.35d);
            sx += Math.cos(theta * 4d) * weight;
            sy += Math.sin(theta * 4d) * weight;
        }
        if (Math.hypot(sx, sy) < 1e-6d) {
            return Result.fail("Не удалось определить направление стен для режима 90°.");
        }

        double phi = Math.atan2(sy, sx) / 4d;
        double cos = Math.cos(phi);
        double sin = Math.sin(phi);

        ArrayList<OrthoSegment> segments = new ArrayList<>();
        float rawErrorX = 0f;
        float rawErrorY = 0f;
        double worstDeviation = 0d;

        for (int i = 0; i < order.size(); i++) {
            String aId = order.get(i);
            String bId = order.get((i + 1) % order.size());
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, aId);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, bId);
            NativeProjectStore.EdgeData edge = NativeGeometry.edgeBetween(room, aId, bId);
            if (a == null || b == null || edge == null) {
                return Result.fail("Не удалось прочитать одну из стен.");
            }

            double dx = b.x - a.x;
            double dy = b.y - a.y;
            double xr = dx * cos + dy * sin;
            double yr = -dx * sin + dy * cos;
            double major = Math.max(Math.abs(xr), Math.abs(yr));
            double minor = Math.min(Math.abs(xr), Math.abs(yr));
            worstDeviation = Math.max(
                    worstDeviation,
                    Math.atan2(minor, Math.max(1e-9d, major)));

            boolean xAxis = Math.abs(xr) >= Math.abs(yr);
            int sign = (xAxis ? xr : yr) >= 0d ? 1 : -1;
            OrthoSegment segment =
                    new OrthoSegment(aId, bId, xAxis, sign, edge.value);
            segments.add(segment);
            if (xAxis) rawErrorX += sign * edge.value;
            else rawErrorY += sign * edge.value;
        }

        if (worstDeviation > Math.PI / 10d) {
            return Result.fail(
                    "Форма содержит стену, которая не похожа на угол 90°. Используй «По размерам».");
        }

        if (Math.abs(rawErrorX) > FIELD_MEASURE_TOL
                || Math.abs(rawErrorY) > FIELD_MEASURE_TOL) {
            return Result.fail("Размеры не замыкаются в допустимом рабочем допуске.");
        }

        int xCount = 0;
        int yCount = 0;
        for (OrthoSegment s : segments) {
            if (s.xAxis) xCount++;
            else yCount++;
        }
        if (xCount > 0 && Math.abs(rawErrorX) > 1e-6f) {
            for (OrthoSegment s : segments) {
                if (s.xAxis) {
                    s.adjusted = s.target - s.sign * (rawErrorX / xCount);
                }
            }
        }
        if (yCount > 0 && Math.abs(rawErrorY) > 1e-6f) {
            for (OrthoSegment s : segments) {
                if (!s.xAxis) {
                    s.adjusted = s.target - s.sign * (rawErrorY / yCount);
                }
            }
        }
        for (OrthoSegment s : segments) {
            if (s.adjusted <= 0f) {
                return Result.fail("После учёта невязки одна из стен получила некорректную длину.");
            }
        }

        Map<String, PointF> positions = new HashMap<>();
        positions.put(order.get(0), new PointF(0f, 0f));
        float x = 0f;
        float y = 0f;
        for (int i = 0; i < segments.size(); i++) {
            OrthoSegment s = segments.get(i);
            if (s.xAxis) x += s.sign * s.adjusted;
            else y += s.sign * s.adjusted;
            if (i < segments.size() - 1) {
                positions.put(s.b, new PointF(x, y));
            }
        }

        float maxDiagError = 0f;
        for (NativeProjectStore.DiagonalData d : room.diagonals) {
            if (d.value <= 0f) continue;
            PointF a = positions.get(d.a);
            PointF b = positions.get(d.b);
            if (a == null || b == null) continue;
            float actual = (float) Math.hypot(b.x - a.x, b.y - a.y);
            maxDiagError = Math.max(maxDiagError, Math.abs(actual - d.value));
        }
        if (maxDiagError > FIELD_MEASURE_TOL) {
            return Result.fail("Диагональ не совпадает с 90°-формой. Проверь замеры.");
        }

        boolean adjusted = Math.abs(rawErrorX) > 1e-6f
                || Math.abs(rawErrorY) > 1e-6f;
        return new Result(
                true,
                adjusted
                        ? "Авто 90° построено с учётом небольшой невязки."
                        : "Авто 90° построено точно.",
                positions,
                maxDiagError,
                true,
                adjusted);
    }

    @Nullable
    static Double exactRectAreaRaw(@NonNull NativeProjectStore.RoomData room) {
        List<String> order = NativeGeometry.cycleOrder(room);
        if (order == null || order.size() != 4) return null;

        float[] sides = new float[4];
        NativeProjectStore.PointData[] points = new NativeProjectStore.PointData[4];
        for (int i = 0; i < 4; i++) {
            String aId = order.get(i);
            String bId = order.get((i + 1) % 4);
            NativeProjectStore.EdgeData edge = NativeGeometry.edgeBetween(room, aId, bId);
            if (edge == null || edge.value <= 0f) return null;
            sides[i] = edge.value;
            points[i] = NativeGeometry.pointById(room, aId);
            if (points[i] == null) return null;
        }

        for (int i = 0; i < 4; i++) {
            NativeProjectStore.PointData prev = points[(i + 3) % 4];
            NativeProjectStore.PointData cur = points[i];
            NativeProjectStore.PointData next = points[(i + 1) % 4];
            float ax = prev.x - cur.x;
            float ay = prev.y - cur.y;
            float bx = next.x - cur.x;
            float by = next.y - cur.y;
            float la = (float) Math.hypot(ax, ay);
            float lb = (float) Math.hypot(bx, by);
            if (la < 1f || lb < 1f) return null;
            float cosine = Math.abs((ax * bx + ay * by) / (la * lb));
            if (cosine > .22f) return null;
        }

        double width = (sides[0] + sides[2]) / 2d;
        double height = (sides[1] + sides[3]) / 2d;
        return width * height;
    }

    @Nullable
    static Double measuredAreaRaw(@NonNull NativeProjectStore.RoomData room) {
        Double rectangle = exactRectAreaRaw(room);
        if (rectangle != null) return rectangle;

        List<String> order = NativeGeometry.cycleOrder(room);
        if (order == null) return null;

        if (NativeGeometry.isFactualGeometry(room)) {
            Result factual = solveFactual(room);
            if (!factual.ok || factual.maxError > FACTUAL_CONFLICT_TOL) return null;
            return (double) Math.abs(signedArea(factual.positions, order));
        }

        Result solved = solveFree(room);
        if (!solved.ok || solved.maxError > FIELD_MEASURE_TOL) return null;
        return (double) Math.abs(signedArea(solved.positions, order));
    }

    @Nullable
    static Float autoDiagonalValue(
            @NonNull NativeProjectStore.RoomData room,
            @NonNull NativeProjectStore.DiagonalData diagonal) {
        Result solved = solveFree(room);
        if (!solved.ok) return null;
        PointF a = solved.positions.get(diagonal.a);
        PointF b = solved.positions.get(diagonal.b);
        if (a == null || b == null) return null;
        return (float) Math.hypot(b.x - a.x, b.y - a.y);
    }

    static int generateAutoDiagonals(@NonNull NativeProjectStore.RoomData room) {
        List<String> order = NativeGeometry.cycleOrder(room);
        if (order == null || order.size() < 4) return 0;
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value <= 0f) return 0;
        }

        String anchor = order.get(0);
        int anchorIndex = 0;
        int added = 0;
        for (int j = 0; j < order.size(); j++) {
            if (j == anchorIndex) continue;
            int diff = Math.abs(j - anchorIndex);
            boolean adjacent = diff == 1 || diff == order.size() - 1;
            if (adjacent) continue;

            String other = order.get(j);
            if (NativeGeometry.diagonalBetween(room, anchor, other) == null) {
                room.diagonals.add(new NativeProjectStore.DiagonalData(anchor, other));
                added++;
            }
        }
        return added;
    }

    private static ArrayList<Constraint> constraints(NativeProjectStore.RoomData room) {
        ArrayList<Constraint> result = new ArrayList<>();
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value > 0f) result.add(new Constraint(edge.a, edge.b, edge.value));
        }
        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            if (diagonal.value > 0f) {
                result.add(new Constraint(diagonal.a, diagonal.b, diagonal.value));
            }
        }
        return result;
    }

    private static float maxConstraintError(
            Map<String, PointF> positions,
            List<Constraint> constraints) {
        float max = 0f;
        for (Constraint c : constraints) {
            PointF a = positions.get(c.a);
            PointF b = positions.get(c.b);
            if (a == null || b == null) continue;
            float actual = (float) Math.hypot(b.x - a.x, b.y - a.y);
            max = Math.max(max, Math.abs(actual - c.target));
        }
        return max;
    }

    private static float signedArea(
            Map<String, PointF> positions,
            List<String> order) {
        double area = 0d;
        for (int i = 0; i < order.size(); i++) {
            PointF a = positions.get(order.get(i));
            PointF b = positions.get(order.get((i + 1) % order.size()));
            if (a == null || b == null) return 0f;
            area += a.x * b.y - b.x * a.y;
        }
        return (float) (area / 2d);
    }

    private static float median(List<Float> values) {
        if (values.isEmpty()) return 1f;
        ArrayList<Float> copy = new ArrayList<>(values);
        Collections.sort(copy);
        int middle = copy.size() / 2;
        if (copy.size() % 2 == 1) return copy.get(middle);
        return (copy.get(middle - 1) + copy.get(middle)) / 2f;
    }
}
