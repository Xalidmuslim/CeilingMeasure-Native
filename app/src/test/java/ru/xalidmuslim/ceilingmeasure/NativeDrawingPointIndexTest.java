package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;
import org.junit.Test;

/** Rendering-only cache: never substitute it for persisted room geometry. */
public final class NativeDrawingPointIndexTest {
    @Test public void returnsExactlyTheSameVertexObjectsAsGeometry() {
        NativeProjectStore.RoomData room=new NativeProjectStore.RoomData();
        NativeProjectStore.PointData first=new NativeProjectStore.PointData("a",20,30);
        NativeProjectStore.PointData second=new NativeProjectStore.PointData("b",60,80);
        room.points.add(first);
        room.points.add(second);
        NativeDrawingPointIndex index=new NativeDrawingPointIndex();

        assertSame(first,index.get(room,"a"));
        assertSame(second,index.get(room,"b"));
        assertNull(index.get(room,"missing"));
        // Moving a point mutates the original object, not a stale snapshot.
        second.x=120;
        assertEquals(120,index.get(room,"b").x,0.0001f);
        assertEquals(2,index.sizeForTest());
    }

    @Test public void roomSwitchPointAdditionAndRemovalRefreshLookup() {
        NativeDrawingPointIndex index=new NativeDrawingPointIndex();
        NativeProjectStore.RoomData one=new NativeProjectStore.RoomData();
        NativeProjectStore.RoomData two=new NativeProjectStore.RoomData();
        NativeProjectStore.PointData a=new NativeProjectStore.PointData("a",0,0);
        NativeProjectStore.PointData b=new NativeProjectStore.PointData("b",10,10);
        one.points.add(a);
        two.points.add(b);
        assertSame(a,index.get(one,"a"));
        assertNull(index.get(two,"a"));
        assertSame(b,index.get(two,"b"));

        two.points.add(new NativeProjectStore.PointData("c",15,30));
        assertNotNull(index.get(two,"c"));
        two.points.remove(b);
        assertNull(index.get(two,"b"));
        assertEquals(1,index.sizeForTest());
    }

    @Test public void explicitInvalidationHandlesReplacementAtSameListSize() {
        NativeDrawingPointIndex index=new NativeDrawingPointIndex();
        NativeProjectStore.RoomData room=new NativeProjectStore.RoomData();
        NativeProjectStore.PointData original=new NativeProjectStore.PointData("a",0,0);
        room.points.add(original);
        assertSame(original,index.get(room,"a"));
        room.points.set(0,new NativeProjectStore.PointData("b",5,5));
        index.invalidate();
        assertNull(index.get(room,"a"));
        assertNotNull(index.get(room,"b"));
    }

    @Test public void duplicatesRetainFirstMatchSemantics() {
        NativeDrawingPointIndex index=new NativeDrawingPointIndex();
        NativeProjectStore.RoomData room=new NativeProjectStore.RoomData();
        NativeProjectStore.PointData first=new NativeProjectStore.PointData("a",1,1);
        room.points.add(first);
        room.points.add(new NativeProjectStore.PointData("a",2,2));
        assertSame(first,index.get(room,"a"));
    }
}
