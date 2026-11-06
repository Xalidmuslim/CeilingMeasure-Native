package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

public final class NativeProjectTrashTest {
    @Test public void deletedDrawingCanBeRestoredWithAllRoomGeometry() throws Exception {
        MemorySharedPreferences prefs=new MemorySharedPreferences();
        NativeProjectStore store=new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData project=store.newProject();
        project.name="Объект для восстановления";
        NativeProjectStore.RoomData room=project.rooms.get(0);
        room.name="Зал";
        room.notes="Оставить размеры";
        room.points.add(new NativeProjectStore.PointData("A",0f,0f));
        room.points.add(new NativeProjectStore.PointData("B",320f,0f));
        room.edges.add(new NativeProjectStore.EdgeData("AB","A","B",320f));
        project.rooms.add(store.createRoom("Кухня"));
        project.rooms.get(1).notes="Дополнительный чертёж";
        assertTrue(store.saveSnapshot(project));

        assertTrue(store.deleteSaved(project.id));
        NativeProjectStore afterDelete=new NativeProjectStore(prefs);
        assertTrue(afterDelete.loadSaved().stream().noneMatch(p->p.id.equals(project.id)));
        assertEquals(1,afterDelete.loadDeleted().size());
        assertEquals(project.id,afterDelete.loadDeleted().get(0).project.id);

        assertTrue(afterDelete.restoreDeleted(project.id));
        NativeProjectStore afterRestore=new NativeProjectStore(prefs);
        assertTrue(afterRestore.loadDeleted().isEmpty());
        NativeProjectStore.ProjectData restored=afterRestore.loadSaved().stream()
                .filter(p->p.id.equals(project.id)).findFirst().orElse(null);
        assertNotNull(restored);
        assertEquals(project.name,restored.name);
        assertEquals(2,restored.rooms.size());
        assertEquals("Зал",restored.rooms.get(0).name);
        assertEquals("Оставить размеры",restored.rooms.get(0).notes);
        assertEquals(2,restored.rooms.get(0).points.size());
        assertEquals("AB",restored.rooms.get(0).edges.get(0).id);
        assertEquals(320f,restored.rooms.get(0).edges.get(0).value,.001f);
        assertEquals("Дополнительный чертёж",restored.rooms.get(1).notes);
    }

    @Test public void lateQueuedSaveCannotResurrectTrashedProjectOrOverwriteOthers() {
        MemorySharedPreferences prefs=new MemorySharedPreferences();
        NativeProjectStore store=new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData one=store.newProject();
        one.name="Удаляемый";
        store.saveSnapshot(one);
        NativeProjectStore.ProjectData two=store.newProject();
        two.name="Оставить";
        store.saveSnapshot(two);

        assertTrue(store.deleteSaved(one.id));
        one.name="Старый фоновый снимок";
        store.saveCurrent(one);
        NativeProjectStore reloaded=new NativeProjectStore(prefs);
        assertTrue(reloaded.loadSaved().stream().noneMatch(p->p.id.equals(one.id)));
        assertTrue(reloaded.loadSaved().stream().anyMatch(p->p.id.equals(two.id)));
        assertEquals(1,reloaded.loadDeleted().size());
        assertEquals("Удаляемый",reloaded.loadDeleted().get(0).project.name);
        assertFalse(store.restoreDeleted("missing-id"));
    }

    @Test public void trashSurvivesExportImportWithOriginalRecord() throws Exception {
        NativeProjectStore first=new NativeProjectStore(new MemorySharedPreferences());
        NativeProjectStore.ProjectData p=first.newProject();
        p.name="В резервной копии";
        first.saveSnapshot(p);
        first.deleteSaved(p.id);

        String exported=first.exportBackup();
        JSONObject payload=new JSONObject(exported);
        assertNotNull(payload.getJSONObject("db").optJSONArray("deletedOrders"));

        NativeProjectStore second=new NativeProjectStore(new MemorySharedPreferences());
        assertTrue(second.importBackup(exported));
        assertEquals(1,second.loadDeleted().size());
        assertTrue(second.restoreDeleted(p.id));
        assertEquals("В резервной копии",second.loadSaved().get(0).name);
    }

    @Test public void existingPurchaseOrderMovesOnlyDowelOnce() {
        MemorySharedPreferences prefs=new MemorySharedPreferences();
        NativeProjectStore store=new NativeProjectStore(prefs);
        java.util.List<String> old=java.util.Arrays.asList(
                "std:canvas", "std:baguette", "std:insert", "std:light",
                "custom:wood", "std:cornice", "std:dowel", "std:pipe");
        store.saveDefaultPriceOrder(false,old);
        NativeProjectStore.PricePresetSnapshot updated=store.loadPricePresetSnapshot(false);
        assertEquals("std:dowel",updated.order.get(2));
        assertEquals("std:insert",updated.order.get(3));
        assertTrue(updated.order.contains("custom:wood"));

        // A later user drag must remain authoritative after this one-time migration.
        java.util.List<String> personal=java.util.Arrays.asList(
                "std:canvas", "std:dowel", "std:baguette", "std:insert");
        store.saveDefaultPriceOrder(false,personal);
        assertEquals(personal,new NativeProjectStore(prefs).loadDefaultPriceOrder(false));
        assertEquals(personal,new NativeProjectStore(prefs).loadPricePresetSnapshot(false).order);
    }

    @Test public void baguettePrecedesDowelInDefaultPurchaseList() {
        String[] prices=NativeDataContract.INTERNAL_PRICE_KEYS;
        int baguette=-1,dowel=-1;
        for(int i=0;i<prices.length;i++){
            if("baguette".equals(prices[i]))baguette=i;
            if("dowel".equals(prices[i]))dowel=i;
        }
        assertTrue(baguette>=0);
        assertEquals(baguette+1,dowel);
        NativeEstimateEngine.Meta[] rows=NativeEstimateEngine.INTERNAL_META;
        int metaIndex=-1;
        for(int i=0;i<rows.length;i++)if("baguette".equals(rows[i].key))metaIndex=i;
        assertTrue(metaIndex>=0);
        assertEquals("dowel",rows[metaIndex+1].key);
    }
}
