package ru.xalidmuslim.ceilingmeasure;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/** Reordering is stable across hidden rows and custom/standard material mixes. */
public final class NativePriceOrderTest {
    @Test public void savedOrderKeepsAvailableRowsAndAppendsNewOnes() {
        List<String> available = Arrays.asList("std:canvas","std:insert","custom:a","custom:b");
        List<String> saved = Arrays.asList("custom:a","std:canvas","removed","custom:a");
        assertEquals(Arrays.asList("custom:a","std:canvas","std:insert","custom:b"),
                NativePriceOrder.normalized(saved,available));
    }

    @Test public void dragMovesBothStandardAndCustomItemsUpOrDown() {
        List<String> initial = Arrays.asList("std:canvas","std:insert","custom:a","custom:b");
        assertEquals(Arrays.asList("custom:b","std:canvas","std:insert","custom:a"),
                NativePriceOrder.move(initial,"custom:b","std:canvas"));
        assertEquals(Arrays.asList("std:canvas","custom:a","std:insert","custom:b"),
                NativePriceOrder.move(initial,"custom:a","std:insert"));
    }

    @Test public void draggingDownPlacesSourceBelowTarget() {
        List<String> original=Arrays.asList("std:canvas","std:insert","custom:a","custom:b");
        assertEquals(Arrays.asList("std:insert","custom:a","std:canvas","custom:b"),
                NativePriceOrder.move(original,"std:canvas","custom:a"));
    }

    @Test public void missingSourceOrTargetCannotChangeOrder() {
        List<String> available=Arrays.asList("std:canvas","custom:a");
        assertEquals(available,NativePriceOrder.move(available,"missing","custom:a"));
        assertEquals(available,NativePriceOrder.move(available,"std:canvas","missing"));
    }

    @Test public void priceOrderPersistsSeparatelyForClientAndPurchase() {
        MemorySharedPreferences prefs=new MemorySharedPreferences();
        NativeProjectStore first=new NativeProjectStore(prefs);
        first.saveDefaultPriceOrder(true,Arrays.asList("custom:client","std:canvas"));
        first.saveDefaultPriceOrder(false,Arrays.asList("std:pipe","std:canvas"));
        NativeProjectStore second=new NativeProjectStore(prefs);
        assertEquals(Arrays.asList("custom:client","std:canvas"),
                second.loadDefaultPriceOrder(true));
        assertEquals(Arrays.asList("std:pipe","std:canvas"),
                second.loadDefaultPriceOrder(false));
    }
}
