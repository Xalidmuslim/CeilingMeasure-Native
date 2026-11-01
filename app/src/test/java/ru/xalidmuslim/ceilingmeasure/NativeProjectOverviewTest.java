package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/** The overview must never present partial measurements as complete totals. */
public final class NativeProjectOverviewTest {
    @Test public void threeRoomsAggregateEachMetricAndReportIncompleteMeasurements() {
        NativeProjectOverview.Summary summary = NativeProjectOverview.of(Arrays.asList(
                new NativeProjectOverview.RoomMetrics("Кухня", true, 14.5, true, 15.0),
                new NativeProjectOverview.RoomMetrics("Спальня", true, 19.25, true, 18.8),
                new NativeProjectOverview.RoomMetrics("Гостиная", false, 0, false, 0)));
        assertEquals(3, summary.rooms.size());
        assertEquals(2, summary.areasMeasured);
        assertEquals(2, summary.perimetersMeasured);
        assertEquals(33.75, summary.totalAreaM2, 0.0001);
        assertEquals(33.8, summary.totalPerimeterM, 0.0001);
        assertFalse(summary.isComplete());
    }

    @Test public void allMeasuredRoomsShowCompleteTotals() {
        NativeProjectOverview.Summary summary = NativeProjectOverview.of(Arrays.asList(
                new NativeProjectOverview.RoomMetrics("Комната 1", true, 10, true, 13),
                new NativeProjectOverview.RoomMetrics("Комната 2", true, 20, true, 19),
                new NativeProjectOverview.RoomMetrics("Комната 3", true, 30, true, 23)));
        assertEquals(60, summary.totalAreaM2, 0.0001);
        assertEquals(55, summary.totalPerimeterM, 0.0001);
        assertTrue(summary.isComplete());
    }

    @Test public void emptyAndUnmeasuredAreNotClaimedAsZeroSquareMeters() {
        assertFalse(NativeProjectOverview.of(Collections.emptyList()).isComplete());
        NativeProjectOverview.Summary summary = NativeProjectOverview.of(
                Collections.singletonList(
                        new NativeProjectOverview.RoomMetrics("Комната 1", false, 0, false, 0)));
        assertEquals(0, summary.areasMeasured);
        assertEquals(0, summary.perimetersMeasured);
        assertFalse(summary.isComplete());
    }
}
