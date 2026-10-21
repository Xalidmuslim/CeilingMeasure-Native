package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;

/**
 * A lightweight vertex lookup used only by Canvas rendering. Sketch editing
 * continues to use the authoritative room data and unchanged geometry engine.
 */
final class NativeDrawingPointIndex {
    private final HashMap<String,NativeProjectStore.PointData> points=new HashMap<>();
    private NativeProjectStore.RoomData cachedRoom;
    private int cachedCount=-1;
    private int rebuildCount;
    private NativeProjectStore.PointData cachedFirst;
    private NativeProjectStore.PointData cachedLast;

    @Nullable
    NativeProjectStore.PointData get(@NonNull NativeProjectStore.RoomData room,
            @Nullable String id){
        if(room!=cachedRoom || room.points.size()!=cachedCount
                || (!room.points.isEmpty() &&
                (cachedFirst!=room.points.get(0)
                        ||cachedLast!=room.points.get(room.points.size()-1)))){
            rebuild(room);
        }
        return id==null?null:points.get(id);
    }

    void invalidate(){
        cachedRoom=null;
        cachedCount=-1;
        cachedFirst=null;
        cachedLast=null;
        points.clear();
    }

    int rebuildCountForTest(){
        return rebuildCount;
    }

    int sizeForTest(){
        return points.size();
    }

    private void rebuild(@NonNull NativeProjectStore.RoomData room){
        rebuildCount++;
        points.clear();
        for(NativeProjectStore.PointData point:room.points){
            // Match the first-match behavior of NativeProjectStore.pointById.
            if(point.id!=null&&!points.containsKey(point.id))
                points.put(point.id,point);
        }
        cachedRoom=room;
        cachedCount=room.points.size();
        cachedFirst=cachedCount==0?null:room.points.get(0);
        cachedLast=cachedCount==0?null:room.points.get(cachedCount-1);
    }
}
