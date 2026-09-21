package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Native runtime facade over the audited v184 persistence contract.
 *
 * Runtime-only selection (active order/room) intentionally stays out of the
 * persisted JSON. The persisted payload is the same root DB shape used by web v184.
 */
final class NativeProjectStore {
    static final String PREFS = "native_ceiling_projects";

    // Canonical v184 DB is now stored under the same logical key as the web app.
    private static final String KEY_DB = NativeDataContract.DB_STORAGE_KEY;

    // Pre-Phase-1 native keys are read once only so an installed development build
    // is not silently wiped when switching to the audited canonical store.
    private static final String KEY_CURRENT = "current_project";
    private static final String KEY_SAVED = "saved_projects";
    private static final String KEY_DEFAULT_PURCHASE = "default_purchase_pricing";
    private static final String KEY_DEFAULT_CLIENT = "default_client_pricing";
    private static final String KEY_DEFAULT_PURCHASE_CUSTOM = "default_purchase_custom";
    private static final String KEY_DEFAULT_CLIENT_CUSTOM = "default_client_custom";
    private static final String KEY_HIDDEN_DEFAULT_PURCHASE = "hidden_default_purchase";
    private static final String KEY_HIDDEN_DEFAULT_CLIENT = "hidden_default_client";

    static final String[] INTERNAL_PRICE_KEYS = NativeDataContract.INTERNAL_PRICE_KEYS;
    static final String[] CLIENT_PRICE_KEYS = NativeDataContract.CLIENT_PRICE_KEYS;
    static final String[] EXTRA_QTY_KEYS = NativeDataContract.EXTRA_QTY_KEYS;
    static final String[] CLIENT_QTY_KEYS = NativeDataContract.CLIENT_QTY_KEYS;

    static String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString();
    }

    static final class PointData {
        String id;
        float x;
        float y;
        String customLabel = "";
        JSONObject raw = new JSONObject();

        PointData(float x, float y) {
            this(newId("p"), x, y);
        }

        PointData(@NonNull String id, float x, float y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    static final class EdgeData {
        String id;
        String a;
        String b;
        float value;

        // Existing native drawing runtime uses offsets. Persistence uses labelPose.x/y.
        float labelDx;
        float labelDy;
        float labelAngle;
        float labelSize = 1f;
        boolean labelManual;

        @Nullable Float labelX;
        @Nullable Float labelY;
        boolean labelPosePresent;
        @Nullable JSONObject rawLabelPose;
        @Nullable Object rawMeasurement;
        float rawMeasurementValue;
        JSONObject raw = new JSONObject();

        EdgeData(@NonNull String a, @NonNull String b) {
            this(newId("e"), a, b, 0f);
        }

        EdgeData(@NonNull String id, @NonNull String a, @NonNull String b, float value) {
            this.id = id;
            this.a = a;
            this.b = b;
            this.value = value;
        }
    }

    static final class DiagonalData {
        String id;
        String a;
        String b;
        float value;
        float labelDx;
        float labelDy;
        float labelAngle;
        float labelSize = 1f;
        boolean labelManual;

        @Nullable Float labelX;
        @Nullable Float labelY;
        boolean labelPosePresent;
        @Nullable JSONObject rawLabelPose;
        @Nullable Object rawMeasurement;
        float rawMeasurementValue;
        JSONObject raw = new JSONObject();

        DiagonalData(@NonNull String a, @NonNull String b) {
            this(newId("d"), a, b, 0f);
        }

        DiagonalData(@NonNull String id, @NonNull String a, @NonNull String b, float value) {
            this.id = id;
            this.a = a;
            this.b = b;
            this.value = value;
        }
    }

    static final class MaterialData {
        String id = newId("mat");
        String sourceId;
        String name = "Материал";
        String unit = "шт";
        float qty;
        float price;
        JSONObject raw = new JSONObject();

        MaterialData copy() {
            MaterialData m = new MaterialData();
            m.id = id;
            m.sourceId = sourceId;
            m.name = name;
            m.unit = unit;
            m.qty = qty;
            m.price = price;
            m.raw = copyJson(raw);
            return m;
        }
    }

    static final class RoomData {
        String id = newId("room");
        String name = "Комната 1";
        String notes = "";

        final ArrayList<PointData> points = new ArrayList<>();
        final ArrayList<EdgeData> edges = new ArrayList<>();
        final ArrayList<DiagonalData> diagonals = new ArrayList<>();

        final HashMap<String, Float> pricing = new HashMap<>();
        final HashMap<String, Float> clientPricing = new HashMap<>();
        final HashMap<String, Float> extraQty = new HashMap<>();
        final HashMap<String, Float> clientEstimateQty = new HashMap<>();
        final HashSet<String> clientEstimateOverride = new LinkedHashSet<>();
        final HashSet<String> hiddenInternalKeys = new LinkedHashSet<>();
        final ArrayList<MaterialData> customItems = new ArrayList<>();
        final ArrayList<MaterialData> clientCustomItems = new ArrayList<>();

        boolean point90Magnet = true;

        // v184 wall angle magnet is runtime-only. Kept for drawing implementation
        // compatibility, but it is never written to canonical persistence/export.
        boolean wallAngleMagnet = true;

        boolean showAutoLetters = false;
        boolean geometryLocked = false;
        boolean validationExpanded = true;
        boolean extrasExpanded = false;
        boolean hintCollapsed = true;

        float viewScale = 1f;
        float panX = 0f;
        float panY = 0f;
        long savedAt = 0L;

        JSONObject raw = new JSONObject();

        RoomData() {
            ensureRoomEstimateDefaults(this);
        }
    }

    static void ensureRoomEstimateDefaults(@NonNull RoomData room) {
        for (String key : INTERNAL_PRICE_KEYS) room.pricing.putIfAbsent(key, 0f);
        room.clientPricing.putIfAbsent("baguette", 0f);
        room.clientPricing.putIfAbsent("dowel", 0f);
        for (String key : CLIENT_PRICE_KEYS) room.clientPricing.putIfAbsent(key, 0f);
        for (String key : EXTRA_QTY_KEYS) room.extraQty.putIfAbsent(key, 0f);
        for (String key : CLIENT_QTY_KEYS) room.clientEstimateQty.putIfAbsent(key, 0f);
    }

    static final class ProjectData {
        String id = newId("order");
        String name = "";
        String phone = "";
        String address = "";
        String status = "Замер";

        // Native display helper only; not part of v184 persisted order schema.
        long updatedAt = System.currentTimeMillis();

        // Runtime millisecond representation of web ISO savedAt.
        long savedAt = 0L;

        // Runtime-only selection. Never serialized.
        int activeRoom = 0;

        boolean pricingMigratedV59 = true;
        final HashMap<String, Float> legacyPricing = new HashMap<>();
        final ArrayList<String> combinedEstimateOrderIds = new ArrayList<>();
        final ArrayList<RoomData> rooms = new ArrayList<>();
        JSONObject raw = new JSONObject();

        ProjectData() {
            legacyPricing.put("canvas", 0f);
            legacyPricing.put("baguette", 0f);
            legacyPricing.put("insert", 0f);
        }
    }

    private final SharedPreferences prefs;

    NativeProjectStore(@NonNull Context context) {
        this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    // Test seam: no Android runtime behavior is required for unit tests.
    NativeProjectStore(@NonNull SharedPreferences preferences) {
        prefs = preferences;
        ensureCanonicalStore();
    }

    ProjectData newProject() {
        JSONObject db = loadDb();
        ProjectData project = new ProjectData();
        project.rooms.add(createRoomFromDb("Комната 1", db));

        try {
            JSONArray orders = db.optJSONArray("orders");
            if (orders == null) {
                orders = new JSONArray();
                db.put("orders", orders);
            }
            JSONArray next = new JSONArray();
            next.put(projectToWebOrder(project));
            for (int i = 0; i < orders.length(); i++) next.put(orders.opt(i));
            db.put("orders", next);
            persistDb(db);
        } catch (Exception ignored) {
        }
        return project;
    }

    RoomData createRoom(@NonNull String name) {
        return createRoomFromDb(name, loadDb());
    }

    ProjectData loadCurrent() {
        JSONObject db = loadDb();
        JSONArray orders = db.optJSONArray("orders");
        if (orders == null || orders.length() == 0) return newProject();

        JSONObject first = orders.optJSONObject(0);
        if (first == null) return newProject();
        ProjectData result = projectFromWebOrder(first);
        result.activeRoom = 0;
        return result;
    }

    void saveCurrent(@NonNull ProjectData project) {
        project.updatedAt = System.currentTimeMillis();
        JSONObject db = loadDb();
        try {
            JSONArray orders = db.optJSONArray("orders");
            if (orders == null) orders = new JSONArray();
            JSONObject encoded = projectToWebOrder(project);

            JSONArray next = new JSONArray();
            boolean replaced = false;
            for (int i = 0; i < orders.length(); i++) {
                JSONObject old = orders.optJSONObject(i);
                if (!replaced && old != null && project.id.equals(old.optString("id", ""))) {
                    next.put(encoded);
                    replaced = true;
                } else {
                    next.put(orders.opt(i));
                }
            }
            if (!replaced) next.put(encoded);
            db.put("orders", next);
            persistDb(db);
            project.raw = copyJson(encoded);
        } catch (Exception ignored) {
        }
    }

    List<ProjectData> loadSaved() {
        ArrayList<ProjectData> result = new ArrayList<>();
        JSONObject db = loadDb();
        JSONArray orders = db.optJSONArray("orders");
        if (orders == null) return result;
        for (int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.optJSONObject(i);
            if (order == null || !hasSavedAt(order.opt("savedAt"))) continue;
            result.add(projectFromWebOrder(order));
        }
        return result;
    }

    HashMap<String, Float> loadDefaultPricing(boolean client) {
        JSONObject db = loadDb();
        JSONObject source = db.optJSONObject(client ? "defaultClientPricing" : "defaultPricing");
        HashMap<String, Float> result = new HashMap<>();
        String[] keys = client ? CLIENT_PRICE_KEYS : INTERNAL_PRICE_KEYS;
        for (String key : keys) result.put(key, 0f);
        readFloatMap(source, result);
        return result;
    }

    void saveDefaultPrice(boolean client, @NonNull String key, float value) {
        JSONObject db = loadDb();
        try {
            String rootKey = client ? "defaultClientPricing" : "defaultPricing";
            JSONObject pricing = db.optJSONObject(rootKey);
            if (pricing == null) pricing = new JSONObject();
            pricing.put(key, Math.max(0f, value));
            db.put(rootKey, pricing);
            persistDb(db);
        } catch (Exception ignored) {
        }
    }

    HashSet<String> loadHiddenDefaultKeys(boolean client) {
        JSONObject db = loadDb();
        HashSet<String> result = new LinkedHashSet<>();
        JSONArray array = db.optJSONArray(
                client ? "hiddenDefaultClientKeys" : "hiddenDefaultPurchaseKeys");
        readStringSet(array, result);
        return result;
    }

    void setDefaultKeyHidden(boolean client, @NonNull String key, boolean hidden) {
        JSONObject db = loadDb();
        String rootKey = client ? "hiddenDefaultClientKeys" : "hiddenDefaultPurchaseKeys";
        LinkedHashSet<String> values = new LinkedHashSet<>();
        readStringSet(db.optJSONArray(rootKey), values);
        if (hidden) values.add(key);
        else values.remove(key);
        try {
            db.put(rootKey, stringSetToJson(values));
            persistDb(db);
        } catch (Exception ignored) {
        }
    }

    ArrayList<MaterialData> loadDefaultCustom(boolean client) {
        JSONObject db = loadDb();
        ArrayList<MaterialData> result = new ArrayList<>();
        JSONArray array = db.optJSONArray(
                client ? "defaultClientCustomItems" : "defaultPurchaseCustomItems");
        if (array == null) return result;
        for (int i = 0; i < array.length(); i++) {
            JSONObject raw = array.optJSONObject(i);
            if (raw != null) result.add(defaultMaterialFromJson(raw));
        }
        return result;
    }

    void saveDefaultCustom(boolean client, @NonNull List<MaterialData> items) {
        JSONObject db = loadDb();
        JSONArray array = new JSONArray();
        for (MaterialData item : items) array.put(defaultMaterialToJson(item));
        try {
            db.put(client ? "defaultClientCustomItems" : "defaultPurchaseCustomItems", array);
            persistDb(db);
        } catch (Exception ignored) {
        }
    }

    void upsertDefaultCustom(boolean client, @NonNull MaterialData material) {
        ArrayList<MaterialData> items = loadDefaultCustom(client);
        boolean found = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(material.id)) {
                items.set(i, material.copy());
                found = true;
                break;
            }
        }
        if (!found) items.add(material.copy());
        saveDefaultCustom(client, items);
    }

    void deleteDefaultCustom(boolean client, @NonNull String id) {
        ArrayList<MaterialData> items = loadDefaultCustom(client);
        items.removeIf(item -> id.equals(item.id));
        saveDefaultCustom(client, items);
    }

    void applyDefaultsToRoom(@NonNull RoomData room, boolean client) {
        applyDefaultsToRoom(room, client, loadDb());
    }

    static String normalizeMaterialUnit(String unit) {
        return NativeDataContract.normalizeMaterialUnit(unit);
    }

    void saveSnapshot(@NonNull ProjectData project) {
        long now = System.currentTimeMillis();
        project.savedAt = now;
        if (!project.rooms.isEmpty()) {
            int index = Math.max(0, Math.min(project.activeRoom, project.rooms.size() - 1));
            project.rooms.get(index).savedAt = now;
        }
        saveCurrent(project);
    }

    void updateSavedProject(@NonNull ProjectData project) {
        saveCurrent(project);
    }

    void deleteSaved(@NonNull String id) {
        JSONObject db = loadDb();
        JSONArray orders = db.optJSONArray("orders");
        if (orders == null) return;
        JSONArray next = new JSONArray();
        for (int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.optJSONObject(i);
            if (order == null || !id.equals(order.optString("id", ""))) {
                next.put(orders.opt(i));
            }
        }
        try {
            db.put("orders", next);
            persistDb(db);
        } catch (Exception ignored) {
        }
    }

    String exportBackup() {
        try {
            return NativeDataContract.exportBackup(
                    loadDb(),
                    Instant.now().toString(),
                    NativeDataContract.RANDOM_IDS);
        } catch (Exception e) {
            return "{}";
        }
    }

    boolean importBackup(String raw) {
        try {
            JSONObject db = NativeDataContract.importBackup(
                    raw,
                    NativeDataContract.RANDOM_IDS);
            persistDb(db);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static String snapshotRoom(@NonNull RoomData room) {
        try {
            JSONObject json = roomToWebJson(room);
            json.put("_nativeWallAngleMagnet", room.wallAngleMagnet);
            JSONArray edges = json.optJSONArray("edges");
            if (edges != null) {
                for (int i = 0; i < room.edges.size() && i < edges.length(); i++) {
                    JSONObject item = edges.optJSONObject(i);
                    if (item == null) continue;
                    EdgeData edge = room.edges.get(i);
                    item.put("_nativeLabelDx", edge.labelDx);
                    item.put("_nativeLabelDy", edge.labelDy);
                }
            }
            JSONArray diags = json.optJSONArray("diagList");
            if (diags != null) {
                for (int i = 0; i < room.diagonals.size() && i < diags.length(); i++) {
                    JSONObject item = diags.optJSONObject(i);
                    if (item == null) continue;
                    DiagonalData diagonal = room.diagonals.get(i);
                    item.put("_nativeLabelDx", diagonal.labelDx);
                    item.put("_nativeLabelDy", diagonal.labelDy);
                }
            }
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @NonNull
    static RoomData roomFromSnapshot(String raw) {
        try {
            JSONObject json = new JSONObject(raw);
            RoomData room = roomFromWebJson(json);
            room.wallAngleMagnet = json.optBoolean("_nativeWallAngleMagnet", true);
            room.raw.remove("_nativeWallAngleMagnet");

            JSONArray edges = json.optJSONArray("edges");
            if (edges != null) {
                for (int i = 0; i < room.edges.size() && i < edges.length(); i++) {
                    JSONObject item = edges.optJSONObject(i);
                    if (item == null) continue;
                    room.edges.get(i).labelDx =
                            (float) item.optDouble("_nativeLabelDx", room.edges.get(i).labelDx);
                    room.edges.get(i).labelDy =
                            (float) item.optDouble("_nativeLabelDy", room.edges.get(i).labelDy);
                    room.edges.get(i).raw.remove("_nativeLabelDx");
                    room.edges.get(i).raw.remove("_nativeLabelDy");
                }
            }
            JSONArray diags = json.optJSONArray("diagList");
            if (diags != null) {
                for (int i = 0; i < room.diagonals.size() && i < diags.length(); i++) {
                    JSONObject item = diags.optJSONObject(i);
                    if (item == null) continue;
                    room.diagonals.get(i).labelDx =
                            (float) item.optDouble("_nativeLabelDx", room.diagonals.get(i).labelDx);
                    room.diagonals.get(i).labelDy =
                            (float) item.optDouble("_nativeLabelDy", room.diagonals.get(i).labelDy);
                    room.diagonals.get(i).raw.remove("_nativeLabelDx");
                    room.diagonals.get(i).raw.remove("_nativeLabelDy");
                }
            }
            return room;
        } catch (Exception e) {
            return new RoomData();
        }
    }

    @Nullable
    static PointData pointById(RoomData room, String id) {
        if (room == null || id == null) return null;
        for (PointData point : room.points) if (id.equals(point.id)) return point;
        return null;
    }

    static float normalizeAngle(float value) {
        float angle = value % 360f;
        if (angle < 0f) angle += 360f;
        return Math.round(angle * 10f) / 10f;
    }

    static float normalizedLabelSize(float value) {
        return Math.round(clamp(value, .55f, 2.5f) * 100f) / 100f;
    }

    // ---------------- Canonical store ----------------

    private void ensureCanonicalStore() {
        if (prefs.contains(KEY_DB)) {
            // Parse/canonicalize even when an old development build already wrote this key.
            JSONObject db = parseDatabaseString(prefs.getString(KEY_DB, null));
            persistDb(db);
            return;
        }

        NativeDataContract.SelectedStorage selected =
                NativeDataContract.selectLegacyStorage(prefs.getAll());
        if (selected != null) {
            try {
                JSONObject db = new JSONObject(selected.raw);
                db = NativeDataContract.canonicalizeDatabase(
                        db,
                        selected.unitMode,
                        NativeDataContract.RANDOM_IDS);
                persistDb(db);
                return;
            } catch (Exception ignored) {
                // Golden web behavior on malformed selected storage is an empty DB,
                // not fallback to a lower-priority legacy key.
                persistDb(NativeDataContract.newEmptyDatabase());
                return;
            }
        }

        if (prefs.contains(KEY_CURRENT) || prefs.contains(KEY_SAVED)) {
            persistDb(convertPrePhase1NativeStore());
            return;
        }

        persistDb(NativeDataContract.newEmptyDatabase());
    }

    private JSONObject loadDb() {
        String raw = prefs.getString(KEY_DB, null);
        if (raw == null || raw.isEmpty()) {
            ensureCanonicalStore();
            raw = prefs.getString(KEY_DB, null);
        }
        JSONObject db = parseDatabaseString(raw);
        try {
            JSONObject canonical = NativeDataContract.canonicalizeDatabase(
                    db,
                    NativeDataContract.LegacyUnitMode.NONE,
                    NativeDataContract.RANDOM_IDS);
            String canonicalText = canonical.toString();
            if (!canonicalText.equals(raw)) {
                prefs.edit().putString(KEY_DB, canonicalText).apply();
            }
            return canonical;
        } catch (Exception e) {
            JSONObject empty = NativeDataContract.newEmptyDatabase();
            prefs.edit().putString(KEY_DB, empty.toString()).apply();
            return empty;
        }
    }

    private void persistDb(JSONObject input) {
        try {
            JSONObject canonical = NativeDataContract.canonicalizeDatabase(
                    input,
                    NativeDataContract.LegacyUnitMode.NONE,
                    NativeDataContract.RANDOM_IDS);
            prefs.edit().putString(KEY_DB, canonical.toString()).apply();
        } catch (Exception e) {
            JSONObject empty = NativeDataContract.newEmptyDatabase();
            prefs.edit().putString(KEY_DB, empty.toString()).apply();
        }
    }

    private JSONObject parseDatabaseString(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return NativeDataContract.newEmptyDatabase();
        try {
            JSONObject parsed = new JSONObject(raw);
            if (!(parsed.opt("orders") instanceof JSONArray)) {
                parsed.put("orders", new JSONArray());
            }
            return parsed;
        } catch (Exception e) {
            return NativeDataContract.newEmptyDatabase();
        }
    }

    /**
     * One-time bridge from the repository's pre-audit native development store.
     * This is not a new web migration; it only prevents already-installed native
     * development builds from losing their local data when adopting v184 storage.
     */
    private JSONObject convertPrePhase1NativeStore() {
        JSONObject db = new JSONObject();
        JSONArray orders = new JSONArray();
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        try {
            String currentRaw = prefs.getString(KEY_CURRENT, null);
            if (currentRaw != null && !currentRaw.isEmpty()) {
                JSONObject current = legacyNativeProjectToWeb(new JSONObject(currentRaw));
                String id = current.optString("id", "");
                orders.put(current);
                if (!id.isEmpty()) seen.add(id);
            }

            JSONArray saved = new JSONArray(prefs.getString(KEY_SAVED, "[]"));
            for (int i = 0; i < saved.length(); i++) {
                JSONObject old = saved.optJSONObject(i);
                if (old == null) continue;
                JSONObject converted = legacyNativeProjectToWeb(old);
                String id = converted.optString("id", "");
                if (!id.isEmpty() && seen.contains(id)) continue;
                orders.put(converted);
                if (!id.isEmpty()) seen.add(id);
            }

            db.put("orders", orders);
            db.put("defaultPricing",
                    objectFromPreference(KEY_DEFAULT_PURCHASE));
            db.put("defaultClientPricing",
                    objectFromPreference(KEY_DEFAULT_CLIENT));
            db.put("hiddenDefaultPurchaseKeys",
                    arrayFromPreference(KEY_HIDDEN_DEFAULT_PURCHASE));
            db.put("hiddenDefaultClientKeys",
                    arrayFromPreference(KEY_HIDDEN_DEFAULT_CLIENT));
            db.put("defaultPurchaseCustomItems",
                    stripLegacyDefaultCustom(arrayFromPreference(KEY_DEFAULT_PURCHASE_CUSTOM)));
            db.put("defaultClientCustomItems",
                    stripLegacyDefaultCustom(arrayFromPreference(KEY_DEFAULT_CLIENT_CUSTOM)));

            return NativeDataContract.canonicalizeDatabase(
                    db,
                    NativeDataContract.LegacyUnitMode.NONE,
                    NativeDataContract.RANDOM_IDS);
        } catch (Exception e) {
            return NativeDataContract.newEmptyDatabase();
        }
    }

    private JSONObject objectFromPreference(String key) {
        try {
            return new JSONObject(prefs.getString(key, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private JSONArray arrayFromPreference(String key) {
        try {
            return new JSONArray(prefs.getString(key, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static JSONArray stripLegacyDefaultCustom(JSONArray source) {
        JSONArray out = new JSONArray();
        for (int i = 0; i < source.length(); i++) {
            JSONObject old = source.optJSONObject(i);
            if (old == null) continue;
            JSONObject item = new JSONObject();
            try {
                item.put("id", old.optString("id", newId("mat")));
                item.put("name", old.optString("name", "Материал"));
                item.put("unit", normalizeMaterialUnit(old.optString("unit", "шт")));
                item.put("price", Math.max(0d, old.optDouble("price", 0d)));
            } catch (JSONException ignored) {
            }
            out.put(item);
        }
        return out;
    }

    private static JSONObject legacyNativeProjectToWeb(JSONObject old) throws JSONException {
        JSONObject order = new JSONObject();
        order.put("id", old.optString("id", newId("order")));
        order.put("name", old.optString("name", ""));
        order.put("phone", old.optString("phone", ""));
        order.put("address", old.optString("address", ""));
        order.put("status", "Замер");

        JSONObject legacyPricing = new JSONObject();
        legacyPricing.put("canvas", 0d);
        legacyPricing.put("baguette", 0d);
        legacyPricing.put("insert", 0d);
        order.put("pricing", legacyPricing);
        order.put("pricingMigratedV59", true);

        long saved = old.optLong("savedAt", 0L);
        if (saved > 0L) order.put("savedAt", Instant.ofEpochMilli(saved).toString());

        JSONArray combined = old.optJSONArray("combinedEstimateOrderIds");
        order.put("combinedEstimateOrderIds", combined == null ? new JSONArray() : combined);

        JSONArray roomsOut = new JSONArray();
        JSONArray rooms = old.optJSONArray("rooms");
        if (rooms != null) {
            for (int i = 0; i < rooms.length(); i++) {
                JSONObject room = rooms.optJSONObject(i);
                if (room != null) roomsOut.put(legacyNativeRoomToWeb(room));
            }
        }
        order.put("rooms", roomsOut);
        return order;
    }

    private static JSONObject legacyNativeRoomToWeb(JSONObject old) throws JSONException {
        JSONObject room = new JSONObject();
        room.put("id", old.optString("id", newId("room")));
        room.put("name", old.optString("name", "Комната 1"));
        room.put("notes", old.optString("notes", ""));
        room.put("pricing", copyOrEmpty(old.optJSONObject("pricing")));
        room.put("clientPricing", copyOrEmpty(old.optJSONObject("clientPricing")));
        room.put("extraQty", copyOrEmpty(old.optJSONObject("extraQty")));
        room.put("clientEstimateQty", copyOrEmpty(old.optJSONObject("clientEstimateQty")));

        Object override = old.opt("clientEstimateOverride");
        JSONObject overrideObject = new JSONObject();
        if (override instanceof JSONObject) {
            overrideObject = copyJson((JSONObject) override);
        } else if (override instanceof JSONArray) {
            JSONArray a = (JSONArray) override;
            for (int i = 0; i < a.length(); i++) {
                String key = a.optString(i, "");
                if (!key.isEmpty()) overrideObject.put(key, true);
            }
        }
        room.put("clientEstimateOverride", overrideObject);

        room.put("hiddenInternalKeys",
                old.optJSONArray("hiddenInternalKeys") == null
                        ? new JSONArray()
                        : new JSONArray(old.optJSONArray("hiddenInternalKeys").toString()));
        room.put("customItems",
                old.optJSONArray("customItems") == null
                        ? new JSONArray()
                        : new JSONArray(old.optJSONArray("customItems").toString()));

        JSONArray clientCustomOut = new JSONArray();
        JSONArray clientCustom = old.optJSONArray("clientCustomItems");
        if (clientCustom != null) {
            for (int i = 0; i < clientCustom.length(); i++) {
                JSONObject item = clientCustom.optJSONObject(i);
                if (item == null) continue;
                JSONObject next = copyJson(item);
                if (!next.has("sourcePresetId") && next.has("sourceId")) {
                    next.put("sourcePresetId", next.opt("sourceId"));
                    next.remove("sourceId");
                }
                clientCustomOut.put(next);
            }
        }
        room.put("clientCustomItems", clientCustomOut);

        room.put("point90Magnet", old.optBoolean("point90Magnet", true));
        room.put("showAutoLetters", old.optBoolean("showAutoLetters", false));
        room.put("geometryLocked", old.optBoolean("geometryLocked", false));
        room.put("validationExpanded", old.optBoolean("validationExpanded", true));
        room.put("extrasExpanded", old.optBoolean("extrasExpanded", false));
        room.put("hintCollapsed", old.optBoolean("hintCollapsed", true));

        JSONObject view = new JSONObject();
        view.put("scale", old.optDouble("viewScale", 1d));
        view.put("panX", old.optDouble("panX", 0d));
        view.put("panY", old.optDouble("panY", 0d));
        room.put("view", view);

        long saved = old.optLong("savedAt", 0L);
        if (saved > 0L) room.put("savedAt", Instant.ofEpochMilli(saved).toString());

        room.put("points",
                old.optJSONArray("points") == null
                        ? new JSONArray()
                        : new JSONArray(old.optJSONArray("points").toString()));

        JSONArray edgesOut = new JSONArray();
        JSONArray edges = old.optJSONArray("edges");
        if (edges != null) {
            for (int i = 0; i < edges.length(); i++) {
                JSONObject source = edges.optJSONObject(i);
                if (source == null) continue;
                JSONObject edge = new JSONObject();
                edge.put("id", source.optString("id", newId("e")));
                edge.put("a", source.optString("a", ""));
                edge.put("b", source.optString("b", ""));
                edge.put("mm", formatMeasurement((float) source.optDouble("value", 0d)));
                boolean manual = source.optBoolean("labelManual", false);
                float dx = (float) source.optDouble("labelDx", 0d);
                float dy = (float) source.optDouble("labelDy", 0d);
                float angle = (float) source.optDouble("labelAngle", 0d);
                float size = (float) source.optDouble("labelSize", 1d);
                if (manual || Math.abs(dx) > .001f || Math.abs(dy) > .001f
                        || Math.abs(angle) > .001f || Math.abs(size - 1f) > .001f) {
                    JSONObject pose = new JSONObject();
                    pose.put("manual", manual);
                    pose.put("x", dx);
                    pose.put("y", dy);
                    pose.put("angle", angle);
                    pose.put("size", size);
                    edge.put("labelPose", pose);
                }
                edgesOut.put(edge);
            }
        }
        room.put("edges", edgesOut);

        JSONArray diagsOut = new JSONArray();
        JSONArray diags = old.optJSONArray("diagonals");
        if (diags != null) {
            for (int i = 0; i < diags.length(); i++) {
                JSONObject source = diags.optJSONObject(i);
                if (source == null) continue;
                JSONObject diagonal = new JSONObject();
                diagonal.put("id", source.optString("id", newId("d")));
                diagonal.put("a", source.optString("a", ""));
                diagonal.put("b", source.optString("b", ""));
                diagonal.put("mm", formatMeasurement((float) source.optDouble("value", 0d)));
                boolean manual = source.optBoolean("labelManual", false);
                float dx = (float) source.optDouble("labelDx", 0d);
                float dy = (float) source.optDouble("labelDy", 0d);
                float angle = (float) source.optDouble("labelAngle", 0d);
                float size = (float) source.optDouble("labelSize", 1d);
                if (manual || Math.abs(dx) > .001f || Math.abs(dy) > .001f
                        || Math.abs(angle) > .001f || Math.abs(size - 1f) > .001f) {
                    JSONObject pose = new JSONObject();
                    pose.put("manual", manual);
                    pose.put("x", dx);
                    pose.put("y", dy);
                    pose.put("angle", angle);
                    pose.put("size", size);
                    diagonal.put("labelPose", pose);
                }
                diagsOut.put(diagonal);
            }
        }
        room.put("diagList", diagsOut);
        return room;
    }

    // ---------------- Runtime model mapping ----------------

    private RoomData createRoomFromDb(String name, JSONObject db) {
        RoomData room = new RoomData();
        room.name = name;
        room.validationExpanded = true;
        room.extrasExpanded = false;
        room.showAutoLetters = false;
        room.point90Magnet = true;
        room.hintCollapsed = true;
        applyDefaultsToRoom(room, false, db);
        applyDefaultsToRoom(room, true, db);
        return room;
    }

    private void applyDefaultsToRoom(RoomData room, boolean client, JSONObject db) {
        JSONObject defaults = db.optJSONObject(client ? "defaultClientPricing" : "defaultPricing");
        HashSet<String> hidden = new LinkedHashSet<>();
        readStringSet(
                db.optJSONArray(client ? "hiddenDefaultClientKeys" : "hiddenDefaultPurchaseKeys"),
                hidden);

        if (client) {
            for (String key : CLIENT_PRICE_KEYS) {
                room.clientPricing.put(key,
                        hidden.contains(key) ? 0f : floatValue(defaults, key));
            }
            room.clientPricing.put("baguette", 0f);
            room.clientPricing.put("dowel", 0f);
            mergeCustomPresets(
                    room.clientCustomItems,
                    readDefaultCustom(db.optJSONArray("defaultClientCustomItems")),
                    true);
        } else {
            for (String key : INTERNAL_PRICE_KEYS) {
                room.pricing.put(key,
                        hidden.contains(key) ? 0f : floatValue(defaults, key));
            }
            mergeCustomPresets(
                    room.customItems,
                    readDefaultCustom(db.optJSONArray("defaultPurchaseCustomItems")),
                    false);
        }
    }

    private static void mergeCustomPresets(
            List<MaterialData> roomItems,
            List<MaterialData> presets,
            boolean client) {
        HashMap<String, MaterialData> bySource = new HashMap<>();
        ArrayList<MaterialData> adhoc = new ArrayList<>();
        for (MaterialData item : roomItems) {
            if (item.sourceId != null && !item.sourceId.isEmpty()) bySource.put(item.sourceId, item);
            else adhoc.add(item);
        }

        roomItems.clear();
        for (MaterialData preset : presets) {
            MaterialData existing = bySource.get(preset.id);
            MaterialData item = existing == null ? new MaterialData() : existing;
            if (existing == null) {
                item.id = newId("mat");
                item.sourceId = preset.id;
                item.qty = 0f;
            }
            item.name = preset.name;
            item.unit = normalizeMaterialUnit(preset.unit);
            item.price = Math.max(0f, preset.price);
            roomItems.add(item);
        }
        roomItems.addAll(adhoc);
    }

    private static ProjectData projectFromWebOrder(JSONObject source) {
        ProjectData project = new ProjectData();
        project.raw = copyJson(source);
        project.id = source.optString("id", project.id);
        project.name = source.optString("name", "");
        project.phone = source.optString("phone", "");
        project.address = source.optString("address", "");
        project.status = source.optString("status", "Замер");
        project.savedAt = parseTimestamp(source.opt("savedAt"));
        project.updatedAt = project.savedAt > 0L ? project.savedAt : System.currentTimeMillis();
        project.activeRoom = 0;
        project.pricingMigratedV59 = source.optBoolean("pricingMigratedV59", true);
        readFloatMap(source.optJSONObject("pricing"), project.legacyPricing);

        project.combinedEstimateOrderIds.clear();
        JSONArray combined = source.optJSONArray("combinedEstimateOrderIds");
        if (combined != null) {
            for (int i = 0; i < combined.length(); i++) {
                String id = combined.optString(i, "");
                if (!id.isEmpty() && !project.combinedEstimateOrderIds.contains(id)) {
                    project.combinedEstimateOrderIds.add(id);
                }
            }
        }

        project.rooms.clear();
        JSONArray rooms = source.optJSONArray("rooms");
        if (rooms != null) {
            for (int i = 0; i < rooms.length(); i++) {
                JSONObject room = rooms.optJSONObject(i);
                if (room != null) project.rooms.add(roomFromWebJson(room));
            }
        }
        if (project.rooms.isEmpty()) project.rooms.add(new RoomData());
        return project;
    }

    private static JSONObject projectToWebOrder(ProjectData project) throws JSONException {
        JSONObject out = copyJson(project.raw);
        out.remove("number");
        out.remove("savedName");
        out.remove("activeRoom");
        out.remove("updatedAt");
        out.remove("geometryVersion");

        out.put("id", project.id);
        out.put("name", project.name == null ? "" : project.name);
        out.put("phone", project.phone == null ? "" : project.phone);
        out.put("address", project.address == null ? "" : project.address);
        out.put("status", project.status == null ? "Замер" : project.status);
        out.put("pricing", floatMapToJson(project.legacyPricing));
        out.put("pricingMigratedV59", project.pricingMigratedV59);
        if (project.savedAt > 0L) {
            out.put("savedAt", Instant.ofEpochMilli(project.savedAt).toString());
        } else {
            out.remove("savedAt");
        }
        out.put("combinedEstimateOrderIds", new JSONArray(project.combinedEstimateOrderIds));

        JSONArray rooms = new JSONArray();
        for (RoomData room : project.rooms) rooms.put(roomToWebJson(room));
        out.put("rooms", rooms);
        return out;
    }

    private static RoomData roomFromWebJson(JSONObject source) {
        RoomData room = new RoomData();
        room.raw = copyJson(source);
        room.id = source.optString("id", room.id);
        room.name = source.optString("name", room.name);
        room.notes = source.optString("notes", "");

        room.pricing.clear();
        readFloatMap(source.optJSONObject("pricing"), room.pricing);
        room.clientPricing.clear();
        readFloatMap(source.optJSONObject("clientPricing"), room.clientPricing);
        room.extraQty.clear();
        readFloatMap(source.optJSONObject("extraQty"), room.extraQty);
        room.clientEstimateQty.clear();
        readFloatMap(source.optJSONObject("clientEstimateQty"), room.clientEstimateQty);
        room.clientEstimateOverride.clear();
        readStringSet(source.opt("clientEstimateOverride"), room.clientEstimateOverride);
        room.hiddenInternalKeys.clear();
        readStringSet(source.opt("hiddenInternalKeys"), room.hiddenInternalKeys);

        room.customItems.clear();
        readRoomMaterialArray(source.optJSONArray("customItems"), room.customItems, false);
        room.clientCustomItems.clear();
        readRoomMaterialArray(source.optJSONArray("clientCustomItems"), room.clientCustomItems, true);
        ensureRoomEstimateDefaults(room);

        room.point90Magnet = source.optBoolean("point90Magnet", true);
        room.wallAngleMagnet = true;
        room.showAutoLetters = source.optBoolean("showAutoLetters", false);
        room.geometryLocked = source.optBoolean("geometryLocked", false);
        room.validationExpanded = source.optBoolean("validationExpanded", true);
        room.extrasExpanded = source.optBoolean("extrasExpanded", false);
        room.hintCollapsed = source.optBoolean("hintCollapsed", true);
        room.savedAt = parseTimestamp(source.opt("savedAt"));

        JSONObject view = source.optJSONObject("view");
        room.viewScale = clamp((float) (view == null ? 1d : view.optDouble("scale", 1d)), .45f, 12f);
        room.panX = (float) (view == null ? 0d : view.optDouble("panX", 0d));
        room.panY = (float) (view == null ? 0d : view.optDouble("panY", 0d));

        room.points.clear();
        JSONArray points = source.optJSONArray("points");
        if (points != null) {
            for (int i = 0; i < points.length(); i++) {
                JSONObject raw = points.optJSONObject(i);
                if (raw == null) continue;
                PointData point = new PointData(
                        raw.optString("id", newId("p")),
                        (float) raw.optDouble("x", 0d),
                        (float) raw.optDouble("y", 0d));
                point.customLabel = "A".equals(raw.optString("customLabel", "")) ? "A" : "";
                point.raw = copyJson(raw);
                room.points.add(point);
            }
        }

        room.edges.clear();
        JSONArray edges = source.optJSONArray("edges");
        if (edges != null) {
            for (int i = 0; i < edges.length(); i++) {
                JSONObject raw = edges.optJSONObject(i);
                if (raw == null) continue;
                float measurement = parseWebMeasurement(raw.opt("mm"));
                EdgeData edge = new EdgeData(
                        raw.optString("id", newId("e")),
                        raw.optString("a", ""),
                        raw.optString("b", ""),
                        measurement);
                edge.raw = copyJson(raw);
                edge.rawMeasurement = raw.has("mm") ? raw.opt("mm") : null;
                edge.rawMeasurementValue = measurement;
                readLabelPose(raw.optJSONObject("labelPose"), edge);
                room.edges.add(edge);
            }
        }

        room.diagonals.clear();
        JSONArray diags = source.optJSONArray("diagList");
        if (diags != null) {
            for (int i = 0; i < diags.length(); i++) {
                JSONObject raw = diags.optJSONObject(i);
                if (raw == null) continue;
                float measurement = parseWebMeasurement(raw.opt("mm"));
                DiagonalData diagonal = new DiagonalData(
                        raw.optString("id", newId("d")),
                        raw.optString("a", ""),
                        raw.optString("b", ""),
                        measurement);
                diagonal.raw = copyJson(raw);
                diagonal.rawMeasurement = raw.has("mm") ? raw.opt("mm") : null;
                diagonal.rawMeasurementValue = measurement;
                readLabelPose(raw.optJSONObject("labelPose"), diagonal);
                room.diagonals.add(diagonal);
            }
        }

        NativeGeometry.sanitize(room);
        return room;
    }

    private static JSONObject roomToWebJson(RoomData room) throws JSONException {
        JSONObject out = copyJson(room.raw);
        out.remove("geometryVersion");
        out.remove("viewScale");
        out.remove("panX");
        out.remove("panY");
        out.remove("diagonals");
        out.remove("wallAngleMagnet");
        out.remove("_nativeWallAngleMagnet");

        out.put("id", room.id);
        out.put("name", room.name == null ? "Комната 1" : room.name);
        out.put("notes", room.notes == null ? "" : room.notes);
        out.put("pricing", floatMapToJson(room.pricing));
        out.put("clientPricing", floatMapToJson(room.clientPricing));
        out.put("extraQty", floatMapToJson(room.extraQty));
        out.put("clientEstimateQty", floatMapToJson(room.clientEstimateQty));

        JSONObject override = out.optJSONObject("clientEstimateOverride");
        if (override == null) override = new JSONObject();
        for (String key : CLIENT_QTY_KEYS) override.put(key, room.clientEstimateOverride.contains(key));
        out.put("clientEstimateOverride", override);
        out.put("hiddenInternalKeys", stringSetToJson(room.hiddenInternalKeys));

        JSONArray custom = new JSONArray();
        for (MaterialData item : room.customItems) custom.put(roomMaterialToJson(item, false));
        out.put("customItems", custom);

        JSONArray clientCustom = new JSONArray();
        for (MaterialData item : room.clientCustomItems) clientCustom.put(roomMaterialToJson(item, true));
        out.put("clientCustomItems", clientCustom);

        out.put("point90Magnet", room.point90Magnet);
        out.put("showAutoLetters", room.showAutoLetters);
        out.put("geometryLocked", room.geometryLocked);
        out.put("validationExpanded", room.validationExpanded);
        out.put("extrasExpanded", room.extrasExpanded);
        out.put("hintCollapsed", room.hintCollapsed);

        JSONObject view = out.optJSONObject("view");
        if (view == null) view = new JSONObject();
        view.put("scale", clamp(room.viewScale, .45f, 12f));
        view.put("panX", room.panX);
        view.put("panY", room.panY);
        out.put("view", view);

        if (room.savedAt > 0L) out.put("savedAt", Instant.ofEpochMilli(room.savedAt).toString());
        else out.remove("savedAt");

        JSONArray points = new JSONArray();
        for (PointData point : room.points) {
            JSONObject json = copyJson(point.raw);
            json.put("id", point.id);
            json.put("x", point.x);
            json.put("y", point.y);
            if ("A".equals(point.customLabel)) json.put("customLabel", "A");
            else json.remove("customLabel");
            points.put(json);
        }
        out.put("points", points);

        JSONArray edges = new JSONArray();
        for (EdgeData edge : room.edges) edges.put(edgeToWebJson(edge));
        out.put("edges", edges);

        JSONArray diagonals = new JSONArray();
        for (DiagonalData diagonal : room.diagonals) diagonals.put(diagonalToWebJson(diagonal));
        out.put("diagList", diagonals);
        return out;
    }

    private static JSONObject edgeToWebJson(EdgeData edge) throws JSONException {
        JSONObject out = copyJson(edge.raw);
        out.remove("value");
        out.remove("labelDx");
        out.remove("labelDy");
        out.remove("labelAngle");
        out.remove("labelSize");
        out.remove("labelManual");
        out.remove("_nativeLabelDx");
        out.remove("_nativeLabelDy");

        out.put("id", edge.id);
        out.put("a", edge.a);
        out.put("b", edge.b);
        out.put("mm", measurementForWrite(edge.value, edge.rawMeasurement, edge.rawMeasurementValue));
        writeLabelPose(out, edge);
        return out;
    }

    private static JSONObject diagonalToWebJson(DiagonalData diagonal) throws JSONException {
        JSONObject out = copyJson(diagonal.raw);
        out.remove("value");
        out.remove("labelDx");
        out.remove("labelDy");
        out.remove("labelAngle");
        out.remove("labelSize");
        out.remove("labelManual");
        out.remove("_nativeLabelDx");
        out.remove("_nativeLabelDy");

        out.put("id", diagonal.id);
        out.put("a", diagonal.a);
        out.put("b", diagonal.b);
        out.put("mm", measurementForWrite(
                diagonal.value,
                diagonal.rawMeasurement,
                diagonal.rawMeasurementValue));
        writeLabelPose(out, diagonal);
        return out;
    }

    private static void readLabelPose(@Nullable JSONObject pose, EdgeData edge) {
        if (pose == null) return;
        edge.labelPosePresent = true;
        edge.rawLabelPose = copyJson(pose);
        if (pose.has("x") && pose.opt("x") instanceof Number) {
            edge.labelX = (float) pose.optDouble("x", 0d);
            edge.labelDx = edge.labelX;
        }
        if (pose.has("y") && pose.opt("y") instanceof Number) {
            edge.labelY = (float) pose.optDouble("y", 0d);
            edge.labelDy = edge.labelY;
        }
        edge.labelAngle = normalizeAngle((float) pose.optDouble("angle", 0d));
        edge.labelSize = normalizedLabelSize((float) pose.optDouble("size", 1d));
        edge.labelManual = pose.optBoolean("manual", false);
    }

    private static void readLabelPose(@Nullable JSONObject pose, DiagonalData diagonal) {
        if (pose == null) return;
        diagonal.labelPosePresent = true;
        diagonal.rawLabelPose = copyJson(pose);
        if (pose.has("x") && pose.opt("x") instanceof Number) {
            diagonal.labelX = (float) pose.optDouble("x", 0d);
            diagonal.labelDx = diagonal.labelX;
        }
        if (pose.has("y") && pose.opt("y") instanceof Number) {
            diagonal.labelY = (float) pose.optDouble("y", 0d);
            diagonal.labelDy = diagonal.labelY;
        }
        diagonal.labelAngle = normalizeAngle((float) pose.optDouble("angle", 0d));
        diagonal.labelSize = normalizedLabelSize((float) pose.optDouble("size", 1d));
        diagonal.labelManual = pose.optBoolean("manual", false);
    }

    private static void writeLabelPose(JSONObject parent, EdgeData edge) throws JSONException {
        boolean include = edge.labelPosePresent
                || edge.labelManual
                || edge.labelX != null
                || edge.labelY != null
                || Math.abs(edge.labelDx) > .001f
                || Math.abs(edge.labelDy) > .001f
                || Math.abs(edge.labelAngle) > .001f
                || Math.abs(edge.labelSize - 1f) > .001f;
        if (!include) {
            parent.remove("labelPose");
            return;
        }

        JSONObject pose = edge.rawLabelPose == null
                ? new JSONObject()
                : copyJson(edge.rawLabelPose);
        pose.put("manual", edge.labelManual);
        if (edge.labelX != null) pose.put("x", edge.labelX);
        else if (edge.labelManual || Math.abs(edge.labelDx) > .001f) pose.put("x", edge.labelDx);
        if (edge.labelY != null) pose.put("y", edge.labelY);
        else if (edge.labelManual || Math.abs(edge.labelDy) > .001f) pose.put("y", edge.labelDy);
        pose.put("angle", normalizeAngle(edge.labelAngle));
        pose.put("size", normalizedLabelSize(edge.labelSize));
        parent.put("labelPose", pose);
    }

    private static void writeLabelPose(JSONObject parent, DiagonalData diagonal)
            throws JSONException {
        boolean include = diagonal.labelPosePresent
                || diagonal.labelManual
                || diagonal.labelX != null
                || diagonal.labelY != null
                || Math.abs(diagonal.labelDx) > .001f
                || Math.abs(diagonal.labelDy) > .001f
                || Math.abs(diagonal.labelAngle) > .001f
                || Math.abs(diagonal.labelSize - 1f) > .001f;
        if (!include) {
            parent.remove("labelPose");
            return;
        }

        JSONObject pose = diagonal.rawLabelPose == null
                ? new JSONObject()
                : copyJson(diagonal.rawLabelPose);
        pose.put("manual", diagonal.labelManual);
        if (diagonal.labelX != null) pose.put("x", diagonal.labelX);
        else if (diagonal.labelManual || Math.abs(diagonal.labelDx) > .001f) {
            pose.put("x", diagonal.labelDx);
        }
        if (diagonal.labelY != null) pose.put("y", diagonal.labelY);
        else if (diagonal.labelManual || Math.abs(diagonal.labelDy) > .001f) {
            pose.put("y", diagonal.labelDy);
        }
        pose.put("angle", normalizeAngle(diagonal.labelAngle));
        pose.put("size", normalizedLabelSize(diagonal.labelSize));
        parent.put("labelPose", pose);
    }

    private static MaterialData defaultMaterialFromJson(JSONObject source) {
        MaterialData item = new MaterialData();
        item.raw = copyJson(source);
        item.id = source.optString("id", item.id);
        item.sourceId = null;
        item.name = source.optString("name", "Материал").trim();
        if (item.name.isEmpty()) item.name = "Материал";
        item.unit = normalizeMaterialUnit(source.optString("unit", "шт"));
        item.qty = 0f;
        item.price = Math.max(0f, (float) source.optDouble("price", 0d));
        return item;
    }

    private static JSONObject defaultMaterialToJson(MaterialData item) {
        JSONObject out = copyJson(item.raw);
        try {
            out.remove("sourceId");
            out.remove("sourcePresetId");
            out.remove("qty");
            out.put("id", item.id);
            out.put("name", cleanMaterialName(item.name));
            out.put("unit", normalizeMaterialUnit(item.unit));
            out.put("price", Math.max(0f, item.price));
        } catch (JSONException ignored) {
        }
        return out;
    }

    private static void readRoomMaterialArray(
            @Nullable JSONArray array,
            List<MaterialData> out,
            boolean client) {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            JSONObject source = array.optJSONObject(i);
            if (source == null) continue;
            MaterialData item = new MaterialData();
            item.raw = copyJson(source);
            item.id = source.optString("id", item.id);
            item.sourceId = source.optString(client ? "sourcePresetId" : "sourceId", "");
            if (item.sourceId != null && item.sourceId.isEmpty()) item.sourceId = null;
            item.name = cleanMaterialName(source.optString("name", "Материал"));
            item.unit = normalizeMaterialUnit(source.optString("unit", "шт"));
            item.qty = Math.max(0f, (float) source.optDouble("qty", 0d));
            item.price = Math.max(0f, (float) source.optDouble("price", 0d));
            out.add(item);
        }
    }

    private static JSONObject roomMaterialToJson(MaterialData item, boolean client) {
        JSONObject out = copyJson(item.raw);
        try {
            String sourceKey = client ? "sourcePresetId" : "sourceId";
            String otherKey = client ? "sourceId" : "sourcePresetId";
            out.remove(otherKey);
            out.put("id", item.id);
            if (item.sourceId == null || item.sourceId.isEmpty()) {
                out.put(sourceKey, JSONObject.NULL);
            } else {
                out.put(sourceKey, item.sourceId);
            }
            out.put("name", cleanMaterialName(item.name));
            out.put("unit", normalizeMaterialUnit(item.unit));
            out.put("qty", Math.max(0f, item.qty));
            out.put("price", Math.max(0f, item.price));
        } catch (JSONException ignored) {
        }
        return out;
    }

    private static ArrayList<MaterialData> readDefaultCustom(@Nullable JSONArray array) {
        ArrayList<MaterialData> result = new ArrayList<>();
        if (array == null) return result;
        for (int i = 0; i < array.length(); i++) {
            JSONObject source = array.optJSONObject(i);
            if (source != null) result.add(defaultMaterialFromJson(source));
        }
        return result;
    }

    private static JSONObject floatMapToJson(Map<String, Float> map) {
        JSONObject out = new JSONObject();
        if (map == null) return out;
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            try {
                out.put(entry.getKey(),
                        Math.max(0f, entry.getValue() == null ? 0f : entry.getValue()));
            } catch (JSONException ignored) {
            }
        }
        return out;
    }

    private static void readFloatMap(@Nullable JSONObject source, Map<String, Float> out) {
        if (source == null || out == null) return;
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            out.put(key, Math.max(0f, (float) source.optDouble(key, 0d)));
        }
    }

    private static JSONArray stringSetToJson(Iterable<String> values) {
        JSONArray array = new JSONArray();
        if (values != null) {
            for (String value : values) array.put(value);
        }
        return array;
    }

    private static void readStringSet(@Nullable Object raw, java.util.Set<String> out) {
        if (raw == null || raw == JSONObject.NULL || out == null) return;
        if (raw instanceof JSONArray) {
            JSONArray array = (JSONArray) raw;
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "");
                if (!value.isEmpty()) out.add(value);
            }
            return;
        }
        if (raw instanceof JSONObject) {
            JSONObject object = (JSONObject) raw;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (object.optBoolean(key, false)) out.add(key);
            }
        }
    }

    private static float floatValue(@Nullable JSONObject source, String key) {
        return source == null ? 0f : Math.max(0f, (float) source.optDouble(key, 0d));
    }

    private static Object measurementForWrite(
            float current,
            @Nullable Object raw,
            float originalValue) {
        if (raw != null && Math.abs(current - originalValue) < .0001f) return raw;
        return formatMeasurement(current);
    }

    private static String formatMeasurement(float value) {
        if (!(value > 0f)) return "";
        if (Math.abs(value - Math.round(value)) < .0001f) {
            return String.valueOf(Math.round(value));
        }
        return new BigDecimal(Float.toString(value)).stripTrailingZeros().toPlainString();
    }

    private static float parseWebMeasurement(Object value) {
        if (value == null || value == JSONObject.NULL) return 0f;
        try {
            String raw = String.valueOf(value).trim().replace(',', '.');
            if (raw.isEmpty()) return 0f;
            return Math.max(0f, Float.parseFloat(raw));
        } catch (Exception ignored) {
            return 0f;
        }
    }

    private static boolean hasSavedAt(Object value) {
        if (value == null || value == JSONObject.NULL) return false;
        if (value instanceof Number) return ((Number) value).longValue() > 0L;
        return !String.valueOf(value).trim().isEmpty();
    }

    private static long parseTimestamp(Object value) {
        if (value == null || value == JSONObject.NULL) return 0L;
        if (value instanceof Number) return Math.max(0L, ((Number) value).longValue());
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) return 0L;
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (Exception ignored) {
            try {
                return Math.max(0L, Long.parseLong(raw));
            } catch (Exception ignoredAgain) {
                return 0L;
            }
        }
    }

    private static String cleanMaterialName(String value) {
        String name = value == null ? "" : value.trim();
        return name.isEmpty() ? "Материал" : name;
    }

    private static JSONObject copyOrEmpty(@Nullable JSONObject source) {
        return source == null ? new JSONObject() : copyJson(source);
    }

    private static JSONObject copyJson(@Nullable JSONObject source) {
        if (source == null) return new JSONObject();
        try {
            return new JSONObject(source.toString());
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // Test evidence helpers. They expose canonical text only, never selection state.
    String rawDatabaseForTest() {
        return loadDb().toString();
    }

    void replaceDatabaseForTest(@NonNull JSONObject db) {
        persistDb(db);
    }
}
