package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Centering the text of a field must NEVER replace the working compact
 * one-line estimate with an older two-tier layout (v240 regression).
 */
@RunWith(AndroidJUnit4.class)
public final class NativeEstimateCenteredOneRowRegressionTest {
    @Test public void bothEstimatesRetainOneRowWhenItFitsAndCenterNumericFields() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                NativeProjectStore.RoomData room=store.createRoom("Зал");
                room.extraQty.put("light",2f);
                room.clientEstimateQty.put("light",2f);
                room.clientEstimateOverride.add("light");
                room.clientPricing.put("light",500f);
                room.pricing.put("light",500f);
                NativeProjectStore.MaterialData longName=new NativeProjectStore.MaterialData();
                longName.name="Длинный дополнительный материал для потолка";
                longName.unit="шт";
                longName.qty=3f;
                longName.price=125000f;
                room.clientCustomItems.add(longName);
                room.customItems.add(longName);
                project.rooms.add(room);

                for(NativeEstimateDialog.Mode mode:NativeEstimateDialog.Mode.values()){
                    NativeEstimateDialog dialog=new NativeEstimateDialog(
                            activity,store,project,mode,()->{});
                    dialog.show();
                    try{
                        View root=dialog.getWindow().getDecorView();
                        String lightKey="light_"+room.id;
                        assertCentered(root,lightKey);
                        LinearLayout normal=root.findViewWithTag(
                                "native_estimate_line_"+lightKey);
                        assertNotNull(normal);
                        boolean expectedDense=NativeEstimateDensePolicy.oneRow(
                                activity.getResources().getConfiguration().screenWidthDp,
                                activity.getResources().getConfiguration().fontScale,
                                NativeUiPrefs.load(activity).scale/
                                        NativeUiPrefs.defaults().scale,
                                "Светильники","шт","500","1 000","2");
                        assertEquals("only the field text changes, NOT the row layout",
                                expectedDense?LinearLayout.HORIZONTAL:
                                        LinearLayout.VERTICAL,
                                normal.getOrientation());

                        String customKey="custom:"+longName.id+"_"+room.id;
                        LinearLayout fallback=root.findViewWithTag(
                                "native_estimate_line_"+customKey);
                        assertNotNull(fallback);
                        assertEquals("long names keep the accessible two-tier layout",
                                LinearLayout.VERTICAL,fallback.getOrientation());
                        assertCentered(root,customKey);
                        assertNotNull("bottom navigation must stay visible",
                                root.findViewWithTag("native_fullscreen_dialog_nav"));
                    }finally{
                        dialog.dismiss();
                    }
                }
            });
        }
    }

    private static void assertCentered(View root,String key){
        Button qty=root.findViewWithTag("native_estimate_qty_"+key);
        Button price=root.findViewWithTag("native_estimate_price_"+key);
        assertNotNull("editable quantity exists for "+key,qty);
        assertNotNull("editable price exists for "+key,price);
        for(Button button:new Button[]{qty,price}){
            assertEquals("number centered horizontally and vertically",
                    Gravity.CENTER,button.getGravity());
            assertEquals("symmetrical left/right padding",
                    button.getPaddingLeft(),button.getPaddingRight());
            assertTrue("number must remain in a compact touch field",
                    button.getLayoutParams().height>0);
        }
    }
}
