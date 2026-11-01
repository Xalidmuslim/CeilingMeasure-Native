package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only measurements for the visible project overview.
 * It never changes saved geometry, room pricing, or the client estimate.
 */
final class NativeProjectOverview {
    static final class RoomMetrics {
        final String name;
        final boolean areaMeasured;
        final double areaM2;
        final boolean perimeterMeasured;
        final double perimeterM;

        RoomMetrics(String name, boolean areaMeasured, double areaM2,
                    boolean perimeterMeasured, double perimeterM) {
            this.name = name;
            this.areaMeasured = areaMeasured;
            this.areaM2 = areaM2;
            this.perimeterMeasured = perimeterMeasured;
            this.perimeterM = perimeterM;
        }
    }

    static final class Summary {
        final List<RoomMetrics> rooms;
        final int areasMeasured;
        final int perimetersMeasured;
        final double totalAreaM2;
        final double totalPerimeterM;

        Summary(List<RoomMetrics> rooms, int areasMeasured, int perimetersMeasured,
                double totalAreaM2, double totalPerimeterM) {
            this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
            this.areasMeasured = areasMeasured;
            this.perimetersMeasured = perimetersMeasured;
            this.totalAreaM2 = totalAreaM2;
            this.totalPerimeterM = totalPerimeterM;
        }

        boolean isComplete() {
            return !rooms.isEmpty()
                    && areasMeasured == rooms.size()
                    && perimetersMeasured == rooms.size();
        }
    }

    private NativeProjectOverview() {}

    @NonNull
    static Summary of(@NonNull List<RoomMetrics> rooms) {
        int areas = 0, perimeters = 0;
        double totalArea = 0d, totalPerimeter = 0d;
        for (RoomMetrics row : rooms) {
            if (row.areaMeasured) {
                areas++;
                totalArea += row.areaM2;
            }
            if (row.perimeterMeasured) {
                perimeters++;
                totalPerimeter += row.perimeterM;
            }
        }
        return new Summary(rooms, areas, perimeters, totalArea, totalPerimeter);
    }

    @NonNull
    static Summary fromProject(@NonNull NativeProjectStore.ProjectData project) {
        ArrayList<RoomMetrics> rows = new ArrayList<>();
        for (NativeProjectStore.RoomData room : project.rooms) {
            NativeEstimateEngine.Metrics measured = NativeEstimateEngine.metrics(room);
            // Match the editor: a square-metre value needs a measured,
            // closed contour, not merely a temporary outline estimate.
            boolean validArea = measured.areaOk
                    && measured.perimeterComplete && NativeGeometry.isClosed(room);
            rows.add(new RoomMetrics(room.name, validArea, measured.areaM2,
                    measured.perimeterComplete, measured.perimeterM));
        }
        return of(rows);
    }
}
