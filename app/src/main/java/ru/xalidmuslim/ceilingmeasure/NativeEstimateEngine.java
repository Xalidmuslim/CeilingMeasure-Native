package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NativeEstimateEngine {
    static final class Meta {
        final String key;
        final String name;
        final String unit;

        Meta(String key, String name, String unit) {
            this.key = key;
            this.name = name;
            this.unit = unit;
        }
    }

    static final Meta[] INTERNAL_META = new Meta[]{
            new Meta("canvas","Полотно","м²"),
            new Meta("baguette","Багет","м"),
            new Meta("insert","Вставка","м"),
            new Meta("light","Светильники","шт"),
            new Meta("corner","Углы","шт"),
            new Meta("cornice","Карниз","м"),
            new Meta("dowel","Дюбель","шт"),
            new Meta("lightline","Световые линии","м"),
            new Meta("pipe","Трубы","шт")
    };

    static final Meta[] CLIENT_META = new Meta[]{
            new Meta("canvas","Квадратура","м²"),
            new Meta("insert","Вставка","м"),
            new Meta("light","Светильники","шт"),
            new Meta("cornice","Карниз","м"),
            new Meta("corner","Углы","шт"),
            new Meta("pipe","Трубы","шт"),
            new Meta("lightline","Световые линии","м"),
            new Meta("shadow","Теневой профиль","м"),
            new Meta("porcelain","Керамогранит","м²")
    };

    static final class Metrics {
        final boolean areaOk;
        final boolean perimeterComplete;
        final double areaM2;
        final double perimeterM;

        Metrics(boolean areaOk, boolean perimeterComplete, double areaM2, double perimeterM) {
            this.areaOk = areaOk;
            this.perimeterComplete = perimeterComplete;
            this.areaM2 = areaM2;
            this.perimeterM = perimeterM;
        }
    }

    static final class Line {
        final String key;
        final String name;
        final String unit;
        final double qty;
        final double price;
        final double total;
        final boolean qtyEditable;
        final boolean removable;

        Line(
                String key,
                String name,
                String unit,
                double qty,
                double price,
                boolean qtyEditable,
                boolean removable) {
            this.key = key;
            this.name = name;
            this.unit = unit;
            this.qty = qty;
            this.price = price;
            this.total = qty * price;
            this.qtyEditable = qtyEditable;
            this.removable = removable;
        }
    }

    static final class RoomCost {
        final Metrics metrics;
        final int dowelQty;
        final double total;
        final boolean complete;
        final ArrayList<Line> lines;

        RoomCost(Metrics metrics, int dowelQty, double total, boolean complete, ArrayList<Line> lines) {
            this.metrics = metrics;
            this.dowelQty = dowelQty;
            this.total = total;
            this.complete = complete;
            this.lines = lines;
        }
    }

    static final class ProjectCost {
        final double areaM2;
        final double perimeterM;
        final double total;
        final boolean complete;

        ProjectCost(double areaM2, double perimeterM, double total, boolean complete) {
            this.areaM2 = areaM2;
            this.perimeterM = perimeterM;
            this.total = total;
            this.complete = complete;
        }
    }

    private NativeEstimateEngine() {}

    @NonNull
    static Metrics metrics(@NonNull NativeProjectStore.RoomData room) {
        boolean perimeterComplete = !room.edges.isEmpty();
        double perimeterUnits = 0d;
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value <= 0f) perimeterComplete = false;
            else perimeterUnits += edge.value;
        }
        Double areaRaw = NativeMeasurementSolver.measuredAreaRaw(room);
        return new Metrics(
                areaRaw != null,
                perimeterComplete,
                areaRaw == null ? 0d : areaRaw / 10000d,
                perimeterComplete ? perimeterUnits / 100d : 0d);
    }

    @NonNull
    static RoomCost internalCost(@NonNull NativeProjectStore.RoomData room) {
        NativeProjectStore.ensureRoomEstimateDefaults(room);
        Metrics m = metrics(room);
        Set<String> hidden = room.hiddenInternalKeys;
        int dowelQty = m.perimeterComplete ? (int) Math.ceil(m.perimeterM * 6d) : 0;
        ArrayList<Line> lines = new ArrayList<>();

        addInternal(lines, room, hidden, "canvas", m.areaM2, "Полотно", "м²", false);
        addInternal(lines, room, hidden, "baguette", m.perimeterM, "Багет", "м", false);
        addInternal(lines, room, hidden, "insert", m.perimeterM, "Вставка", "м", true);
        addInternal(lines, room, hidden, "light", value(room.extraQty,"light"), "Светильники", "шт", true);
        addInternal(lines, room, hidden, "corner", value(room.extraQty,"corner"), "Углы", "шт", true);
        addInternal(lines, room, hidden, "cornice", value(room.extraQty,"cornice"), "Карниз", "м", true);
        addInternal(lines, room, hidden, "dowel", dowelQty, "Дюбель", "шт", true);
        addInternal(lines, room, hidden, "lightline", value(room.extraQty,"lightline"), "Световые линии", "м", true);
        addInternal(lines, room, hidden, "pipe", value(room.extraQty,"pipe"), "Трубы", "шт", true);

        double total = 0d;
        for (Line line : lines) total += line.total;

        boolean perimeterPriceRequired =
                value(room.pricing,"baguette") > 0f
                        || (!hidden.contains("insert") && value(room.pricing,"insert") > 0f)
                        || (!hidden.contains("dowel") && value(room.pricing,"dowel") > 0f);
        boolean complete =
                (value(room.pricing,"canvas") <= 0f || m.areaOk)
                        && (!perimeterPriceRequired || m.perimeterComplete);

        return new RoomCost(m, dowelQty, total, complete, lines);
    }

    @NonNull
    static RoomCost clientCost(@NonNull NativeProjectStore.RoomData room) {
        NativeProjectStore.ensureRoomEstimateDefaults(room);
        Metrics m = metrics(room);
        ArrayList<Line> lines = new ArrayList<>();

        if (m.areaM2 > 0d) {
            lines.add(new Line(
                    "canvas","Квадратура","м²",
                    m.areaM2,
                    value(room.clientPricing,"canvas"),
                    false,
                    false));
        }

        addClient(lines, room, "insert", m.perimeterM, "Вставка", "м");
        addClient(lines, room, "light", value(room.extraQty,"light"), "Светильники", "шт");
        addClient(lines, room, "cornice", value(room.extraQty,"cornice"), "Карниз", "м");
        addClient(lines, room, "corner", value(room.extraQty,"corner"), "Углы", "шт");
        addClient(lines, room, "pipe", value(room.extraQty,"pipe"), "Трубы", "шт");
        addClient(lines, room, "lightline", value(room.extraQty,"lightline"), "Световые линии", "м");
        addClient(lines, room, "shadow", 0d, "Теневой профиль", "м");
        addClient(lines, room, "porcelain", 0d, "Керамогранит", "м²");

        double total = 0d;
        for (Line line : lines) total += line.total;
        return new RoomCost(m, 0, total, m.areaOk && m.perimeterComplete, lines);
    }

    @NonNull
    static ProjectCost projectCost(
            @NonNull NativeProjectStore.ProjectData project,
            boolean client) {
        double area = 0d;
        double perimeter = 0d;
        double total = 0d;
        boolean complete = true;
        for (NativeProjectStore.RoomData room : project.rooms) {
            RoomCost cost = client ? clientCost(room) : internalCost(room);
            area += cost.metrics.areaM2;
            perimeter += cost.metrics.perimeterM;
            total += cost.total;
            if (!cost.complete) complete = false;
        }
        return new ProjectCost(area, perimeter, total, complete);
    }

    static double clientSourceQty(
            @NonNull NativeProjectStore.RoomData room,
            @NonNull String key) {
        Metrics m = metrics(room);
        if ("insert".equals(key)) return m.perimeterM;
        if ("shadow".equals(key) || "porcelain".equals(key)) return 0d;
        return value(room.extraQty, key);
    }

    static double clientDefaultQty(
            @NonNull NativeProjectStore.RoomData room,
            @NonNull String key) {
        double source = clientSourceQty(room, key);
        if (source > 0d) return source;
        if ("shadow".equals(key)) {
            double perimeter = metrics(room).perimeterM;
            return perimeter > 0d ? perimeter : 1d;
        }
        return 1d;
    }

    private static void addInternal(
            List<Line> lines,
            NativeProjectStore.RoomData room,
            Set<String> hidden,
            String key,
            double qty,
            String name,
            String unit,
            boolean removable) {
        if (hidden.contains(key)) return;
        double price = value(room.pricing, key);
        if (qty <= 0d && price <= 0d) return;
        lines.add(new Line(key, name, unit, qty, price, false, removable));
    }

    private static void addClient(
            List<Line> lines,
            NativeProjectStore.RoomData room,
            String key,
            double sourceQty,
            String name,
            String unit) {
        boolean overridden = room.clientEstimateOverride.contains(key);
        double qty = overridden ? value(room.clientEstimateQty, key) : Math.max(0d, sourceQty);
        if (qty <= 0d) return;
        lines.add(new Line(
                key,
                name,
                unit,
                qty,
                value(room.clientPricing,key),
                overridden,
                true));
    }

    static float value(Map<String, Float> map, String key) {
        if (map == null || key == null) return 0f;
        Float value = map.get(key);
        return value == null ? 0f : Math.max(0f, value);
    }

    static Meta clientMeta(String key) {
        for (Meta meta : CLIENT_META) if (meta.key.equals(key)) return meta;
        return new Meta(key,key,"шт");
    }
}
