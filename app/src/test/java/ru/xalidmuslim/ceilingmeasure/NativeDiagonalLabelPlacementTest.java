package ru.xalidmuslim.ceilingmeasure;

import org.junit.Test;

import static org.junit.Assert.*;

/** Regression tests for automatic dimension placement on interior diagonals. */
public final class NativeDiagonalLabelPlacementTest {
    @Test
    public void automaticLabelIsCenteredExactlyOnItsDiagonal() {
        NativeProjectStore.RoomData room = new NativeProjectStore.RoomData();
        addClosedContour(room,
                new float[][]{{0,0},{400,0},{400,300},{0,300}});
        NativeProjectStore.DiagonalData diagonal =
                new NativeProjectStore.DiagonalData("d1","p0","p2",500f);
        room.diagonals.add(diagonal);

        NativeLabelEngine.Placement p = NativeLabelEngine.find(
                NativeLabelEngine.layout(room,1f,1f),
                NativeLabelEngine.Kind.DIAGONAL,diagonal.id);

        assertNotNull(p);
        // Signed point-to-line distance: a=(0,0), b=(400,300).
        assertEquals(0f,(400f*p.y-300f*p.x)/500f,.001f);
        assertEquals(.5f,p.x/400f,.12f);
        assertEquals(.5f,p.y/300f,.12f);
    }

    @Test
    public void narrowWaistMovesLabelToRoomierStartOfDiagonal() {
        NativeProjectStore.RoomData room = new NativeProjectStore.RoomData();
        // Two broad regions connected by a narrow 20-unit neck at x=200.
        // The diagonal crosses the neck at y=100.
        addClosedContour(room,new float[][]{
                {0,100},{0,0},{150,0},{190,90},{210,90},{250,0},
                {400,0},{400,100},{400,200},{250,200},{210,110},
                {190,110},{150,200},{0,200}
        });
        NativeProjectStore.DiagonalData diagonal =
                new NativeProjectStore.DiagonalData("d_waist","p0","p7",400f);
        room.diagonals.add(diagonal);

        NativeLabelEngine.Placement p = NativeLabelEngine.find(
                NativeLabelEngine.layout(room,1f,1f),
                NativeLabelEngine.Kind.DIAGONAL,diagonal.id);

        assertNotNull(p);
        assertEquals("Label must stay on the diagonal",100f,p.y,.001f);
        assertTrue("Prefer open region nearer start over narrow midpoint: x="+p.x,
                p.x<160f);
    }

    @Test
    public void userPositionedDiagonalLabelRemainsUntouched() {
        NativeProjectStore.RoomData room = new NativeProjectStore.RoomData();
        addClosedContour(room,new float[][]{{0,0},{400,0},{400,300},{0,300}});
        NativeProjectStore.DiagonalData d =
                new NativeProjectStore.DiagonalData("manual","p0","p2",500f);
        d.labelManual = true;
        d.labelX = 123f;
        d.labelY = 75f;
        room.diagonals.add(d);

        NativeLabelEngine.Placement p = NativeLabelEngine.find(
                NativeLabelEngine.layout(room,1f,1f),
                NativeLabelEngine.Kind.DIAGONAL,d.id);

        assertNotNull(p);
        assertEquals(123f,p.x,.001f);
        assertEquals(75f,p.y,.001f);
    }

    private static void addClosedContour(
            NativeProjectStore.RoomData room,float[][] coordinates) {
        for(int i=0;i<coordinates.length;i++){
            room.points.add(new NativeProjectStore.PointData(
                    "p"+i,coordinates[i][0],coordinates[i][1]));
        }
        for(int i=0;i<coordinates.length;i++){
            int next=(i+1)%coordinates.length;
            float dx=coordinates[next][0]-coordinates[i][0];
            float dy=coordinates[next][1]-coordinates[i][1];
            room.edges.add(new NativeProjectStore.EdgeData(
                    "e"+i,"p"+i,"p"+next,(float)Math.hypot(dx,dy)));
        }
    }
}
