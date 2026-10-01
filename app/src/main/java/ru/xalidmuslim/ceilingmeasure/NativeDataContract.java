package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure data-contract implementation of the audited v184 web persistence model.
 *
 * This class deliberately contains no Android storage/UI code so migrations and
 * backup compatibility can be verified by local JVM unit tests.
 */
final class NativeDataContract {
    static final int DB_SCHEMA_VERSION = 165;
    static final int UI_PREFS_VERSION = 3;
    static final String DB_STORAGE_KEY = "ceilingMeasureData";
    static final String UI_PREFS_STORAGE_KEY = "ceilingMeasureUiPrefsV1";

    static final String[] INTERNAL_PRICE_KEYS = new String[]{
            "canvas", "baguette", "insert", "light", "corner",
            "cornice", "dowel", "lightline", "pipe"
    };
    static final String[] INTERNAL_REMOVABLE_KEYS = new String[]{
            "insert", "light", "corner", "cornice", "dowel", "lightline", "pipe"
    };
    static final String[] CLIENT_PRICE_KEYS = new String[]{
            "canvas", "insert", "light", "cornice", "corner",
            "pipe", "lightline", "shadow", "porcelain"
    };
    static final String[] CLIENT_QTY_KEYS = new String[]{
            "insert", "light", "cornice", "corner",
            "pipe", "lightline", "shadow", "porcelain"
    };
    static final String[] EXTRA_QTY_KEYS = new String[]{
            "light", "corner", "cornice", "lightline", "pipe"
    };

    private static final String[][] CLIENT_META = new String[][]{
            {"canvas", "Квадратура", "м²"},
            {"insert", "Вставка", "м"},
            {"light", "Светильники", "шт"},
            {"cornice", "Карниз", "м"},
            {"corner", "Углы", "шт"},
            {"pipe", "Трубы", "шт"},
            {"lightline", "Световые линии", "м"},
            {"shadow", "Теневой профиль", "м"},
            {"porcelain", "Керамогранит", "м²"}
    };

    interface IdSource {
        String next(@NonNull String prefix);
    }

    static final IdSource RANDOM_IDS =
            prefix -> prefix + "_" + UUID.randomUUID().toString();

    enum LegacyUnitMode {
        NONE,
        CM_X10,
        OLD_MM
    }

    static final class SelectedStorage {
        final String key;
        final String raw;
        final LegacyUnitMode unitMode;

        SelectedStorage(String key, String raw, LegacyUnitMode unitMode) {
            this.key = key;
            this.raw = raw;
            this.unitMode = unitMode;
        }
    }

    private NativeDataContract() {
    }

    @Nullable
    static SelectedStorage selectLegacyStorage(@NonNull Map<String, ?> storage) {
        String raw = nonEmptyString(storage.get(DB_STORAGE_KEY));
        if (raw != null) {
            return new SelectedStorage(DB_STORAGE_KEY, raw, LegacyUnitMode.NONE);
        }

        for (int version = 125; version >= 15; version--) {
            String key = "ceilingTouchV" + version;
            raw = nonEmptyString(storage.get(key));
            if (raw != null) return new SelectedStorage(key, raw, LegacyUnitMode.NONE);
        }

        for (int version : new int[]{14, 13}) {
            String key = "ceilingTouchV" + version;
            raw = nonEmptyString(storage.get(key));
            if (raw != null) return new SelectedStorage(key, raw, LegacyUnitMode.CM_X10);
        }

        for (int version : new int[]{12, 11, 10, 9, 8, 6, 5}) {
            String key = "ceilingTouchV" + version;
            raw = nonEmptyString(storage.get(key));
            if (raw != null) return new SelectedStorage(key, raw, LegacyUnitMode.OLD_MM);
        }
        return null;
    }

    @NonNull
    static JSONObject newEmptyDatabase() {
        JSONObject db = new JSONObject();
        try {
            db.put("orders", new JSONArray());
            return canonicalizeDatabase(db, LegacyUnitMode.NONE, RANDOM_IDS);
        } catch (JSONException e) {
            return db;
        }
    }

    @NonNull
    static JSONObject canonicalizeDatabase(
            @NonNull JSONObject input,
            @NonNull LegacyUnitMode unitMode,
            @NonNull IdSource ids) throws JSONException {
        JSONObject db = deepCopy(input);
        JSONArray orders = db.optJSONArray("orders");
        if (orders == null) {
            orders = new JSONArray();
            db.put("orders", orders);
        }

        // persistDbRaw always deletes these two legacy order fields.
        for (int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.optJSONObject(i);
            if (order == null) continue;
            order.remove("number");
            order.remove("savedName");
        }

        // v14/v13 and old-mm keys both pass through normRoom; only v14/v13 scales values.
        if (unitMode != LegacyUnitMode.NONE) {
            forEachRoom(db, room -> {
                normalizeRoom(room, ids);
                if (unitMode == LegacyUnitMode.CM_X10) {
                    multiplyMeasurementsByTen(room.optJSONArray("edges"));
                    multiplyMeasurementsByTen(room.optJSONArray("diagList"));
                }
            });
        }

        migratePricingToRooms(db, ids);
        migrateDefaultClientPricingV146(db);
        migrateInternalAllRoomsV157(db, ids);
        migrateHideAutoLettersV64(db, ids);
        migrateOtherRoomNamesV66(db);
        migrateHintsCollapsedV73(db, ids);

        // Every native read/write uses the audited room shape. This is equivalent to
        // the web app reaching each room through normRoom before use.
        forEachRoom(db, room -> normalizeRoom(room, ids));

        ensureDefaultPricing(db);
        ensureDefaultClientPricing(db);
        ensureHiddenDefaultKeys(db, "hiddenDefaultPurchaseKeys", INTERNAL_PRICE_KEYS);
        ensureHiddenDefaultKeys(db, "hiddenDefaultClientKeys", CLIENT_PRICE_KEYS);
        ensureDefaultClientStandardMeta(db);
        ensureDefaultCustomItems(db, "defaultPurchaseCustomItems", ids);
        ensureDefaultCustomItems(db, "defaultClientCustomItems", ids);

        JSONObject uiCollapse = db.optJSONObject("uiCollapse");
        if (uiCollapse == null) db.put("uiCollapse", new JSONObject());

        // Canonical order fields that are present on newly-created v184 orders.
        for (int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.optJSONObject(i);
            if (order == null) continue;
            normalizeOrder(order, ids);
        }

        db.put("schemaVersion", DB_SCHEMA_VERSION);
        return db;
    }

    @NonNull
    static JSONObject importBackup(@NonNull String raw, @NonNull IdSource ids)
            throws JSONException {
        JSONObject payload = new JSONObject(raw);
        JSONObject db = payload.optJSONObject("db");
        if (db == null) db = payload;
        if (!(db.opt("orders") instanceof JSONArray)) {
            throw new JSONException("Backup must contain orders array");
        }
        return canonicalizeDatabase(db, LegacyUnitMode.NONE, ids);
    }

    @NonNull
    static String exportBackup(
            @NonNull JSONObject db,
            @NonNull String exportedAt,
            @NonNull IdSource ids) throws JSONException {
        JSONObject canonical = canonicalizeDatabase(db, LegacyUnitMode.NONE, ids);
        JSONObject payload = new JSONObject();
        payload.put("app", "ceilingMeasure");
        payload.put("version", DB_SCHEMA_VERSION);
        payload.put("exportedAt", exportedAt);
        payload.put("db", canonical);
        return payload.toString();
    }

    @NonNull
    static JSONObject canonicalizeUiPrefs(@Nullable JSONObject input) {
        JSONObject raw = input == null ? new JSONObject() : deepCopyQuiet(input);

        String font = raw.optString("font", "system");
        if ("georgia".equals(font)) font = "serif";
        else if ("arial".equals(font)) font = "system";
        if (!"system".equals(font) && !"serif".equals(font) && !"mono".equals(font)) {
            font = "system";
        }

        double scale;
        if (raw.has("scale") && raw.opt("scale") != JSONObject.NULL) {
            scale = numeric(raw.opt("scale"), 1.18d);
        } else {
            String size = raw.optString("size", "");
            if ("small".equals(size)) scale = .90d;
            else if ("normal".equals(size)) scale = 1d;
            else if ("large".equals(size)) scale = 1.12d;
            else if ("xlarge".equals(size)) scale = 1.24d;
            else scale = 1.18d;
        }
        scale = clamp(scale, .85d, 1.30d);

        String weight = raw.optString("weight", "normal");
        if (!"normal".equals(weight) && !"medium".equals(weight) && !"bold".equals(weight)) {
            weight = "normal";
        }

        int version = (int) numeric(raw.opt("version"), 1d);
        // Preserve the old v1 -> v2 migration first, then move only the
        // canonical default profile to the slightly larger v3 default.
        if (version < 2 && Math.abs(scale - 1d) < .001d) {
            scale = 1.12d;
        }
        if (version < 3
                && Math.abs(scale - 1.12d) < .001d
                && "system".equals(font)
                && "normal".equals(weight)) {
            scale = 1.18d;
        }

        JSONObject out = new JSONObject();
        try {
            out.put("version", UI_PREFS_VERSION);
            out.put("font", font);
            out.put("scale", scale);
            out.put("weight", weight);
        } catch (JSONException ignored) {
        }
        return out;
    }

    @NonNull
    static String normalizeMaterialUnit(@Nullable String unit) {
        if ("м".equals(unit) || "м²".equals(unit) || "компл.".equals(unit)) return unit;
        return "шт";
    }

    private interface RoomConsumer {
        void accept(JSONObject room) throws JSONException;
    }

    private static void forEachRoom(JSONObject db, RoomConsumer consumer) throws JSONException {
        JSONArray orders = db.optJSONArray("orders");
        if (orders == null) return;
        for (int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.optJSONObject(i);
            if (order == null) continue;
            JSONArray rooms = order.optJSONArray("rooms");
            if (rooms == null) continue;
            for (int j = 0; j < rooms.length(); j++) {
                JSONObject room = rooms.optJSONObject(j);
                if (room != null) consumer.accept(room);
            }
        }
    }

    private static void migratePricingToRooms(JSONObject db, IdSource ids) throws JSONException {
        JSONArray orders = db.optJSONArray("orders");
        if (orders == null) return;
        for (int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.optJSONObject(i);
            if (order == null) continue;
            JSONArray rooms = ensureArray(order, "rooms");
            if (order.optBoolean("pricingMigratedV59", false)) {
                for (int j = 0; j < rooms.length(); j++) {
                    JSONObject room = rooms.optJSONObject(j);
                    if (room == null) continue;
                    normalizeRoom(room, ids);
                    ensureNumberMap(room, "pricing", INTERNAL_PRICE_KEYS);
                    ensureNumberMap(room, "extraQty", EXTRA_QTY_KEYS);
                }
                continue;
            }

            JSONObject legacy = ensureObject(order, "pricing");
            setNonNegative(legacy, "canvas");
            setNonNegative(legacy, "baguette");
            setNonNegative(legacy, "insert");

            for (int j = 0; j < rooms.length(); j++) {
                JSONObject room = rooms.optJSONObject(j);
                if (room == null) continue;
                JSONObject pricing = new JSONObject();
                pricing.put("canvas", nonNegative(legacy.opt("canvas")));
                pricing.put("baguette", nonNegative(legacy.opt("baguette")));
                pricing.put("insert", nonNegative(legacy.opt("insert")));
                pricing.put("light", 0d);
                pricing.put("corner", 0d);
                pricing.put("cornice", 0d);
                pricing.put("dowel", 0d);
                pricing.put("lightline", 0d);
                pricing.put("pipe", 0d);
                room.put("pricing", pricing);

                JSONObject extra = new JSONObject();
                for (String key : EXTRA_QTY_KEYS) extra.put(key, 0d);
                room.put("extraQty", extra);
            }
            order.put("pricingMigratedV59", true);
        }
    }

    private static void migrateDefaultClientPricingV146(JSONObject db) throws JSONException {
        if (db.optBoolean("defaultClientPricingMigratedV146", false)) return;

        if (!(db.opt("defaultClientPricing") instanceof JSONObject)) {
            JSONObject seed = null;
            JSONArray orders = db.optJSONArray("orders");
            if (orders != null) {
                outer:
                for (int i = 0; i < orders.length(); i++) {
                    JSONObject order = orders.optJSONObject(i);
                    if (order == null) continue;
                    JSONArray rooms = order.optJSONArray("rooms");
                    if (rooms == null) continue;
                    for (int j = 0; j < rooms.length(); j++) {
                        JSONObject room = rooms.optJSONObject(j);
                        if (room != null && room.opt("clientPricing") instanceof JSONObject) {
                            seed = room.optJSONObject("clientPricing");
                            break outer;
                        }
                    }
                }
            }
            JSONObject defaults = new JSONObject();
            for (String key : CLIENT_PRICE_KEYS) {
                defaults.put(key, seed == null ? 0d : nonNegative(seed.opt(key)));
            }
            db.put("defaultClientPricing", defaults);
        }

        ensureDefaultClientPricing(db);
        db.put("defaultClientPricingMigratedV146", true);
    }

    private static void migrateInternalAllRoomsV157(JSONObject db, IdSource ids)
            throws JSONException {
        if (db.optBoolean("internalAllRoomsMigratedV157", false)) return;
        forEachRoom(db, room -> {
            normalizeRoom(room, ids);
            room.put("extrasExpanded", true);
        });
        db.put("internalAllRoomsMigratedV157", true);
    }

    private static void migrateHideAutoLettersV64(JSONObject db, IdSource ids)
            throws JSONException {
        if (db.optBoolean("autoLettersMigratedV64", false)) return;
        forEachRoom(db, room -> {
            normalizeRoom(room, ids);
            room.put("showAutoLetters", false);
        });
        db.put("autoLettersMigratedV64", true);
    }

    private static void migrateOtherRoomNamesV66(JSONObject db) throws JSONException {
        if (db.optBoolean("otherRoomNamesMigratedV66", false)) return;
        forEachRoom(db, room -> {
            String name = room.optString("name", "");
            if ("Другая".equalsIgnoreCase(name)) {
                room.put("name", "Другое");
            } else if (name.matches("(?i)^Другая\\s+\\d+$")) {
                int space = name.lastIndexOf(' ');
                room.put("name", "Другое " + name.substring(space + 1));
            }
        });
        db.put("otherRoomNamesMigratedV66", true);
    }

    private static void migrateHintsCollapsedV73(JSONObject db, IdSource ids)
            throws JSONException {
        if (db.optBoolean("hintsCollapsedMigratedV73", false)) return;
        forEachRoom(db, room -> {
            normalizeRoom(room, ids);
            room.put("hintCollapsed", true);
        });
        db.put("hintsCollapsedMigratedV73", true);
    }

    static void normalizeRoom(@NonNull JSONObject room, @NonNull IdSource ids)
            throws JSONException {
        flattenLegacyDrawings(room);

        JSONObject view = room.optJSONObject("view");
        if (view == null) view = new JSONObject();
        view.put("scale", clamp(numeric(view.opt("scale"), 1d), .45d, 12d));
        view.put("panX", numeric(view.opt("panX"), 0d));
        view.put("panY", numeric(view.opt("panY"), 0d));
        room.put("view", view);

        if (!room.has("notes") || room.isNull("notes")) room.put("notes", "");

        room.remove("objects");
        room.remove("objectAutoQty");
        room.remove("lightAutoLayout");

        if (room.opt("clientPricing") instanceof JSONObject) {
            JSONObject clientPricing = room.optJSONObject("clientPricing");
            for (String key : INTERNAL_PRICE_KEYS) setNonNegative(clientPricing, key);
            setNonNegative(clientPricing, "shadow");
            setNonNegative(clientPricing, "porcelain");
        } else {
            room.put("clientPricing", JSONObject.NULL);
        }

        JSONObject estimateQty = room.optJSONObject("clientEstimateQty");
        if (estimateQty == null) estimateQty = new JSONObject();
        JSONObject estimateOverride = room.optJSONObject("clientEstimateOverride");
        if (estimateOverride == null) estimateOverride = new JSONObject();
        for (String key : CLIENT_QTY_KEYS) {
            estimateQty.put(key, nonNegative(estimateQty.opt(key)));
            estimateOverride.put(key, truthyBoolean(estimateOverride.opt(key)));
        }
        room.put("clientEstimateQty", estimateQty);
        room.put("clientEstimateOverride", estimateOverride);

        normalizeRoomCustomItems(room, "customItems", "sourceId", ids);
        normalizeRoomCustomItems(room, "clientCustomItems", "sourcePresetId", ids);
        ensureHiddenRoomKeys(room);

        room.put("geometryLocked", room.optBoolean("geometryLocked", false));
        room.put("validationExpanded",
                !room.has("validationExpanded") || room.optBoolean("validationExpanded", true));
        if (!(room.opt("showAutoLetters") instanceof Boolean)) room.put("showAutoLetters", false);
        if (!(room.opt("point90Magnet") instanceof Boolean)) room.put("point90Magnet", true);
        room.put("hintCollapsed", room.optBoolean("hintCollapsed", false));
        room.put("extrasExpanded", room.optBoolean("extrasExpanded", false));

        JSONArray points = room.optJSONArray("points");
        if (points == null) points = new JSONArray();

        if (!(room.opt("edges") instanceof JSONArray)) {
            JSONArray oldPoints = points;
            JSONArray newPoints = new JSONArray();
            for (int i = 0; i < oldPoints.length(); i++) {
                Object raw = oldPoints.opt(i);
                JSONObject source = raw instanceof JSONObject ? (JSONObject) raw : new JSONObject();
                JSONObject point = new JSONObject();
                point.put("id", ids.next("p"));
                point.put("x", numeric(source.opt("x"), 0d));
                point.put("y", numeric(source.opt("y"), 0d));
                newPoints.put(point);
            }

            boolean closed = room.optBoolean("closed", false);
            JSONObject walls = room.optJSONObject("walls");
            JSONArray edges = new JSONArray();
            int n = newPoints.length();
            int limit = closed ? n : Math.max(0, n - 1);
            for (int i = 0; i < limit; i++) {
                int j = (i + 1) % Math.max(1, n);
                JSONObject a = newPoints.optJSONObject(i);
                JSONObject b = newPoints.optJSONObject(j);
                if (a == null || b == null) continue;
                JSONObject edge = new JSONObject();
                edge.put("id", ids.next("e"));
                edge.put("a", a.optString("id", ""));
                edge.put("b", b.optString("id", ""));
                Object mm = walls == null ? "" : walls.opt(i + "-" + j);
                edge.put("mm", mm == null || mm == JSONObject.NULL ? "" : mm);
                edges.put(edge);
            }

            JSONArray diagList = new JSONArray();
            JSONObject diags = room.optJSONObject("diags");
            if (diags != null) {
                Iterator<String> keys = diags.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String[] parts = key.split("-");
                    if (parts.length != 2) continue;
                    int aIndex = parseInt(parts[0], -1);
                    int bIndex = parseInt(parts[1], -1);
                    JSONObject a = newPoints.optJSONObject(aIndex);
                    JSONObject b = newPoints.optJSONObject(bIndex);
                    if (a == null || b == null) continue;
                    JSONObject diag = new JSONObject();
                    diag.put("id", ids.next("d"));
                    diag.put("a", a.optString("id", ""));
                    diag.put("b", b.optString("id", ""));
                    Object mm = diags.opt(key);
                    diag.put("mm", mm == null || mm == JSONObject.NULL ? "" : mm);
                    diagList.put(diag);
                }
            }

            room.put("points", newPoints);
            room.put("edges", edges);
            room.put("diagList", diagList);
            room.remove("walls");
            room.remove("diags");
            room.remove("closed");
        } else {
            room.put("points", points);
            JSONArray edges = room.optJSONArray("edges");
            JSONArray diagList = room.optJSONArray("diagList");
            if (diagList == null) diagList = new JSONArray();
            room.put("diagList", diagList);

            for (int i = 0; i < points.length(); i++) {
                JSONObject point = points.optJSONObject(i);
                if (point == null) continue;
                if (point.optString("id", "").isEmpty()) point.put("id", ids.next("p"));
                if (point.has("customLabel")
                        && !"A".equals(point.optString("customLabel", ""))) {
                    point.remove("customLabel");
                }
            }
            if (edges != null) {
                for (int i = 0; i < edges.length(); i++) {
                    JSONObject edge = edges.optJSONObject(i);
                    if (edge != null && edge.optString("id", "").isEmpty()) {
                        edge.put("id", ids.next("e"));
                    }
                }
            }
            for (int i = 0; i < diagList.length(); i++) {
                JSONObject diag = diagList.optJSONObject(i);
                if (diag != null && diag.optString("id", "").isEmpty()) {
                    diag.put("id", ids.next("d"));
                }
            }
        }

        ensureNumberMap(room, "pricing", INTERNAL_PRICE_KEYS);
        ensureNumberMap(room, "extraQty", EXTRA_QTY_KEYS);
    }

    private static void normalizeOrder(JSONObject order, IdSource ids) throws JSONException {
        order.remove("number");
        order.remove("savedName");
        order.remove("activeRoom");
        order.remove("updatedAt");
        order.remove("geometryVersion");

        if (order.optString("id", "").isEmpty()) order.put("id", ids.next("order"));
        if (!order.has("name") || order.isNull("name")) order.put("name", "");
        if (!order.has("phone") || order.isNull("phone")) order.put("phone", "");
        if (!order.has("address") || order.isNull("address")) order.put("address", "");
        if (!order.has("status") || order.isNull("status")) order.put("status", "Замер");

        JSONObject pricing = ensureObject(order, "pricing");
        setNonNegative(pricing, "canvas");
        setNonNegative(pricing, "baguette");
        setNonNegative(pricing, "insert");
        if (!(order.opt("pricingMigratedV59") instanceof Boolean)) {
            order.put("pricingMigratedV59", true);
        }

        JSONArray combined = order.optJSONArray("combinedEstimateOrderIds");
        if (combined == null) order.put("combinedEstimateOrderIds", new JSONArray());

        JSONArray rooms = ensureArray(order, "rooms");
        for (int i = 0; i < rooms.length(); i++) {
            JSONObject room = rooms.optJSONObject(i);
            if (room == null) continue;
            if (room.optString("id", "").isEmpty()) room.put("id", ids.next("room"));
            if (!room.has("name") || room.isNull("name")) room.put("name", "Комната " + (i + 1));
            normalizeRoom(room, ids);
        }
    }

    private static void flattenLegacyDrawings(JSONObject room) throws JSONException {
        JSONArray drawings = room.optJSONArray("drawings");
        if (drawings == null || drawings.length() == 0) return;

        JSONObject active = null;
        String activeId = room.optString("activeDrawingId", "");
        for (int i = 0; i < drawings.length(); i++) {
            JSONObject candidate = drawings.optJSONObject(i);
            if (candidate == null) continue;
            if (!activeId.isEmpty() && activeId.equals(candidate.optString("id", ""))) {
                active = candidate;
                break;
            }
        }
        if (active == null) active = drawings.optJSONObject(0);

        boolean rootHasGeometry = length(room.optJSONArray("points")) > 0
                || length(room.optJSONArray("edges")) > 0
                || length(room.optJSONArray("diagList")) > 0;
        if (!rootHasGeometry && active != null) {
            room.put("points", copyArray(active.optJSONArray("points")));
            room.put("edges", copyArray(active.optJSONArray("edges")));
            room.put("diagList", copyArray(active.optJSONArray("diagList")));
            room.put("notes", active.optString("notes", room.optString("notes", "")));
            if (active.opt("showAutoLetters") instanceof Boolean) {
                room.put("showAutoLetters", active.optBoolean("showAutoLetters"));
            }
            room.put("hintCollapsed", active.optBoolean("hintCollapsed", false));
            JSONObject activeView = active.optJSONObject("view");
            if (activeView != null) room.put("view", deepCopy(activeView));
        }

        room.remove("drawings");
        room.remove("activeDrawingId");
    }

    private static void multiplyMeasurementsByTen(@Nullable JSONArray items)
            throws JSONException {
        if (items == null) return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            double value = measurement(item.opt("mm"));
            if (value > 0d) item.put("mm", String.valueOf(Math.round(value * 10d)));
        }
    }

    private static void normalizeRoomCustomItems(
            JSONObject room,
            String key,
            String sourceKey,
            IdSource ids) throws JSONException {
        JSONArray input = room.optJSONArray(key);
        JSONArray output = new JSONArray();
        if (input != null) {
            for (int i = 0; i < input.length(); i++) {
                JSONObject raw = input.optJSONObject(i);
                if (raw == null) continue;
                JSONObject item = new JSONObject();
                item.put("id", emptyTo(raw.optString("id", ""), ids.next("mat")));
                String source = raw.optString(sourceKey, "");
                item.put(sourceKey, source.isEmpty() ? JSONObject.NULL : source);
                String name = raw.optString("name", "Материал").trim();
                item.put("name", name.isEmpty() ? "Материал" : name);
                item.put("unit", normalizeMaterialUnit(raw.optString("unit", "шт")));
                item.put("qty", nonNegative(raw.opt("qty")));
                item.put("price", nonNegative(raw.opt("price")));
                output.put(item);
            }
        }
        room.put(key, output);
    }

    private static void ensureHiddenRoomKeys(JSONObject room) throws JSONException {
        JSONArray input = room.optJSONArray("hiddenInternalKeys");
        Set<String> allowed = stringSet(INTERNAL_REMOVABLE_KEYS);
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (input != null) {
            for (int i = 0; i < input.length(); i++) {
                String key = input.optString(i, "");
                if (allowed.contains(key)) unique.add(key);
            }
        }
        room.put("hiddenInternalKeys", toArray(unique));
    }

    private static void ensureDefaultPricing(JSONObject db) throws JSONException {
        ensureNumberMap(db, "defaultPricing", INTERNAL_PRICE_KEYS);
    }

    private static void ensureDefaultClientPricing(JSONObject db) throws JSONException {
        ensureNumberMap(db, "defaultClientPricing", CLIENT_PRICE_KEYS);
    }

    private static void ensureHiddenDefaultKeys(
            JSONObject db,
            String key,
            String[] allowedKeys) throws JSONException {
        JSONArray input = db.optJSONArray(key);
        Set<String> allowed = stringSet(allowedKeys);
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (input != null) {
            for (int i = 0; i < input.length(); i++) {
                String value = input.optString(i, "");
                if (allowed.contains(value)) unique.add(value);
            }
        }
        db.put(key, toArray(unique));
    }

    private static void ensureDefaultClientStandardMeta(JSONObject db) throws JSONException {
        JSONObject meta = db.optJSONObject("defaultClientStandardMeta");
        if (meta == null) meta = new JSONObject();
        for (String[] base : CLIENT_META) {
            JSONObject old = meta.optJSONObject(base[0]);
            JSONObject next = new JSONObject();
            String name = old == null ? base[1] : old.optString("name", base[1]).trim();
            next.put("name", name.isEmpty() ? base[1] : name);
            next.put("unit", normalizeMaterialUnit(
                    old == null ? base[2] : old.optString("unit", base[2])));
            meta.put(base[0], next);
        }
        db.put("defaultClientStandardMeta", meta);
    }

    private static void ensureDefaultCustomItems(
            JSONObject db,
            String key,
            IdSource ids) throws JSONException {
        JSONArray input = db.optJSONArray(key);
        JSONArray output = new JSONArray();
        if (input != null) {
            for (int i = 0; i < input.length(); i++) {
                JSONObject raw = input.optJSONObject(i);
                if (raw == null) continue;
                JSONObject item = new JSONObject();
                item.put("id", emptyTo(raw.optString("id", ""), ids.next("mat")));
                String name = raw.optString("name", "Материал").trim();
                item.put("name", name.isEmpty() ? "Материал" : name);
                item.put("unit", normalizeMaterialUnit(raw.optString("unit", "шт")));
                item.put("price", nonNegative(raw.opt("price")));
                output.put(item);
            }
        }
        db.put(key, output);
    }

    private static void ensureNumberMap(JSONObject parent, String key, String[] keys)
            throws JSONException {
        JSONObject map = parent.optJSONObject(key);
        if (map == null) map = new JSONObject();
        for (String item : keys) map.put(item, nonNegative(map.opt(item)));
        parent.put(key, map);
    }

    private static JSONObject ensureObject(JSONObject parent, String key) throws JSONException {
        JSONObject object = parent.optJSONObject(key);
        if (object == null) {
            object = new JSONObject();
            parent.put(key, object);
        }
        return object;
    }

    private static JSONArray ensureArray(JSONObject parent, String key) throws JSONException {
        JSONArray array = parent.optJSONArray(key);
        if (array == null) {
            array = new JSONArray();
            parent.put(key, array);
        }
        return array;
    }

    private static void setNonNegative(JSONObject object, String key) throws JSONException {
        object.put(key, nonNegative(object.opt(key)));
    }

    private static double nonNegative(Object value) {
        return Math.max(0d, numeric(value, 0d));
    }

    private static double numeric(Object value, double fallback) {
        if (value == null || value == JSONObject.NULL) return fallback;
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return Double.isFinite(d) ? d : fallback;
        }
        try {
            double d = Double.parseDouble(String.valueOf(value).trim());
            return Double.isFinite(d) ? d : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double measurement(Object value) {
        if (value == null || value == JSONObject.NULL) return 0d;
        try {
            String raw = String.valueOf(value).trim().replace(',', '.');
            if (raw.isEmpty()) return 0d;
            double d = Double.parseDouble(raw);
            return Double.isFinite(d) ? d : 0d;
        } catch (Exception ignored) {
            return 0d;
        }
    }

    private static boolean truthyBoolean(Object value) {
        if (value == null || value == JSONObject.NULL) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0d;
        return !String.valueOf(value).isEmpty();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int length(@Nullable JSONArray array) {
        return array == null ? 0 : array.length();
    }

    private static String emptyTo(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    @Nullable
    private static String nonEmptyString(Object value) {
        if (!(value instanceof String)) return null;
        String raw = (String) value;
        return raw.isEmpty() ? null : raw;
    }

    private static Set<String> stringSet(String[] values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) set.add(value);
        return set;
    }

    private static JSONArray toArray(Set<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        return array;
    }

    private static JSONArray copyArray(@Nullable JSONArray array) throws JSONException {
        return array == null ? new JSONArray() : new JSONArray(array.toString());
    }

    @NonNull
    static JSONObject deepCopy(@NonNull JSONObject object) throws JSONException {
        return new JSONObject(object.toString());
    }

    @NonNull
    private static JSONObject deepCopyQuiet(@NonNull JSONObject object) {
        try {
            return deepCopy(object);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
