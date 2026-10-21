package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression for values sticking to the right edges of framed quantity and price
 * fields in both the client and purchase estimate.
 */
@RunWith(AndroidJUnit4.class)
public final class NativeEstimateFieldAlignmentTest {
    @Test public void pricesAndEditableQuantitiesAreCenteredInBothEstimateModes(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                NativeProjectStore.RoomData room=store.createRoom("Комната 1");

                NativeProjectStore.MaterialData client=new NativeProjectStore.MaterialData();
                client.name="Светильник";
                client.unit="шт";
                client.qty=3f;
                client.price=3500f;
                room.clientCustomItems.add(client);

                NativeProjectStore.MaterialData purchase=new NativeProjectStore.MaterialData();
                purchase.name="Светильник";
                purchase.unit="шт";
                purchase.qty=3f;
                purchase.price=500f;
                room.customItems.add(purchase);
                project.rooms.add(room);

                for(NativeEstimateDialog.Mode mode:NativeEstimateDialog.Mode.values()){
                    NativeEstimateDialog dialog=new NativeEstimateDialog(
                            activity,store,project,mode,()->{});
                    dialog.show();
                    try{
                        View root=dialog.getWindow().getDecorView();
                        String materialId=(mode==NativeEstimateDialog.Mode.CLIENT
                                ?client.id:purchase.id);
                        String key="custom:"+materialId+"_"+room.id;
                        Button quantity=root.findViewWithTag("native_estimate_qty_"+key);
                        Button price=root.findViewWithTag("native_estimate_price_"+key);
                        assertNotNull("editable quantity",quantity);
                        assertNotNull("editable price",price);
                        assertCenteredInsideInput(quantity);
                        assertCenteredInsideInput(price);
                        assertEquals("same calculated totals; only presentation changed",
                                "3",quantity.getText().toString());
                    }finally{
                        dialog.dismiss();
                    }
                }
            });
        }
    }

    private static void assertCenteredInsideInput(Button input){
        assertEquals("horizontal center within outline",
                Gravity.CENTER_HORIZONTAL,
                input.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK);
        assertEquals("vertical center within outline",
                Gravity.CENTER_VERTICAL,
                input.getGravity() & Gravity.VERTICAL_GRAVITY_MASK);
        assertEquals("symmetric horizontal field padding",
                input.getPaddingLeft(),input.getPaddingRight());
        assertTrue("the number is not cropped by an oversized right inset",
                input.getPaddingLeft()<=input.getResources()
                        .getDisplayMetrics().density*3);
    }
}
