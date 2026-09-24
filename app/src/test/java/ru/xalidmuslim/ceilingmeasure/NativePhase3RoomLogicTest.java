package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;

public final class NativePhase3RoomLogicTest {
    @Test
    public void roomTypesAreExactlyGoldenChoices() {
        assertArrayEquals(new String[]{"Комната","Коридор","Ванная","Другое"},
                NativeOrderRoomLogic.ROOM_TYPES);
    }

    @Test
    public void roomTypeNumberingMatchesWebWithGapsAndCustomNames() {
        assertEquals("Комната 4", NativeOrderRoomLogic.nextRoomTypeName(
                Arrays.asList(room("Комната 1"), room("Комната 3"), room("Комната гости")),
                "Комната"));
        assertEquals("Коридор", NativeOrderRoomLogic.nextRoomTypeName(
                Arrays.asList(room("Комната 1")), "Коридор"));
        assertEquals("Коридор 2", NativeOrderRoomLogic.nextRoomTypeName(
                Arrays.asList(room("Коридор")), "Коридор"));
        assertEquals("Коридор 5", NativeOrderRoomLogic.nextRoomTypeName(
                Arrays.asList(room("Коридор"), room("Коридор 4")), "Коридор"));
        assertEquals("Ванная", NativeOrderRoomLogic.nextRoomTypeName(
                Arrays.asList(room("Ванная большая")), "Ванная"));
        assertEquals("Другое", NativeOrderRoomLogic.nextRoomTypeName(
                Arrays.asList(room("Другое помещение")), "Другое"));
    }

    @Test
    public void duplicateNamingUsesFirstFreeSuffix() {
        NativeProjectStore.RoomData src = room("Зал");
        assertEquals("Зал копия", NativeOrderRoomLogic.nextDuplicateName(
                Arrays.asList(src), src.name));
        assertEquals("Зал копия 3", NativeOrderRoomLogic.nextDuplicateName(
                Arrays.asList(src, room("Зал копия"), room("Зал копия 2"), room("Зал копия 4")),
                src.name));
    }

    @Test
    public void duplicateDeepCopiesGeometryLabelsPricingMaterialsAndFlags() throws Exception {
        NativeProjectStore.RoomData src = room("Зал");
        src.id = "source";
        src.savedAt = 123L;
        src.geometryLocked = true;
        src.notes = "note";
        src.viewScale = 1.7f;
        src.panX = 11f;
        src.panY = -8f;
        src.raw.put("future", "keep");

        NativeProjectStore.PointData p = new NativeProjectStore.PointData("p0", 10f, 20f);
        p.customLabel = "A";
        src.points.add(p);
        src.points.add(new NativeProjectStore.PointData("p1", 100f, 20f));

        NativeProjectStore.EdgeData e = new NativeProjectStore.EdgeData("e0","p0","p1",500f);
        e.labelManual = true;
        e.labelX = 55f;
        e.labelY = 66f;
        e.labelAngle = 37f;
        e.labelSize = 1.4f;
        e.labelPosePresent = true;
        e.rawLabelPose = new JSONObject().put("futurePose","keep");
        e.raw.put("futureEdge", true);
        src.edges.add(e);

        NativeProjectStore.DiagonalData d =
                new NativeProjectStore.DiagonalData("d0","p0","p1",707f);
        d.labelManual = true;
        d.labelX = 80f;
        d.labelY = 90f;
        d.labelAngle = 120f;
        src.diagonals.add(d);

        src.pricing.put("canvas",321f);
        src.clientPricing.put("canvas",777f);
        src.extraQty.put("light",4f);
        src.clientEstimateQty.put("light",5f);
        src.clientEstimateOverride.add("light");
        src.hiddenInternalKeys.add("pipe");

        NativeProjectStore.MaterialData m = new NativeProjectStore.MaterialData();
        m.id="m1"; m.sourceId="preset"; m.name="Клей"; m.qty=2f; m.price=99f;
        src.customItems.add(m);

        NativeProjectStore.RoomData copy = NativeOrderRoomLogic.duplicateRoom(
                src, Arrays.asList(src,room("Зал копия")), "copy");

        assertEquals("copy", copy.id);
        assertEquals("Зал копия 2", copy.name);
        assertEquals(0L, copy.savedAt);
        assertFalse(copy.geometryLocked);
        assertEquals(src.notes, copy.notes);
        assertEquals(500f, copy.edges.get(0).value, .0001f);
        assertEquals(55f, copy.edges.get(0).labelX, .0001f);
        assertEquals(37f, copy.edges.get(0).labelAngle, .0001f);
        assertEquals(707f, copy.diagonals.get(0).value, .0001f);
        assertEquals(321f, copy.pricing.get("canvas"), .0001f);
        assertEquals(4f, copy.extraQty.get("light"), .0001f);
        assertEquals("Клей", copy.customItems.get(0).name);
        assertEquals("keep", copy.raw.getString("future"));
        assertEquals("keep", copy.edges.get(0).rawLabelPose.getString("futurePose"));

        copy.points.get(0).x=999f;
        copy.edges.get(0).raw.put("futureEdge",false);
        copy.customItems.get(0).name="changed";
        assertEquals(10f,src.points.get(0).x,.0001f);
        assertTrue(src.edges.get(0).raw.getBoolean("futureEdge"));
        assertEquals("Клей",src.customItems.get(0).name);
        assertEquals(123L,src.savedAt);
        assertTrue(src.geometryLocked);
    }

    @Test
    public void roomKeyboardRowsExactlyMatchGolden() {
        assertArrayEquals(new String[]{"1","2","3","4","5","6","7","8","9","0"},
                NativeOrderRoomLogic.ROOM_KEY_ROWS[0]);
        assertArrayEquals(new String[]{"й","ц","у","к","е","н","г","ш","щ","з","х"},
                NativeOrderRoomLogic.ROOM_KEY_ROWS[1]);
        assertArrayEquals(new String[]{"ф","ы","в","а","п","р","о","л","д","ж","э"},
                NativeOrderRoomLogic.ROOM_KEY_ROWS[2]);
        assertArrayEquals(new String[]{"я","ч","с","м","и","т","ь","б","ю","ё"},
                NativeOrderRoomLogic.ROOM_KEY_ROWS[3]);
    }

    @Test
    public void keyboardCapitalizesAtStartAfterSpaceAndHyphen() {
        assertEquals("Й",key("", "й", null));
        assertEquals("  Й",key("  ", "й", null));
        assertEquals("Зал Й",key("Зал ","й",null));
        assertEquals("Зал-Й",key("Зал-","й",null));
        assertEquals("Залй",key("Зал","й",null));
    }

    @Test
    public void keyboardControlsMatchGolden() {
        assertEquals("",key("",null,"space"));
        assertEquals("Зал ",key("Зал",null,"space"));
        assertEquals("Зал ",key("Зал ",null,"space"));
        assertEquals("Зал-",key("Зал",null,"dash"));
        assertEquals("За",key("Зал",null,"back"));
        assertEquals("",key("Зал",null,"clear"));
    }

    @Test
    public void keyboardEnforcesFortyCharacterLimit() {
        String forty="1234567890123456789012345678901234567890";
        assertEquals(40,forty.length());
        assertEquals(forty,key(forty,"й",null));
        assertEquals(forty,key(forty,null,"dash"));
        assertEquals(40,key(forty.substring(0,39),"1",null).length());
    }

    @Test
    public void longPressThresholdAndMovementAreExact() {
        assertEquals(330L,NativeOrderRoomLogic.LONG_PRESS_MS);
        assertEquals(22f,NativeOrderRoomLogic.LONG_PRESS_CANCEL_PX,0f);
        assertFalse(NativeOrderRoomLogic.shouldCancelLongPress(0,0,22,0));
        assertTrue(NativeOrderRoomLogic.shouldCancelLongPress(0,0,22.01f,0));
    }

    @Test
    public void roomReadyRequiresEdgesAndExistingValidatorOk() {
        NativeProjectStore.RoomData empty=room("empty");
        assertFalse(NativeOrderRoomLogic.roomReady(empty));

        NativeProjectStore.RoomData square=room("ready");
        NativeProjectStore.PointData a=new NativeProjectStore.PointData(100f,100f);
        NativeProjectStore.PointData b=new NativeProjectStore.PointData(300f,100f);
        NativeProjectStore.PointData c=new NativeProjectStore.PointData(300f,300f);
        NativeProjectStore.PointData d=new NativeProjectStore.PointData(100f,300f);
        square.points.add(a); square.points.add(b); square.points.add(c); square.points.add(d);
        square.edges.add(edge("ab",a.id,b.id,500));
        square.edges.add(edge("bc",b.id,c.id,500));
        square.edges.add(edge("cd",c.id,d.id,500));
        square.edges.add(edge("da",d.id,a.id,500));
        assertTrue(NativeOrderRoomLogic.roomReady(square));
        square.edges.get(0).value=0;
        assertFalse(NativeOrderRoomLogic.roomReady(square));
    }

    private static NativeProjectStore.RoomData room(String name){
        NativeProjectStore.RoomData r=new NativeProjectStore.RoomData();
        r.name=name;
        return r;
    }

    private static NativeProjectStore.EdgeData edge(
            String id,String a,String b,float value){
        return new NativeProjectStore.EdgeData(id,a,b,value);
    }

    private static String key(String value,String key,String action){
        return NativeOrderRoomLogic.applyRoomNameKey(value,key,action);
    }
}
