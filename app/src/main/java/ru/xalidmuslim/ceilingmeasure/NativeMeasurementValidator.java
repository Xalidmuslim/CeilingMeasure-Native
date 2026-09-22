package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

final class NativeMeasurementValidator {
    static final class Result {
        final ArrayList<String> errors = new ArrayList<>();
        final ArrayList<String> warnings = new ArrayList<>();
        double areaM2;
        double perimeterM;

        boolean ok() { return errors.isEmpty(); }

        String message() {
            StringBuilder out = new StringBuilder();
            if (errors.isEmpty() && warnings.isEmpty()) {
                out.append("Проверка пройдена.");
            }
            if (!errors.isEmpty()) {
                out.append("Ошибки:");
                for (String error : errors) out.append("\n• ").append(error);
            }
            if (!warnings.isEmpty()) {
                if (out.length() > 0) out.append("\n\n");
                out.append("Замечания:");
                for (String warning : warnings) out.append("\n• ").append(warning);
            }
            if (ok() && areaM2 > 0d) {
                if (out.length() > 0) out.append("\n\n");
                out.append(String.format(
                        java.util.Locale.getDefault(),
                        "Площадь: %.2f м²\nПериметр: %.2f м",
                        areaM2,
                        perimeterM));
            }
            return out.toString();
        }
    }

    private NativeMeasurementValidator() {}

    @NonNull
    static Result validate(@NonNull NativeProjectStore.RoomData room) {
        Result result = new Result();

        if (room.edges.isEmpty()) {
            result.errors.add("Нет нарисованных стен.");
            return result;
        }

        List<String> cycle = NativeGeometry.cycleOrder(room);
        if (cycle == null) {
            result.errors.add("Стены не образуют один замкнутый контур.");
        }

        int missingWalls = 0;
        float perimeterUnits = 0f;
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value <= 0f) missingWalls++;
            else perimeterUnits += edge.value;
        }
        if (missingWalls > 0) {
            result.errors.add("Не введены размеры стен: " + missingWalls + ".");
        }

        if (cycle != null && missingWalls == 0) {
            NativeMeasurementSolver.Result solved =
                    NativeMeasurementSolver.solveFree(room);
            if (!solved.ok) {
                result.errors.add(solved.message);
            } else if (solved.maxError > NativeMeasurementSolver.FIELD_MEASURE_TOL) {
                result.errors.add("Замеры конфликтуют: расхождение превышает 0,10 м.");
            }

            Double areaRaw = NativeMeasurementSolver.measuredAreaRaw(room);
            if (areaRaw == null) {
                if (result.errors.isEmpty()) {
                    result.errors.add("Не удалось восстановить площадь по введённым размерам.");
                }
            } else {
                result.areaM2 = areaRaw / 10000d;
                result.perimeterM = perimeterUnits / 100d;
            }

            int needed = Math.max(0, cycle.size() - 3);
            int measured = 0;
            int calculated = 0;
            for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
                if (NativeGeometry.isMeasuredDiagonal(diagonal)) measured++;
                else if (diagonal.value > 0f) calculated++;
            }
            if (needed > measured) {
                int left = needed - measured;
                result.warnings.add(
                        "Для фактической геометрии измерь ещё "
                                + left
                                + (left == 1 ? " контрольную диагональ." : " контрольные диагонали."));
            }
            if (calculated > 0) {
                result.warnings.add(
                        "Расчётные диагонали не считаются фактическим замером: "
                                + calculated + ".");
            }
        }

        int emptyDiagonals = 0;
        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            if (diagonal.value <= 0f) emptyDiagonals++;
        }
        if (emptyDiagonals > 0) {
            result.warnings.add("Есть диагонали без введённого размера: " + emptyDiagonals + ".");
        }

        return result;
    }
}
