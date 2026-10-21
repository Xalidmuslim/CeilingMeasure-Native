package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class NativeEstimateCompactRowUiTest {
    @Test public void compactFormulaKeepsOperandsTogetherAndTouchControlsAccessible(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent start=new Intent(context,NativeWorkspaceActivity.class);
        start.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(start)){
            scenario.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                NativeProjectStore.RoomData room=store.createRoom("Гостиная");
                room.extraQty.put("light",2f);
                room.clientPricing.put("light",500f);
                NativeProjectStore.MaterialData special=new NativeProjectStore.MaterialData();
                special.name="Очень длинное название дополнительного материала потолочной конструкции";
                special.unit="шт";
                special.qty=3f;
                special.price=3500f;
                room.clientCustomItems.add(special);
                project.rooms.add(room);

                NativeEstimateDialog dialog=new NativeEstimateDialog(activity,store,
                        project,NativeEstimateDialog.Mode.CLIENT,()->{});
                dialog.show();
                try{
                    View root=dialog.getWindow().getDecorView();
                    String light="light_"+room.id;
                    LinearLayout formula=root.findViewWithTag("native_estimate_formula_"+light);
                    assertNotNull(formula);
                    TextView qty=root.findViewWithTag("native_estimate_qty_"+light);
                    Button price=root.findViewWithTag("native_estimate_price_"+light);
                    assertNotNull(qty);
                    assertNotNull(price);
                    assertFalse("uneditable calculated quantity is readable, not a dead button",
                            qty instanceof Button);
                    assertEquals("шт",((TextView)formula.getChildAt(1)).getText());
                    assertEquals("×",((TextView)formula.getChildAt(2)).getText());
                    assertEquals("2",qty.getText().toString());
                    assertEquals("500 ₽",price.getText().toString());
                    assertEquals("unit must not expand across the whole row",0f,
                            ((LinearLayout.LayoutParams)formula.getChildAt(1)
                                    .getLayoutParams()).weight,0f);
                    assertTrue("price editor must remain a usable touch target",
                            price.getLayoutParams().height>=dp(activity,40));

                    String custom="custom:"+special.id+"_"+room.id;
                    LinearLayout customFormula=root.findViewWithTag(
                            "native_estimate_formula_"+custom);
                    assertNotNull(customFormula);
                    assertTrue(root.findViewWithTag("native_estimate_qty_"+custom)
                            instanceof Button);
                    TextView longTitle=root.findViewWithTag("native_estimate_title_"+custom);
                    assertNotNull(longTitle);
                    assertTrue("long material names must support at least two lines",
                            longTitle.getMaxLines()>=2);
                    assertNotNull(root.findViewWithTag("native_estimate_actions_"+custom));
                    assertNotNull("fixed dialog navigation preserved",
                            root.findViewWithTag("native_fullscreen_dialog_nav"));
                    assertNotNull("room material action preserved",
                            root.findViewWithTag("native_client_add_material_"+room.id));
                }finally{dialog.dismiss();}
            });
        }
    }

    @Test public void repeatedReadyLabelsDoNotWasteRoomCardHeight(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent start=new Intent(context,NativeWorkspaceActivity.class);
        start.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(start)){
            scenario.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                NativeProjectStore.RoomData room=store.createRoom("Зал");
                project.rooms.add(room);
                NativeEstimateDialog dialog=new NativeEstimateDialog(activity,store,
                        project,NativeEstimateDialog.Mode.CLIENT,()->{});
                dialog.show();
                try{
                    View root=dialog.getWindow().getDecorView();
                    // No silent failure: an incomplete room must clearly explain the issue.
                    assertNotNull(root.findViewWithTag(
                            "native_estimate_incomplete_"+room.id));
                    assertNotNull(root.findViewWithTag("native_fullscreen_dialog_nav"));
                }finally{dialog.dismiss();}
            });
        }
    }

    private static int dp(Context context,int value){
        return Math.round(value*context.getResources().getDisplayMetrics().density);
    }
}
