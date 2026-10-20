package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Stable order for mixed standard pricing rows and custom materials. */
final class NativePriceOrder {
    private NativePriceOrder() {}

    @NonNull
    static ArrayList<String> normalized(@NonNull List<String> saved,
            @NonNull List<String> available) {
        HashSet<String> valid=new HashSet<>(available);
        HashSet<String> seen=new HashSet<>();
        ArrayList<String> ordered=new ArrayList<>(available.size());
        for(String key:saved) if(valid.contains(key)&&seen.add(key))ordered.add(key);
        for(String key:available) if(seen.add(key))ordered.add(key);
        return ordered;
    }

    /** Place dragged source immediately BEFORE the target. */
    @NonNull
    static ArrayList<String> move(@NonNull List<String> previous,
            @NonNull String source,
            @NonNull String target) {
        ArrayList<String> reordered=new ArrayList<>(previous);
        if(source.equals(target)||!reordered.contains(source)||!reordered.contains(target))
            return reordered;
        reordered.remove(source);
        int targetIndex=reordered.indexOf(target);
        reordered.add(targetIndex,source);
        return reordered;
    }

    @NonNull static String standard(String key){return "std:"+key;}
    @NonNull static String custom(String id){return "custom:"+id;}
}
