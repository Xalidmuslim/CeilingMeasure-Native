package ru.xalidmuslim.ceilingmeasure;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;

public final class NativeProjectStoreTest {
    private static final String FIX = "fixtures/";

    @Test
    public void createOrderAndRoomPersistAcrossFullStoreReload() {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore first = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData project = first.newProject();
        project.name = "Объект";
        project.phone = "+7";
        project.address = "Адрес";
        project.rooms.get(0).name = "Зал";
        project.rooms.add(first.createRoom("Ванная"));
        first.saveCurrent(project);

        NativeProjectStore second = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData restored = second.loadCurrent();
        assertEquals(project.id, restored.id);
        assertEquals("Объект", restored.name);
        assertEquals("+7", restored.phone);
        assertEquals("Адрес", restored.address);
        assertEquals(2, restored.rooms.size());
        assertEquals("Зал", restored.rooms.get(0).name);
        assertEquals("Ванная", restored.rooms.get(1).name);
    }

    @Test
    public void selectedNonFirstOrderIsRuntimeOnlyAndRelaunchSelectsFirstOrder() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);

        NativeProjectStore.ProjectData older = store.newProject();
        older.name = "Первый созданный";
        store.saveCurrent(older);

        NativeProjectStore.ProjectData newest = store.newProject();
        newest.name = "Верхний заказ";
        store.saveCurrent(newest);

        // Simulate selecting/editing the non-first order. saveCurrent must replace it
        // in place, not promote it and not persist an active-order id.
        older.name = "Выбранный, но не первый";
        store.saveCurrent(older);

        JSONObject raw = new JSONObject(store.rawDatabaseForTest());
        assertFalse(raw.has("activeOrderId"));
        assertFalse(raw.has("orderId"));
        assertEquals(newest.id,
                raw.getJSONArray("orders").getJSONObject(0).getString("id"));

        NativeProjectStore.ProjectData relaunched =
                new NativeProjectStore(prefs).loadCurrent();
        assertEquals(newest.id, relaunched.id);
        assertEquals("Верхний заказ", relaunched.name);
    }

    @Test
    public void selectedNonFirstRoomIsRuntimeOnlyAndRelaunchSelectsFirstRoom() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData project = store.newProject();

        String firstId = project.rooms.get(0).id;
        project.rooms.get(0).name = "Первая";
        project.rooms.add(store.createRoom("Вторая"));
        project.activeRoom = 1;
        store.saveCurrent(project);

        JSONObject rawOrder = new JSONObject(store.rawDatabaseForTest())
                .getJSONArray("orders").getJSONObject(0);
        assertFalse(rawOrder.has("activeRoom"));

        NativeProjectStore.ProjectData relaunched =
                new NativeProjectStore(prefs).loadCurrent();
        assertEquals(0, relaunched.activeRoom);
        assertEquals(firstId, relaunched.rooms.get(0).id);
        assertEquals("Первая", relaunched.rooms.get(0).name);
    }

    @Test
    public void roomViewLabelPoseAndAllParityFlagsPersistExactly() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        store.replaceDatabaseForTest(fixtureObject("current_v184_db.json"));

        NativeProjectStore.ProjectData project = store.loadCurrent();
        NativeProjectStore.RoomData room = project.rooms.get(0);

        assertEquals(1.75f, room.viewScale, .0001f);
        assertEquals(12.5f, room.panX, .0001f);
        assertEquals(-8.25f, room.panY, .0001f);
        assertTrue(room.geometryLocked);
        assertFalse(room.validationExpanded);
        assertTrue(room.extrasExpanded);
        assertTrue(room.showAutoLetters);
        assertFalse(room.point90Magnet);
        assertTrue(room.hintCollapsed);

        NativeProjectStore.EdgeData edge = room.edges.get(0);
        assertTrue(edge.labelManual);
        assertNotNull(edge.labelX);
        assertNotNull(edge.labelY);
        assertEquals(120.5f, edge.labelX, .0001f);
        assertEquals(75.25f, edge.labelY, .0001f);
        assertEquals(15f, edge.labelAngle, .0001f);
        assertEquals(1.2f, edge.labelSize, .0001f);

        store.saveCurrent(project);
        JSONObject persistedRoom = new JSONObject(store.rawDatabaseForTest())
                .getJSONArray("orders").getJSONObject(0)
                .getJSONArray("rooms").getJSONObject(0);
        JSONObject pose = persistedRoom.getJSONArray("edges")
                .getJSONObject(0).getJSONObject("labelPose");
        assertTrue(pose.getBoolean("manual"));
        assertEquals(120.5d, pose.getDouble("x"), .0001d);
        assertEquals(75.25d, pose.getDouble("y"), .0001d);
        assertEquals(15d, pose.getDouble("angle"), .0001d);
        assertEquals(1.2d, pose.getDouble("size"), .0001d);
        assertEquals(1.75d, persistedRoom.getJSONObject("view").getDouble("scale"), .0001d);
        assertFalse(persistedRoom.has("viewScale"));
        assertFalse(persistedRoom.has("wallAngleMagnet"));
    }

    @Test
    public void defaultsMetadataCustomItemsAndHiddenKeysPersist() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore first = new NativeProjectStore(prefs);

        first.saveDefaultPrice(false, "canvas", 222f);
        first.saveDefaultPrice(true, "canvas", 555f);
        first.setDefaultKeyHidden(false, "pipe", true);
        first.setDefaultKeyHidden(true, "porcelain", true);

        NativeProjectStore.MaterialData purchase = new NativeProjectStore.MaterialData();
        purchase.id = "purchase_preset";
        purchase.name = "Клей";
        purchase.unit = "шт";
        purchase.price = 100f;
        first.upsertDefaultCustom(false, purchase);

        NativeProjectStore.MaterialData client = new NativeProjectStore.MaterialData();
        client.id = "client_preset";
        client.name = "Монтаж";
        client.unit = "шт";
        client.price = 150f;
        first.upsertDefaultCustom(true, client);

        NativeProjectStore second = new NativeProjectStore(prefs);
        assertEquals(222f, second.loadDefaultPricing(false).get("canvas"), .0001f);
        assertEquals(555f, second.loadDefaultPricing(true).get("canvas"), .0001f);
        assertTrue(second.loadHiddenDefaultKeys(false).contains("pipe"));
        assertTrue(second.loadHiddenDefaultKeys(true).contains("porcelain"));
        assertEquals("Клей", second.loadDefaultCustom(false).get(0).name);
        assertEquals("Монтаж", second.loadDefaultCustom(true).get(0).name);

        JSONObject db = new JSONObject(second.rawDatabaseForTest());
        JSONObject meta = db.getJSONObject("defaultClientStandardMeta");
        assertEquals("Квадратура", meta.getJSONObject("canvas").getString("name"));
        assertEquals("м²", meta.getJSONObject("canvas").getString("unit"));
    }

    @Test
    public void roomCustomItemsHiddenKeysAndCombinedEstimateIdsPersist() {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData project = store.newProject();
        NativeProjectStore.RoomData room = project.rooms.get(0);

        room.hiddenInternalKeys.add("insert");
        room.clientEstimateOverride.add("shadow");
        room.clientEstimateQty.put("shadow", 12f);

        NativeProjectStore.MaterialData purchase = new NativeProjectStore.MaterialData();
        purchase.id = "room_purchase";
        purchase.sourceId = "default_purchase";
        purchase.name = "Клей";
        purchase.qty = 3f;
        purchase.price = 99f;
        room.customItems.add(purchase);

        NativeProjectStore.MaterialData client = new NativeProjectStore.MaterialData();
        client.id = "room_client";
        client.sourceId = "default_client";
        client.name = "Монтаж";
        client.qty = 2f;
        client.price = 150f;
        room.clientCustomItems.add(client);

        project.combinedEstimateOrderIds.add("other-order");
        store.saveCurrent(project);

        NativeProjectStore.ProjectData restored =
                new NativeProjectStore(prefs).loadCurrent();
        NativeProjectStore.RoomData restoredRoom = restored.rooms.get(0);
        assertTrue(restoredRoom.hiddenInternalKeys.contains("insert"));
        assertTrue(restoredRoom.clientEstimateOverride.contains("shadow"));
        assertEquals(12f, restoredRoom.clientEstimateQty.get("shadow"), .0001f);
        assertEquals("default_purchase", restoredRoom.customItems.get(0).sourceId);
        assertEquals("default_client", restoredRoom.clientCustomItems.get(0).sourceId);
        assertEquals(1, restored.combinedEstimateOrderIds.size());
        assertEquals("other-order", restored.combinedEstimateOrderIds.get(0));
    }

    @Test
    public void explicitSavePersistsOrderAndCurrentRoomSavedAt() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData project = store.newProject();
        project.rooms.add(store.createRoom("Вторая"));
        project.activeRoom = 1;

        store.saveSnapshot(project);
        assertTrue(project.savedAt > 0L);
        assertEquals(project.savedAt, project.rooms.get(1).savedAt);

        JSONObject order = new JSONObject(store.rawDatabaseForTest())
                .getJSONArray("orders").getJSONObject(0);
        String orderSaved = order.getString("savedAt");
        String roomSaved = order.getJSONArray("rooms").getJSONObject(1).getString("savedAt");
        assertEquals(orderSaved, roomSaved);
        assertTrue(Instant.parse(orderSaved).toEpochMilli() > 0L);

        List<NativeProjectStore.ProjectData> saved =
                new NativeProjectStore(prefs).loadSaved();
        assertEquals(1, saved.size());
        assertEquals(project.id, saved.get(0).id);
    }

    @Test
    public void wrapperWebBackupImportsAndNativeExportRoundTripsV184Envelope() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);

        assertTrue(store.importBackup(fixtureText("wrapper_db.json")));
        assertEquals("Wrapper", store.loadCurrent().name);

        JSONObject exported = new JSONObject(store.exportBackup());
        assertEquals("ceilingMeasure", exported.getString("app"));
        assertEquals(165, exported.getInt("version"));
        assertTrue(exported.has("exportedAt"));
        assertTrue(exported.get("db") instanceof JSONObject);
        assertEquals("wrapper_order",
                exported.getJSONObject("db")
                        .getJSONArray("orders").getJSONObject(0).getString("id"));

        MemorySharedPreferences secondPrefs = new MemorySharedPreferences();
        NativeProjectStore second = new NativeProjectStore(secondPrefs);
        assertTrue(second.importBackup(exported.toString()));
        assertEquals("wrapper_order", second.loadCurrent().id);
        assertEquals(165, new JSONObject(second.rawDatabaseForTest()).getInt("schemaVersion"));
    }

    @Test
    public void rawWebBackupImportsWithoutEnvelope() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        assertTrue(store.importBackup(fixtureText("raw_db.json")));
        assertEquals("raw_order", store.loadCurrent().id);
    }

    @Test
    public void invalidBackupDoesNotMutateExistingStore() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData project = store.newProject();
        project.name = "Оставить";
        store.saveCurrent(project);
        JSONObject before = new JSONObject(store.rawDatabaseForTest());

        assertFalse(store.importBackup(fixtureText("invalid.json")));
        assertFalse(store.importBackup(fixtureText("no_orders.json")));

        JSONObject after = new JSONObject(store.rawDatabaseForTest());
        assertJsonSemanticEquals(before, after);
        assertEquals("Оставить", store.loadCurrent().name);
    }

    @Test
    public void unknownCompatibleFieldsSurviveNativeLoadSave() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        store.replaceDatabaseForTest(fixtureObject("current_v184_db.json"));

        NativeProjectStore.ProjectData project = store.loadCurrent();
        project.phone = "+7 111";
        store.saveCurrent(project);

        JSONObject db = new JSONObject(store.rawDatabaseForTest());
        assertEquals("keep-root", db.getString("unknownRoot"));
        JSONObject order = db.getJSONArray("orders").getJSONObject(0);
        assertEquals("keep-me", order.getString("unknownOrder"));
        assertEquals("+7 111", order.getString("phone"));
        assertEquals("keep-room",
                order.getJSONArray("rooms").getJSONObject(0).getString("unknownRoom"));
    }

    @Test
    public void canonicalPersistenceDoesNotIntroduceActiveIdsNightOrWallAngleFields()
            throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData project = store.newProject();
        project.activeRoom = 0;
        project.rooms.get(0).wallAngleMagnet = false;
        store.saveCurrent(project);

        String raw = store.rawDatabaseForTest();
        assertFalse(raw.contains("\"activeRoom\""));
        assertFalse(raw.contains("\"activeOrder\""));
        assertFalse(raw.contains("\"night\""));
        assertFalse(raw.contains("\"ui-dark\""));
        assertFalse(raw.contains("\"wallAngleMagnet\""));
    }

    @Test
    public void legacyV14PreferenceIsMigratedWhenCanonicalKeyIsAbsent() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        prefs.edit()
                .putString("ceilingTouchV14", fixtureText("legacy_v14_cm.json"))
                .commit();

        NativeProjectStore store = new NativeProjectStore(prefs);
        JSONObject room = new JSONObject(store.rawDatabaseForTest())
                .getJSONArray("orders").getJSONObject(0)
                .getJSONArray("rooms").getJSONObject(0);
        assertEquals("500", room.getJSONArray("edges").getJSONObject(0).getString("mm"));
        assertEquals(165, new JSONObject(store.rawDatabaseForTest()).getInt("schemaVersion"));
    }

    @Test
    public void uiPrefsPersistCanonicalVersion2WithoutNightMode() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        prefs.edit()
                .putString("font", "arial")
                .putFloat("scale", 1f)
                .putString("weight", "normal")
                .commit();

        NativeUiPrefs migrated = NativeUiPrefs.loadFrom(prefs);
        assertEquals("system", migrated.font);
        assertEquals(1.12f, migrated.scale, .0001f);
        assertEquals("normal", migrated.weight);

        String raw = prefs.getString("ceilingMeasureUiPrefsV1", null);
        assertNotNull(raw);
        JSONObject canonical = new JSONObject(raw);
        assertEquals(2, canonical.getInt("version"));
        assertFalse(canonical.has("night"));

        NativeUiPrefs custom = migrated
                .withFont("mono")
                .withScale(1.03f)
                .withWeight("medium");
        custom.saveTo(prefs);

        NativeUiPrefs reloaded = NativeUiPrefs.loadFrom(prefs);
        assertEquals("mono", reloaded.font);
        assertEquals(1.03f, reloaded.scale, .0001f);
        assertEquals("medium", reloaded.weight);
        assertFalse(new JSONObject(
                prefs.getString("ceilingMeasureUiPrefsV1", "{}")).has("night"));
    }

    private static JSONObject fixtureObject(String name) throws Exception {
        return new JSONObject(fixtureText(name));
    }

    private static String fixtureText(String name) throws Exception {
        try (InputStream in = NativeProjectStoreTest.class
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
            java.util.Set<String> ak = keys(a);
            java.util.Set<String> bk = keys(b);
            assertEquals(ak, bk);
            for (String key : ak) assertJsonSemanticEquals(a.get(key), b.get(key));
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
            assertEquals(((Number) expected).doubleValue(),
                    ((Number) actual).doubleValue(), .000001d);
            return;
        }
        assertEquals(expected, actual);
    }

    private static java.util.Set<String> keys(JSONObject object) {
        java.util.Set<String> result = new java.util.HashSet<>();
        java.util.Iterator<String> keys = object.keys();
        while (keys.hasNext()) result.add(keys.next());
        return result;
    }
}
