package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;
import org.junit.Test;

public final class NativeEstimateTableColumnsTest {
    @Test public void fixedColumnWidthsAlwaysFitCommonAndroidScreenSizes() {
        for(int screenWidth:new int[]{280,300,320,360,393,430,600}){
            NativeEstimateTableColumns grid=
                    NativeEstimateTableColumns.forScreenWidthDp(screenWidth);
            assertTrue("table must fit even with 32dp horizontal page margins",
                    grid.totalMinimumDp()<=screenWidth-32);
            assertTrue(grid.quantityDp>=54 && grid.quantityDp<=70);
            assertTrue(grid.unitDp>=27 && grid.unitDp<=36);
            assertTrue(grid.priceDp>=82 && grid.priceDp<=105);
            assertTrue(grid.actionsDp>=48);
        }
    }

    @Test public void sameScreenWidthAlwaysUsesIdenticalColumnGeometry() {
        NativeEstimateTableColumns first=
                NativeEstimateTableColumns.forScreenWidthDp(393);
        NativeEstimateTableColumns next=
                NativeEstimateTableColumns.forScreenWidthDp(393);
        assertEquals(first.quantityDp,next.quantityDp);
        assertEquals(first.unitDp,next.unitDp);
        assertEquals(first.multiplyDp,next.multiplyDp);
        assertEquals(first.priceDp,next.priceDp);
        assertEquals(first.actionsDp,next.actionsDp);
    }

    @Test public void narrowerScreensChooseSmallerPriceColumnWithoutShrinkingActions(){
        NativeEstimateTableColumns phone=
                NativeEstimateTableColumns.forScreenWidthDp(320);
        NativeEstimateTableColumns wide=
                NativeEstimateTableColumns.forScreenWidthDp(430);
        assertTrue(phone.priceDp<wide.priceDp);
        assertEquals(phone.actionsDp,wide.actionsDp);
    }
}
