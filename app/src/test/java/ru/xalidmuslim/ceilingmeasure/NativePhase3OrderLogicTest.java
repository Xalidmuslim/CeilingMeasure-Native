package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public final class NativePhase3OrderLogicTest {
    @Test
    public void newOrderIsPrependedAndHasExactCanonicalDefaults() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData older = store.newProject();
        older.name = "Старый";
        store.saveCurrent(older);

        NativeProjectStore.ProjectData fresh = store.newProject();
        JSONObject db = new JSONObject(store.rawDatabaseForTest());
        JSONArray orders = db.getJSONArray("orders");
        JSONObject first = orders.getJSONObject(0);

        assertEquals(fresh.id, first.getString("id"));
        assertEquals("", first.getString("name"));
        assertEquals("", first.getString("phone"));
        assertEquals("", first.getString("address"));
        assertEquals("Замер", first.getString("status"));
        assertTrue(first.getBoolean("pricingMigratedV59"));
        assertFalse(first.has("number"));
        assertEquals(0d, first.getJSONObject("pricing").getDouble("canvas"), 0d);
        assertEquals(0d, first.getJSONObject("pricing").getDouble("baguette"), 0d);
        assertEquals(0d, first.getJSONObject("pricing").getDouble("insert"), 0d);
        assertEquals(1, first.getJSONArray("rooms").length());
        assertEquals("Комната 1",
                first.getJSONArray("rooms").getJSONObject(0).getString("name"));
        assertEquals(older.id, orders.getJSONObject(1).getString("id"));
    }

    @Test
    public void newRoomUsesPhase1Defaults() {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        store.saveDefaultPrice(false, "canvas", 111f);
        store.saveDefaultPrice(true, "canvas", 222f);
        NativeProjectStore.RoomData room = store.createRoom("Комната 7");
        assertEquals(111f, room.pricing.get("canvas"), .0001f);
        assertEquals(222f, room.clientPricing.get("canvas"), .0001f);
        assertTrue(room.point90Magnet);
        assertFalse(room.showAutoLetters);
        assertFalse(room.geometryLocked);
        assertTrue(room.validationExpanded);
        assertFalse(room.extrasExpanded);
        assertTrue(room.hintCollapsed);
        assertEquals(1f, room.viewScale, .0001f);
        assertEquals(0f, room.panX, .0001f);
        assertEquals(0f, room.panY, .0001f);
    }

    @Test
    public void orderDisplayUsesTrimOnlyForHeading() {
        assertEquals("Без имени", NativeOrderRoomLogic.orderDisplayName(""));
        assertEquals("Без имени", NativeOrderRoomLogic.orderDisplayName("   "));
        assertEquals("Иван", NativeOrderRoomLogic.orderDisplayName("  Иван  "));
    }

    @Test
    public void orderFieldsPersistUserTextExactlyWithoutNormalization() {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData p = store.newProject();
        p.name = "  Иван  ";
        p.phone = " +7 (999)  12 ";
        p.address = "  ул. Мира, 5  ";
        store.saveCurrent(p);

        NativeProjectStore.ProjectData restored = new NativeProjectStore(prefs).loadCurrent();
        assertEquals("  Иван  ", restored.name);
        assertEquals(" +7 (999)  12 ", restored.phone);
        assertEquals("  ул. Мира, 5  ", restored.address);
    }

    @Test
    public void deleteMiddleOrderLeavesOrderZeroSelected() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData a = store.newProject();
        a.name = "A"; store.saveCurrent(a);
        NativeProjectStore.ProjectData b = store.newProject();
        b.name = "B"; store.saveCurrent(b);
        NativeProjectStore.ProjectData c = store.newProject();
        c.name = "C"; store.saveCurrent(c);

        NativeProjectStore.ProjectData selected = store.deleteOrderAndLoadFirst(b.id);
        JSONObject db = new JSONObject(store.rawDatabaseForTest());
        assertEquals(2, db.getJSONArray("orders").length());
        assertEquals(c.id, selected.id);
        assertEquals(c.id, db.getJSONArray("orders").getJSONObject(0).getString("id"));
        assertEquals(a.id, db.getJSONArray("orders").getJSONObject(1).getString("id"));
    }

    @Test
    public void deletingFinalOrderRegeneratesFreshCanonicalOrder() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData only = store.newProject();
        only.name = "Удалить"; store.saveCurrent(only);

        NativeProjectStore.ProjectData fresh = store.deleteOrderAndLoadFirst(only.id);
        JSONObject db = new JSONObject(store.rawDatabaseForTest());
        assertEquals(1, db.getJSONArray("orders").length());
        assertNotEquals(only.id, fresh.id);
        assertEquals("", fresh.name);
        assertEquals("Замер", fresh.status);
        assertEquals(1, fresh.rooms.size());
        assertEquals("Комната 1", fresh.rooms.get(0).name);
    }

    @Test
    public void collapseStatePreservesUnrelatedDrawingAndEstimateKeys() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        JSONObject db = new JSONObject(store.rawDatabaseForTest());
        JSONObject state = db.getJSONObject("uiCollapse");
        state.put("drawing", true);
        state.put("estimate", true);
        store.replaceDatabaseForTest(db);

        store.setUiCollapsed("order", true);
        store.setUiCollapsed("rooms", false);

        NativeProjectStore again = new NativeProjectStore(prefs);
        assertTrue(again.isUiCollapsed("order"));
        assertFalse(again.isUiCollapsed("rooms"));
        JSONObject persisted = new JSONObject(again.rawDatabaseForTest())
                .getJSONObject("uiCollapse");
        assertTrue(persisted.getBoolean("drawing"));
        assertTrue(persisted.getBoolean("estimate"));
    }

    @Test
    public void roomSelectionRemainsRuntimeOnlyAfterPersistAndReload() {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData p = store.newProject();
        p.rooms.add(store.createRoom("Коридор"));
        String firstId = p.rooms.get(0).id;
        p.activeRoom = 1;
        store.saveCurrent(p);
        NativeProjectStore.ProjectData restored = new NativeProjectStore(prefs).loadCurrent();
        assertEquals(0, restored.activeRoom);
        assertEquals(firstId, restored.rooms.get(0).id);
    }
}
