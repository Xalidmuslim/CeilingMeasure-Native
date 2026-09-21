package ru.xalidmuslim.ceilingmeasure;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public final class NativeDataContractTest {
    private static final String FIX = "fixtures/";

    private static final class DeterministicIds implements NativeDataContract.IdSource {
        private final AtomicInteger next = new AtomicInteger(1);
        @Override public String next(String prefix) {
            return prefix + "_test_" + next.getAndIncrement();
        }
    }

    @Test
    public void currentV184CanonicalRoundTripPreservesSemanticDataAndUnknownFields() throws Exception {
        JSONObject source = fixtureObject("current_v184_db.json");
        JSONObject canonical = NativeDataContract.canonicalizeDatabase(
                source,
                NativeDataContract.LegacyUnitMode.NONE,
                new DeterministicIds());

        assertEquals(165, canonical.getInt("schemaVersion"));
        assertEquals("keep-root", canonical.getString("unknownRoot"));
        JSONObject order = canonical.getJSONArray("orders").getJSONObject(0);
        assertEquals("keep-me", order.getString("unknownOrder"));
        JSONObject room = order.getJSONArray("rooms").getJSONObject(0);
        assertEquals("keep-room", room.getString("unknownRoom"));

        String exported = NativeDataContract.exportBackup(
                canonical,
                "2026-09-21T20:00:00.000Z",
                new DeterministicIds());
        JSONObject envelope = new JSONObject(exported);
        assertEquals("ceilingMeasure", envelope.getString("app"));
        assertEquals(165, envelope.getInt("version"));
        assertEquals("2026-09-21T20:00:00.000Z", envelope.getString("exportedAt"));

        JSONObject imported = NativeDataContract.importBackup(
                exported,
                new DeterministicIds());
        assertJsonSemanticEquals(canonical, imported);
    }

    @Test
    public void wrapperBackupImportsWebDb() throws Exception {
        JSONObject db = NativeDataContract.importBackup(
                fixtureText("wrapper_db.json"),
                new DeterministicIds());
        assertEquals("wrapper_order", db.getJSONArray("orders").getJSONObject(0).getString("id"));
        assertEquals(165, db.getInt("schemaVersion"));
    }

    @Test
    public void rawBackupImportsWebDb() throws Exception {
        JSONObject db = NativeDataContract.importBackup(
                fixtureText("raw_db.json"),
                new DeterministicIds());
        assertEquals("raw_order", db.getJSONArray("orders").getJSONObject(0).getString("id"));
        assertEquals(165, db.getInt("schemaVersion"));
    }

    @Test(expected = JSONException.class)
    public void invalidJsonBackupIsRejected() throws Exception {
        NativeDataContract.importBackup(
                fixtureText("invalid.json"),
                new DeterministicIds());
    }

    @Test(expected = JSONException.class)
    public void backupWithoutOrdersArrayIsRejected() throws Exception {
        NativeDataContract.importBackup(
                fixtureText("no_orders.json"),
                new DeterministicIds());
    }

    @Test
    public void legacyStorageSelectionUsesExactWebPrecedence() {
        Map<String,Object> values = new LinkedHashMap<>();
        values.put("ceilingTouchV14", "{\"orders\":[]}");
        values.put("ceilingTouchV125", "{\"orders\":[{\"id\":\"v125\"}]}");
        values.put("ceilingTouchV12", "{\"orders\":[{\"id\":\"v12\"}]}");

        NativeDataContract.SelectedStorage selected =
                NativeDataContract.selectLegacyStorage(values);
        assertNotNull(selected);
        assertEquals("ceilingTouchV125", selected.key);
        assertEquals(NativeDataContract.LegacyUnitMode.NONE, selected.unitMode);

        values.put("ceilingMeasureData", "{\"orders\":[{\"id\":\"current\"}]}");
        selected = NativeDataContract.selectLegacyStorage(values);
        assertNotNull(selected);
        assertEquals("ceilingMeasureData", selected.key);
        assertEquals(NativeDataContract.LegacyUnitMode.NONE, selected.unitMode);
    }

    @Test
    public void legacyV14CmValuesScaleByTenAndRoomNameMigrates() throws Exception {
        JSONObject db = NativeDataContract.canonicalizeDatabase(
                fixtureObject("legacy_v14_cm.json"),
                NativeDataContract.LegacyUnitMode.CM_X10,
                new DeterministicIds());

        JSONObject room = firstRoom(db);
        assertEquals("Другое 2", room.getString("name"));
        assertEquals("500", room.getJSONArray("edges").getJSONObject(0).getString("mm"));
        assertEquals("700", room.getJSONArray("diagList").getJSONObject(0).getString("mm"));
        assertFalse(room.getBoolean("showAutoLetters"));
        assertTrue(room.getBoolean("hintCollapsed"));
        assertTrue(room.getBoolean("extrasExpanded"));
        assertEquals(165, db.getInt("schemaVersion"));
    }

    @Test
    public void oldMmLegacyPathDoesNotScaleMeasurements() throws Exception {
        JSONObject db = NativeDataContract.canonicalizeDatabase(
                fixtureObject("old_mm_v12.json"),
                NativeDataContract.LegacyUnitMode.OLD_MM,
                new DeterministicIds());

        JSONObject room = firstRoom(db);
        assertEquals("500", room.getJSONArray("edges").getJSONObject(0).getString("mm"));
    }

    @Test
    public void legacyDrawingsFlattenActiveDrawingIntoRoomRoot() throws Exception {
        JSONObject db = NativeDataContract.canonicalizeDatabase(
                fixtureObject("legacy_drawings.json"),
                NativeDataContract.LegacyUnitMode.NONE,
                new DeterministicIds());

        JSONObject room = firstRoom(db);
        assertFalse(room.has("drawings"));
        assertFalse(room.has("activeDrawingId"));
        assertEquals("Из drawing", room.getString("notes"));
        assertEquals(2d, room.getJSONObject("view").getDouble("scale"), 0.0001d);
        assertEquals(2, room.getJSONArray("points").length());
        assertEquals(1, room.getJSONArray("edges").length());
    }

    @Test
    public void legacyGeometryConvertsToPointIdsEdgesAndDiagList() throws Exception {
        JSONObject db = NativeDataContract.canonicalizeDatabase(
                fixtureObject("legacy_geometry.json"),
                NativeDataContract.LegacyUnitMode.NONE,
                new DeterministicIds());

        JSONObject room = firstRoom(db);
        JSONArray points = room.getJSONArray("points");
        JSONArray edges = room.getJSONArray("edges");
        JSONArray diags = room.getJSONArray("diagList");
        assertEquals(3, points.length());
        assertEquals(2, edges.length());
        assertEquals(1, diags.length());
        assertTrue(points.getJSONObject(0).getString("id").startsWith("p_test_"));
        assertFalse(room.has("walls"));
        assertFalse(room.has("diags"));
        assertFalse(room.has("closed"));
    }

    @Test
    public void pricingV59CopiesLegacyOrderPricingAndResetsRoomExtras() throws Exception {
        JSONObject db = NativeDataContract.canonicalizeDatabase(
                fixtureObject("pricing_v59.json"),
                NativeDataContract.LegacyUnitMode.NONE,
                new DeterministicIds());

        JSONObject order = db.getJSONArray("orders").getJSONObject(0);
        JSONObject room = order.getJSONArray("rooms").getJSONObject(0);
        JSONObject pricing = room.getJSONObject("pricing");
        assertEquals(11d, pricing.getDouble("canvas"), 0.0001d);
        assertEquals(22d, pricing.getDouble("baguette"), 0.0001d);
        assertEquals(33d, pricing.getDouble("insert"), 0.0001d);
        assertEquals(0d, room.getJSONObject("extraQty").getDouble("light"), 0.0001d);
        assertTrue(order.getBoolean("pricingMigratedV59"));
    }

    @Test
    public void objectCleanupKeepsManualCalculatorQuantities() throws Exception {
        JSONObject db = baseDbWithOneRoom();
        JSONObject room = firstRoom(db);
        room.put("objects", new JSONArray().put(new JSONObject().put("type", "light")));
        room.put("objectAutoQty", new JSONObject().put("light", 99));
        room.put("lightAutoLayout", true);
        room.put("extraQty", new JSONObject()
                .put("light", 7)
                .put("corner", 2)
                .put("cornice", 3)
                .put("lightline", 4)
                .put("pipe", 1));

        JSONObject canonical = NativeDataContract.canonicalizeDatabase(
                db,
                NativeDataContract.LegacyUnitMode.NONE,
                new DeterministicIds());
        JSONObject result = firstRoom(canonical);
        assertFalse(result.has("objects"));
        assertFalse(result.has("objectAutoQty"));
        assertFalse(result.has("lightAutoLayout"));
        assertEquals(7d, result.getJSONObject("extraQty").getDouble("light"), 0.0001d);
    }

    @Test
    public void defaultClientPricingV146SeedsFromFirstLegacyRoom() throws Exception {
        JSONObject db = baseDbWithOneRoom();
        db.remove("defaultClientPricing");
        db.remove("defaultClientPricingMigratedV146");
        JSONObject room = firstRoom(db);
        room.put("clientPricing", new JSONObject()
                .put("canvas", 777)
                .put("insert", 55)
                .put("shadow", 12));

        JSONObject canonical = NativeDataContract.canonicalizeDatabase(
                db,
                NativeDataContract.LegacyUnitMode.NONE,
                new DeterministicIds());
        JSONObject defaults = canonical.getJSONObject("defaultClientPricing");
        assertEquals(777d, defaults.getDouble("canvas"), 0.0001d);
        assertEquals(55d, defaults.getDouble("insert"), 0.0001d);
        assertEquals(12d, defaults.getDouble("shadow"), 0.0001d);
        assertTrue(canonical.getBoolean("defaultClientPricingMigratedV146"));
    }

    @Test
    public void canonicalPersistRemovesLegacyOrderNumberAndSavedName() throws Exception {
        JSONObject db = baseDbWithOneRoom();
        JSONObject order = db.getJSONArray("orders").getJSONObject(0);
        order.put("number", "189");
        order.put("savedName", "old");
        order.put("futureCompatible", new JSONObject().put("x", 1));

        JSONObject canonical = NativeDataContract.canonicalizeDatabase(
                db,
                NativeDataContract.LegacyUnitMode.NONE,
                new DeterministicIds());
        JSONObject result = canonical.getJSONArray("orders").getJSONObject(0);
        assertFalse(result.has("number"));
        assertFalse(result.has("savedName"));
        assertEquals(1, result.getJSONObject("futureCompatible").getInt("x"));
    }

    @Test
    public void uiPrefsOldDefaultArialMigratesToSystem112Version2() throws Exception {
        JSONObject prefs = NativeDataContract.canonicalizeUiPrefs(
                fixtureObject("ui_prefs_old_default.json"));
        assertEquals(2, prefs.getInt("version"));
        assertEquals("system", prefs.getString("font"));
        assertEquals(1.12d, prefs.getDouble("scale"), 0.0001d);
        assertEquals("normal", prefs.getString("weight"));
        assertFalse(prefs.has("night"));
    }

    @Test
    public void uiPrefsGeorgiaAliasAndLegacyLargeSizeMigrate() throws Exception {
        JSONObject prefs = NativeDataContract.canonicalizeUiPrefs(
                fixtureObject("ui_prefs_georgia.json"));
        assertEquals("serif", prefs.getString("font"));
        assertEquals(1.12d, prefs.getDouble("scale"), 0.0001d);
        assertEquals("bold", prefs.getString("weight"));
    }

    @Test
    public void uiPrefsV2Explicit100PercentStays100AndNightIsDropped() throws Exception {
        JSONObject prefs = NativeDataContract.canonicalizeUiPrefs(
                fixtureObject("ui_prefs_v2_custom.json"));
        assertEquals(2, prefs.getInt("version"));
        assertEquals("mono", prefs.getString("font"));
        assertEquals(1d, prefs.getDouble("scale"), 0.0001d);
        assertEquals("medium", prefs.getString("weight"));
        assertFalse(prefs.has("night"));
    }

    @Test
    public void clientCustomSourcePresetIdAndAllPersistedFlagsSurviveCanonicalization()
            throws Exception {
        JSONObject db = NativeDataContract.canonicalizeDatabase(
                fixtureObject("current_v184_db.json"),
                NativeDataContract.LegacyUnitMode.NONE,
                new DeterministicIds());
        JSONObject room = firstRoom(db);

        assertTrue(room.getBoolean("geometryLocked"));
        assertFalse(room.getBoolean("validationExpanded"));
        assertTrue(room.getBoolean("extrasExpanded"));
        assertTrue(room.getBoolean("showAutoLetters"));
        assertFalse(room.getBoolean("point90Magnet"));
        assertTrue(room.getBoolean("hintCollapsed"));

        JSONObject custom = room.getJSONArray("clientCustomItems").getJSONObject(0);
        assertEquals("cp1", custom.getString("sourcePresetId"));
        assertFalse(custom.has("sourceId"));
    }

    private static JSONObject baseDbWithOneRoom() throws Exception {
        JSONObject db = fixtureObject("current_v184_db.json");
        JSONArray one = new JSONArray();
        one.put(db.getJSONArray("orders").getJSONObject(0));
        db.put("orders", one);
        return db;
    }

    private static JSONObject firstRoom(JSONObject db) throws Exception {
        return db.getJSONArray("orders")
                .getJSONObject(0)
                .getJSONArray("rooms")
                .getJSONObject(0);
    }

    private static JSONObject fixtureObject(String name) throws Exception {
        return new JSONObject(fixtureText(name));
    }

    private static String fixtureText(String name) throws Exception {
        try (InputStream in = NativeDataContractTest.class
                .getClassLoader()
                .getResourceAsStream(FIX + name)) {
            assertNotNull("Missing fixture " + name, in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void assertJsonSemanticEquals(Object expected, Object actual)
            throws Exception {
        if (expected == JSONObject.NULL || actual == JSONObject.NULL) {
            assertSame(expected, actual);
            return;
        }
        if (expected instanceof JSONObject && actual instanceof JSONObject) {
            JSONObject a = (JSONObject) expected;
            JSONObject b = (JSONObject) actual;
            assertEquals(keyMap(a), keyMap(b));
            for (String key : keyMap(a).keySet()) {
                assertJsonSemanticEquals(a.get(key), b.get(key));
            }
            return;
        }
        if (expected instanceof JSONArray && actual instanceof JSONArray) {
            JSONArray a = (JSONArray) expected;
            JSONArray b = (JSONArray) actual;
            assertEquals(a.length(), b.length());
            for (int i = 0; i < a.length(); i++) {
                assertJsonSemanticEquals(a.get(i), b.get(i));
            }
            return;
        }
        if (expected instanceof Number && actual instanceof Number) {
            assertEquals(
                    ((Number) expected).doubleValue(),
                    ((Number) actual).doubleValue(),
                    0.000001d);
            return;
        }
        assertEquals(expected, actual);
    }

    private static Map<String,Boolean> keyMap(JSONObject object) {
        Map<String,Boolean> result = new HashMap<>();
        java.util.Iterator<String> keys = object.keys();
        while (keys.hasNext()) result.put(keys.next(), true);
        return result;
    }
}
