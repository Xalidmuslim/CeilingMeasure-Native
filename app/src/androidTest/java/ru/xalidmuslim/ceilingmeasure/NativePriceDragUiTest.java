package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public final class NativePriceDragUiTest {
    @Test public void priceScreensProvideDragHandlesAndRespectPersistedRowOrder() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(ctx,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                project.rooms.add(store.createRoom("Тест"));
                NativeProjectStore.MaterialData item=new NativeProjectStore.MaterialData();
                item.name="Дополнительный профиль";
                item.price=300f;
                store.upsertDefaultCustom(true,item);
                store.saveDefaultPriceOrder(true,Arrays.asList(
                        "custom:"+item.id,"std:canvas"));

                NativePricePresetDialog client=new NativePricePresetDialog(
                        activity,store,project,Collections.singletonList(project),
                        true,()->{});
                client.show();
                try{
                    View reorderToggle=client.findViewById(android.R.id.content)
                            .getRootView().findViewWithTag("native_price_toggle_reorder");
                    assertNotNull(reorderToggle);
                    reorderToggle.performClick();
                    ViewGroup rows=(ViewGroup)client.findViewById(android.R.id.content)
                            .getRootView().findViewWithTag("native_client_price_drag_list");
                    assertNotNull(rows);
                    assertEquals("native_price_row_custom:"+item.id,
                            rows.getChildAt(0).getTag());
                    View grip=rows.findViewWithTag("native_price_grip_custom:"+item.id);
                    assertNotNull(grip);
                    assertTrue(grip.isClickable());
                    assertNotNull(rows.findViewWithTag("native_price_grip_std:canvas"));
                }finally{client.dismiss();}

                NativePricePresetDialog purchase=new NativePricePresetDialog(
                        activity,store,project,Collections.singletonList(project),
                        false,()->{});
                purchase.show();
                try{
                    View reorderToggle=purchase.findViewById(android.R.id.content)
                            .getRootView().findViewWithTag("native_price_toggle_reorder");
                    assertNotNull(reorderToggle);
                    reorderToggle.performClick();
                    View list=purchase.findViewById(android.R.id.content).getRootView()
                            .findViewWithTag("native_purchase_price_drag_list");
                    assertNotNull(list);
                    assertNotNull(list.findViewWithTag("native_price_grip_std:canvas"));
                }finally{purchase.dismiss();}
            });
        }
    }
}
