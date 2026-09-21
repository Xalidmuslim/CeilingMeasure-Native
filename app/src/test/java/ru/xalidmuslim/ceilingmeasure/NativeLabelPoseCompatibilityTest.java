package ru.xalidmuslim.ceilingmeasure;

import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

public final class NativeLabelPoseCompatibilityTest {
    private static final String FIX = "fixtures/";

    @Test
    public void importedWallManualPoseResolvesToExactAbsoluteWorldCoordinates() throws Exception {
        NativeProjectStore store = storeWithCurrentFixture();
        NativeProjectStore.RoomData room = store.loadCurrent().rooms.get(0);

        NativeLabelEngine.Placement placement = NativeLabelEngine.find(
                NativeLabelEngine.layout(room, 1f, 1f),
                NativeLabelEngine.Kind.EDGE,
                "e1");

        assertNotNull(placement);
        assertEquals(120.5f, placement.x, .0001f);
        assertEquals(75.25f, placement.y, .0001f);
        assertNotEquals(120.5f + 120.5f, placement.x, .0001f);
    }

    @Test
    public void importedDiagonalManualPoseResolvesToExactAbsoluteWorldCoordinates() throws Exception {
        NativeProjectStore store = storeWithCurrentFixture();
        NativeProjectStore.RoomData room = store.loadCurrent().rooms.get(0);

        NativeLabelEngine.Placement placement = NativeLabelEngine.find(
                NativeLabelEngine.layout(room, 1f, 1f),
                NativeLabelEngine.Kind.DIAGONAL,
                "d1");

        assertNotNull(placement);
        assertEquals(215f, placement.x, .0001f);
        assertEquals(215f, placement.y, .0001f);
    }

    @Test
    public void autoWallWithoutManualPoseKeepsExistingAutoPlacement() {
        NativeProjectStore.RoomData room = new NativeProjectStore.RoomData();
        NativeProjectStore.PointData a = new NativeProjectStore.PointData("pa", 0f, 0f);
        NativeProjectStore.PointData b = new NativeProjectStore.PointData("pb", 100f, 0f);
        room.points.add(a);
        room.points.add(b);

        NativeProjectStore.EdgeData edge =
                new NativeProjectStore.EdgeData("edge_auto", a.id, b.id, 500f);
        room.edges.add(edge);

        NativeLabelEngine.Placement placement = NativeLabelEngine.find(
                NativeLabelEngine.layout(room, 1f, 1f),
                NativeLabelEngine.Kind.EDGE,
                "edge_auto");

        assertNotNull(placement);
        assertFalse(edge.labelManual);
        assertNull(edge.labelX);
        assertNull(edge.labelY);
        assertEquals(50f, placement.x, .0001f);
        assertEquals(27f, placement.y, .0001f);
    }

    @Test
    public void importThenSerializePreservesExactManualPoseFields() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        assertTrue(store.importBackup(fixtureText("current_v184_db.json")));

        NativeProjectStore.ProjectData project = store.loadCurrent();
        store.saveCurrent(project);

        JSONObject pose = persistedWallPose(store);
        assertTrue(pose.getBoolean("manual"));
        assertEquals(120.5d, pose.getDouble("x"), .000001d);
        assertEquals(75.25d, pose.getDouble("y"), .000001d);
        assertEquals(15d, pose.getDouble("angle"), .000001d);
        assertEquals(1.2d, pose.getDouble("size"), .000001d);
    }

    @Test
    public void repeatedSaveReloadDoesNotDriftAbsoluteManualPose() throws Exception {
        NativeProjectStore store = storeWithCurrentFixture();

        for (int i = 0; i < 4; i++) {
            NativeProjectStore.ProjectData project = store.loadCurrent();
            NativeProjectStore.RoomData room = project.rooms.get(0);
            NativeLabelEngine.Placement placement = NativeLabelEngine.find(
                    NativeLabelEngine.layout(room, 1f + i, 1f),
                    NativeLabelEngine.Kind.EDGE,
                    "e1");
            assertNotNull(placement);
            assertEquals(120.5f, placement.x, .0001f);
            assertEquals(75.25f, placement.y, .0001f);
            store.saveCurrent(project);
        }

        JSONObject pose = persistedWallPose(store);
        assertEquals(120.5d, pose.getDouble("x"), .000001d);
        assertEquals(75.25d, pose.getDouble("y"), .000001d);
    }

    @Test
    public void differentAutoPlacementContextsCannotMoveImportedAbsolutePose() throws Exception {
        NativeProjectStore store = storeWithCurrentFixture();
        NativeProjectStore.RoomData room = store.loadCurrent().rooms.get(0);

        NativeLabelEngine.Placement first = NativeLabelEngine.find(
                NativeLabelEngine.layout(room, .6f, 1f),
                NativeLabelEngine.Kind.EDGE,
                "e1");
        NativeLabelEngine.Placement second = NativeLabelEngine.find(
                NativeLabelEngine.layout(room, 4f, 3f),
                NativeLabelEngine.Kind.EDGE,
                "e1");

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(120.5f, first.x, .0001f);
        assertEquals(75.25f, first.y, .0001f);
        assertEquals(first.x, second.x, .0001f);
        assertEquals(first.y, second.y, .0001f);
    }

    @Test
    public void unknownCompatibleFieldInsideLabelPoseSurvivesLoadSave() throws Exception {
        JSONObject db = fixtureObject("current_v184_db.json");
        JSONObject pose = db.getJSONArray("orders").getJSONObject(0)
                .getJSONArray("rooms").getJSONObject(0)
                .getJSONArray("edges").getJSONObject(0)
                .getJSONObject("labelPose");
        pose.put("futureCodecField", "keep-label-pose");

        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        assertTrue(store.importBackup(db.toString()));

        NativeProjectStore.ProjectData project = store.loadCurrent();
        store.saveCurrent(project);

        JSONObject persisted = persistedWallPose(store);
        assertEquals("keep-label-pose", persisted.getString("futureCodecField"));
        assertEquals(120.5d, persisted.getDouble("x"), .000001d);
        assertEquals(75.25d, persisted.getDouble("y"), .000001d);
    }

    @Test
    public void legacyRuntimeOffsetIsNeverSerializedAsAbsoluteWebPose() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData project = store.newProject();
        NativeProjectStore.RoomData room = project.rooms.get(0);

        NativeProjectStore.PointData a = new NativeProjectStore.PointData("pa", 0f, 0f);
        NativeProjectStore.PointData b = new NativeProjectStore.PointData("pb", 100f, 0f);
        room.points.add(a);
        room.points.add(b);
        NativeProjectStore.EdgeData edge =
                new NativeProjectStore.EdgeData("legacy_offset", a.id, b.id, 500f);
        edge.labelDx = 17f;
        edge.labelDy = -9f;
        edge.labelManual = true;
        room.edges.add(edge);

        store.saveCurrent(project);

        JSONObject encodedEdge = new JSONObject(store.rawDatabaseForTest())
                .getJSONArray("orders").getJSONObject(0)
                .getJSONArray("rooms").getJSONObject(0)
                .getJSONArray("edges").getJSONObject(0);
        assertFalse(encodedEdge.has("labelPose"));
    }

    private static NativeProjectStore storeWithCurrentFixture() throws Exception {
        MemorySharedPreferences prefs = new MemorySharedPreferences();
        NativeProjectStore store = new NativeProjectStore(prefs);
        assertTrue(store.importBackup(fixtureText("current_v184_db.json")));
        return store;
    }

    private static JSONObject persistedWallPose(NativeProjectStore store) throws Exception {
        return new JSONObject(store.rawDatabaseForTest())
                .getJSONArray("orders").getJSONObject(0)
                .getJSONArray("rooms").getJSONObject(0)
                .getJSONArray("edges").getJSONObject(0)
                .getJSONObject("labelPose");
    }

    private static JSONObject fixtureObject(String name) throws Exception {
        return new JSONObject(fixtureText(name));
    }

    private static String fixtureText(String name) throws Exception {
        try (InputStream in = NativeLabelPoseCompatibilityTest.class
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
}
